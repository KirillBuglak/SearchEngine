package searchengine.services;

import org.jsoup.Jsoup;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchPageData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@ConfigurationProperties(prefix = "search-service")
public class SearchService {

    private double percentOfPages;
    private final LemmaService lemmaService;
    private final SiteService siteService;
    private final IndexService indexService;
    private List<String> foundLemmasFromDB;

    public SearchService(LemmaService lemmaService, SiteService siteService, IndexService indexService) {
        this.lemmaService = lemmaService;
        this.siteService = siteService;
        this.indexService = indexService;
    }

    public SearchResponse getSearch(String query, String searchSiteUrl) {//fixme SearchResponse getSearch(String query, String searchSiteUrl){return SearchResponse getSearch(String query, String searchSiteUrl, offset = 0, limit = 20) }
        SearchResponse response = new SearchResponse();

        Site searchSite = siteService.getSiteByURL(searchSiteUrl);
        foundLemmasFromDB = lemmaService.getFilteredLemmas(query).stream()
                .filter(lemmaService::isLemmaPresent).toList();

        List<Integer> lemmasIds = lemmaService.getLemmasByListOfWords(foundLemmasFromDB).stream().map(Lemma::getId)
                .distinct().toList();
        List<Index> allIndexesByLemmaIds = indexService.getAllIndexesByLemmaIds(lemmasIds);
        List<Index> searchSiteIndexes = allIndexesByLemmaIds.stream().filter(index -> index.getPage().getSite()
                .getUrl().equals(searchSiteUrl)).toList();

        boolean searchSiteHasResults = searchSiteIndexes.size() != 0;//fixme work down from here - make it easier
        if (allIndexesByLemmaIds.size() != 0 && searchSite == null && addSearchPageDataInfo(getPageRelInfo(allIndexesByLemmaIds)) != null) {
            response.setData(addSearchPageDataInfo(getPageRelInfo(allIndexesByLemmaIds)));
            response.setCount(indexService.indexesCountByLemmasIds(lemmasIds));
            response.setResult(response.getCount() != 0);
        } else if (searchSite != null && searchSiteHasResults && addSearchPageDataInfo(getPageRelInfo(searchSiteIndexes)) != null) {
            response.setData(addSearchPageDataInfo(getPageRelInfo(searchSiteIndexes)));
            response.setCount(searchSiteIndexes.size());
            response.setResult(response.getCount() != 0);
        } else if (query.isBlank()) {
            response.setError("Задан пустой поисковый запрос");
        } else {//fixme work on this check
            response.setError("По вашему запросу ничего не нашлось");
        }
        return response;
    }

    private Map<Page, Float> getPageRelInfo(List<Index> indexesList) {
        HashMap<Page, Float> pageAndAbsRel = new HashMap<>();
        indexesList.forEach(index -> {
            Page pageFromIndex = index.getPage();
            if (pageAndAbsRel.containsKey(pageFromIndex)) {
                pageAndAbsRel.put(pageFromIndex, pageAndAbsRel.get(pageFromIndex) + index.getRank());
            } else {
                pageAndAbsRel.put(pageFromIndex, index.getRank());
            }
        });
        Float divisor = pageAndAbsRel.values().stream().reduce(Math::max).get();
        return pageAndAbsRel.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() / divisor));
    }

    private List<SearchPageData> addSearchPageDataInfo(Map<Page, Float> pageRelInfo) {
        List<SearchPageData> dataList = new ArrayList<>();
        pageRelInfo.forEach((page, relevance) -> {
            StringBuilder snippet = buildSnippet(page);
            if (snippet.length() == 0) {//fixme may also have the same snippets
                return;
            }
            SearchPageData data;
            try {
                data = new SearchPageData(page.getSite().getUrl(),
                        page.getSite().getName(), page.getPath(), Jsoup.connect(page
                        .getPath()).get().title(), snippet.toString(), relevance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dataList.add(data);
        });
        Collections.sort(dataList);
        return dataList.size() == 0 ? null : dataList;
    }

    private StringBuilder buildSnippet(Page page) {//fixme work on all words in snippet
        StringBuilder snippet = new StringBuilder();

        String pageContent = page.getContent();

        List<Lemma> foundFilteredLemmas = getFoundFilteredLemmas(page);

        List<String> contentWordsSorted = getContentWordsSorted(foundFilteredLemmas, pageContent);

        contentWordsSorted.forEach(contentWord -> {
            int pageStart = pageContent.toLowerCase().indexOf(contentWord);
            int pageFinish = pageStart + contentWord.length();
            int snippetStart = snippet.toString().toLowerCase().indexOf(contentWord);
            int snippetFinish = snippetStart + contentWord.length();
            int substringStart = Math.max(pageContent.toLowerCase().indexOf(contentWord) - 20, 0);
            int substringFinish = Math.min(pageStart + contentWord.length() + (snippet.length() > 200 ? 20 : 100),
                    pageContent.length() - 1);
            String boldWord = "<b>" + pageContent.substring(pageStart, pageFinish) + "</b>";

            if (snippet.toString().toLowerCase().contains(contentWord)) {
                snippet.replace(snippetStart, snippetFinish, boldWord)
                        .delete(snippet.toString().indexOf(boldWord) + boldWord.length(), snippet.length() - 1)
                        .append(pageContent, pageFinish, substringFinish)
                        .append("...");
            } else {
                String substring = pageContent.substring(substringStart, substringFinish)
                        .replaceAll(pageContent.substring(pageStart, pageFinish),
                                boldWord);
                snippet.append(snippet.length() > 0 ? "" : "...").append(substring).append("...");
            }
        });
        return snippet;
    }

    private List<String> getContentWordsSorted(List<Lemma> foundLemmas, String pageContent) {
        return lemmaService.getFilteredWordAndLemma(pageContent).entrySet().stream()//fixme work on getFilteredWordAndLemmaS?
                .filter(entry -> foundLemmas.stream().map(Lemma::getLemma)
                        .toList().contains(entry.getValue())).map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt(w -> pageContent.toLowerCase().indexOf(w))).toList();
    }

    private List<Lemma> getFoundFilteredLemmas(Page page) {
        HashMap<Page, List<Lemma>> pagesAndLemmas = LemmaService.getPagesAndLemmas();

        List<String> allPageLemmasForSearch = pagesAndLemmas.entrySet().stream()
                .filter(entry -> entry.getKey().getPath().equals(page.getPath()))
                .map(Map.Entry::getValue).flatMap(List::stream)
                .map(Lemma::getLemma).toList();

        Predicate<String> filterTooCommonLemmas = fLemma -> {
            int numberOfPagesContainingFLemma = pagesAndLemmas.values().stream()
                    .map(lemmaList -> lemmaList.stream().map(Lemma::getLemma).toList())
                    .filter(lemmaList -> lemmaList.contains(fLemma)).toList().size();
            return (double) numberOfPagesContainingFLemma / pagesAndLemmas.size() < percentOfPages;
        };

        List<Lemma> foundLemmas = lemmaService.getLemmasByListOfWords(foundLemmasFromDB.stream()
                .filter(allPageLemmasForSearch::contains)
                .filter(filterTooCommonLemmas)
                .toList());
        return foundLemmas;
    }

    public double getPercentOfPages() {
        return percentOfPages;
    }

    public void setPercentOfPages(double percentOfPages) {
        this.percentOfPages = percentOfPages;
    }
}
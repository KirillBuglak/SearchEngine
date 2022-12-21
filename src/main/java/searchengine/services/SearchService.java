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
import searchengine.services.modelServices.IndexService;
import searchengine.services.modelServices.LemmaService;
import searchengine.services.modelServices.PageService;
import searchengine.services.modelServices.SiteService;

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
    private final PageService pageService;
    private List<Lemma> foundFilteredLemmasFromDB;

    public SearchService(LemmaService lemmaService,
                         SiteService siteService,
                         IndexService indexService,
                         PageService pageService) {
        this.lemmaService = lemmaService;
        this.siteService = siteService;
        this.indexService = indexService;
        this.pageService = pageService;
    }

    public SearchResponse getSearch(String query, String searchSiteUrl) {//fixme SearchResponse getSearch(String query, String searchSiteUrl){return SearchResponse getSearch(String query, String searchSiteUrl, offset = 0, limit = 20) }
        SearchResponse response = new SearchResponse();

        Predicate<String> filterTooCommonLemmas = fLemma -> {
            int numberOfPagesContainingFLemma = indexService.getIndexesByLemma_lemma(fLemma).size();
            return (double) numberOfPagesContainingFLemma / pageService.getAllPages().size() < percentOfPages;
        };

        Site searchSite = siteService.getSiteByURL(searchSiteUrl);
        foundFilteredLemmasFromDB = lemmaService.getLemmasByListOfWords(lemmaService.getFilteredLemmas(query).stream()
                .filter(lemmaService::isLemmaPresent)
                .filter(filterTooCommonLemmas).toList());//fixme filter lemmas here

        List<Integer> lemmasIds = foundFilteredLemmasFromDB.stream().map(Lemma::getId)
                .distinct().toList();
        List<Index> allIndexesByLemmaIds = indexService.getAllIndexesByLemmaIds(lemmasIds);
        List<Index> searchSiteIndexes = allIndexesByLemmaIds.stream().filter(index -> index.getPage().getSite()
                .getUrl().equals(searchSiteUrl)).toList();

        if (searchSite == null && getPageRelInfo(allIndexesByLemmaIds) != null) { //fixme make it easier
            response.setData(addSearchPageDataInfo(Objects.requireNonNull(getPageRelInfo(allIndexesByLemmaIds))));
            response.setCount(response.getData().size());
            response.setResult(response.getCount() != 0);
        } else if (searchSite != null && getPageRelInfo(searchSiteIndexes) != null) { //fixme make it easier
            response.setData(addSearchPageDataInfo(Objects.requireNonNull(getPageRelInfo(searchSiteIndexes))));
            response.setCount(response.getData().size());
            response.setResult(response.getCount() != 0);
        } else if (query.isBlank()) {
            response.setError("Задан пустой поисковый запрос");
        } else {
            response.setError("По вашему запросу ничего не нашлось");
        }
        return response;
    }

    private Map<Page, Float> getPageRelInfo(List<Index> indexesList) {
        if (indexesList.size() == 0){
            return null;
        }
        //fixme may need to return null
        HashMap<Page, Float> pageAndAbsRel = new HashMap<>();
        indexesList.forEach(index -> {
            Page pageFromIndex = index.getPage();
            if (pageAndAbsRel.containsKey(pageFromIndex)) {
                pageAndAbsRel.put(pageFromIndex, pageAndAbsRel.get(pageFromIndex) + index.getRank());
            } else {
                pageAndAbsRel.put(pageFromIndex, index.getRank());
            }
        });
        Float divisor = pageAndAbsRel.values().stream().reduce(Math::max).orElse(0f);
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

    private StringBuilder buildSnippet(Page page) {
        StringBuilder snippet = new StringBuilder();

        String pageContent = page.getContent();

        List<Lemma> foundFilteredLemmasByPage = getFoundFilteredLemmasByPage(page);

        List<String> contentWordsSorted = getContentWordsSorted(foundFilteredLemmasByPage, pageContent);

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

    private List<Lemma> getFoundFilteredLemmasByPage(Page page) {
        return foundFilteredLemmasFromDB.stream()
                .filter(lemma -> indexService.getAllIndexesByPage(page).stream()
                        .map(index -> index.getLemma().getLemma()).toList().contains(lemma.getLemma())).toList();
    }

    private List<String> getContentWordsSorted(List<Lemma> foundFilteredLemmasByPage, String pageContent) {
        return lemmaService.getFilteredWordAndLemma(pageContent).entrySet().stream()
                .filter(entry -> foundFilteredLemmasByPage.stream().map(Lemma::getLemma)
                        .toList().contains(entry.getValue())).map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt(w -> pageContent.toLowerCase().indexOf(w))).toList();
    }

    public double getPercentOfPages() {
        return percentOfPages;
    }

    public void setPercentOfPages(double percentOfPages) {
        this.percentOfPages = percentOfPages;
    }
}
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
    private SortedSet<Lemma> foundFilteredLemmasFromDB;
    private Iterator<Lemma> foundFilteredLemmasFromDBIterator;

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
        foundFilteredLemmasFromDB = lemmaService.getSortedLemmasByListOfWords(lemmaService.getFilteredLemmas(query)
                .stream().filter(filterTooCommonLemmas).toList());
        foundFilteredLemmasFromDBIterator = foundFilteredLemmasFromDB.iterator();

        Set<Index> allIndexesByFirstLemma = indexService.getAllIndexesByLemma(foundFilteredLemmasFromDBIterator.next());
        Set<Index> searchSiteIndexesByFirstLemma = allIndexesByFirstLemma.stream().filter(index -> index.getPage().getSite()
                .getUrl().equals(searchSiteUrl)).collect(Collectors.toSet());

        if (searchSite == null && getPageRelInfo(allIndexesByFirstLemma) != null) { //fixme make it easier
            response.setData(addSearchPageDataInfo(Objects.requireNonNull(getPageRelInfo(allIndexesByFirstLemma))));
            response.setCount(response.getData().size());
            response.setResult(response.getCount() != 0);
        } else if (searchSite != null && getPageRelInfo(searchSiteIndexesByFirstLemma) != null) { //fixme make it easier
            response.setData(addSearchPageDataInfo(Objects.requireNonNull(getPageRelInfo(searchSiteIndexesByFirstLemma))));
            response.setCount(response.getData().size());
            response.setResult(response.getCount() != 0);
        } else if (query.isBlank()) {
            response.setError("Задан пустой поисковый запрос");
        } else {
            response.setError("По вашему запросу ничего не нашлось");
        }
        return response;
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

    private Map<Page, Float> getPageRelInfo(Set<Index> indexSet) {
        if (indexSet.size() == 0) {
            return null;
        }
        Map<Integer, Set<Page>> indexOfLemmaAndPages = getIndexOfLemmaAndPages(indexSet);
        Map<Page, Float> pageAndAbsRel = new HashMap<>();
        indexOfLemmaAndPages.forEach((indexOfLemma, pages) -> pages.forEach(page -> {
            float relevance = 0;
            Iterator<Lemma> iterator = foundFilteredLemmasFromDB.iterator();
            for (int i = 0; i <= indexOfLemma; i++) {
                relevance += indexService.getIndexByPageAndLemma(page, iterator.next()).getRank();
            }
            pageAndAbsRel.put(page, relevance);
        }));
        Float divisor = pageAndAbsRel.values().stream().reduce(Math::max).orElse((float) 0);
        return pageAndAbsRel.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() / divisor));
    }

    private Map<Integer, Set<Page>> getIndexOfLemmaAndPages(Set<Index> indexSet) {
        Map<Integer, Set<Page>> indexOfLemmaAndPages = new HashMap<>();
        indexOfLemmaAndPages.put(0, indexSet.stream().map(Index::getPage).collect(Collectors.toSet()));
        for (int i = 1; i < foundFilteredLemmasFromDB.size() - 1; i++) {
            Set<Page> pagesOfPreviousLemma = indexOfLemmaAndPages.get(i - 1);
            Set<Page> pagesOfThisLemma = indexService.getAllIndexesByLemma(foundFilteredLemmasFromDBIterator.next())
                    .stream().map(Index::getPage).collect(Collectors.toSet());
            Set<Page> result = pagesOfThisLemma.stream().filter(pagesOfPreviousLemma::contains)
                    .peek(pagesOfPreviousLemma::remove).collect(Collectors.toSet());
            if (result.size() == 0) {
                break;
            } else {
                indexOfLemmaAndPages.put(i, result);
            }
            if (pagesOfPreviousLemma.size() == 0) {
                indexOfLemmaAndPages.remove(i - 1);
            } else {
                indexOfLemmaAndPages.put(i - 1, pagesOfPreviousLemma);
            }
        }

        return indexOfLemmaAndPages;
    }

    private StringBuilder buildSnippet(Page page) {
        StringBuilder snippet = new StringBuilder();

        String pageContent = page.getContent();

        Set<Lemma> foundFilteredLemmasByPage = getFoundFilteredLemmasByPage(page);

        Set<String> contentWordsSorted = getContentWordsSorted(foundFilteredLemmasByPage, pageContent);

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

    private Set<Lemma> getFoundFilteredLemmasByPage(Page page) {
        return foundFilteredLemmasFromDB.stream()
                .filter(lemma -> indexService.getAllIndexesByPage(page).stream()
                        .map(index -> index.getLemma().getLemma()).toList().contains(lemma.getLemma()))
                .collect(Collectors.toSet());
    }

    private SortedSet<String> getContentWordsSorted(Set<Lemma> foundFilteredLemmasByPage, String pageContent) {
        return lemmaService.getFilteredWordAndLemma(pageContent).entrySet().stream()
                .filter(entry -> foundFilteredLemmasByPage.stream().map(Lemma::getLemma)
                        .toList().contains(entry.getValue())).map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt(word -> pageContent.toLowerCase().indexOf(word)))//fixme does it work?
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public double getPercentOfPages() {
        return percentOfPages;
    }

    public void setPercentOfPages(double percentOfPages) {
        this.percentOfPages = percentOfPages;
    }
}
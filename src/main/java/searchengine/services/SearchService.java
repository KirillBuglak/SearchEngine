package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchPageData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private LemmaService lemmaService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private IndexService indexService;
    private final List<SearchPageData> dataList = new ArrayList<>();
    private List<String> foundLemmasFromDB;
    public SearchResponse getSearch(String query, String searchSiteUrl) {//fixme SearchResponse getSearch(String query, String searchSiteUrl){return SearchResponse getSearch(String query, String searchSiteUrl, offset = 0, limit = 20) }
        SearchResponse response = new SearchResponse();

        Site searchSite = siteService.getSiteByURL(searchSiteUrl);
        foundLemmasFromDB = lemmaService.getFilteredLemmas(query).stream()
                .filter(lemma -> lemmaService.isLemmaPresent(lemma)).toList();

        response.setResult(foundLemmasFromDB.size() != 0);

        List<Integer> lemmasIds = lemmaService.getLemmasByListOfWords(foundLemmasFromDB).stream().map(Lemma::getId)
                .distinct().toList();
        List<Index> allIndexesByLemmaIds = indexService.getAllIndexesByLemmaIds(lemmasIds);
        List<Index> searchSiteIndexes = allIndexesByLemmaIds.stream().filter(index -> index.getPage().getSite()
                .getUrl().equals(searchSiteUrl)).toList();

        boolean searchSiteHasResults = searchSiteIndexes.size() != 0;
        if (response.isResult() && searchSite == null) {
            addSearchPageDataInfo(getPageRelInfo(allIndexesByLemmaIds));
            response.setCount(indexService.indexesCountByLemmasIds(lemmasIds));
            response.setData(dataList);
        } else if (
//                response.isResult() &&
                searchSite != null && searchSiteHasResults) {
            addSearchPageDataInfo(getPageRelInfo(searchSiteIndexes));
            response.setCount(searchSiteIndexes.size());
            response.setData(dataList);
        } else if (query.isBlank()) { //fixme have to deal with case when query is null
            response.setError("Задан пустой поисковый запрос");
        } else {
            response.setError("По вашему запросу ничего не нашлось");
        }
        return response;
    }

    private LinkedHashMap<Page, Float> getPageRelInfo(List<Index> indexesList) {
        Map<Page, Float> pageAndAbsRel = new ConcurrentHashMap<>();
        LinkedHashMap<Page, Float> sortedPageAndRelatRel = new LinkedHashMap<>();
        indexesList.forEach(index -> {
            Page pageFromIndex = index.getPage();
            if (pageAndAbsRel.containsKey(pageFromIndex)) {
                pageAndAbsRel.put(pageFromIndex, pageAndAbsRel.get(pageFromIndex) + index.getRank());
            } else {
                pageAndAbsRel.put(pageFromIndex, index.getRank());
            }
        });
        LinkedHashMap<Page, Float> sortedPageAndAbsRel = pageAndAbsRel.entrySet().stream()
                .sorted((o1, o2) -> (int) (o2.getValue() - o1.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        Float divisor = sortedPageAndAbsRel.values().iterator().next();
        sortedPageAndAbsRel.forEach((k, v) -> sortedPageAndRelatRel.put(k, v / divisor));
        return sortedPageAndRelatRel;
    }

    @SneakyThrows
    private void addSearchPageDataInfo(LinkedHashMap<Page, Float> pageRelInfo) {
        pageRelInfo.forEach((page, relevance) -> {
            StringBuilder snippet = buildSnippet(page);
            SearchPageData data = null;
            try {
                data = new SearchPageData(page.getSite().getUrl(),
                        page.getSite().getName(), page.getPath(), Jsoup.connect(page
                        .getPath()).get().title(), snippet.toString(), relevance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dataList.add(data);
        });
    }
    private StringBuilder buildSnippet(Page page) {//fixme work on firstWord and contentWord
        StringBuilder snippet = new StringBuilder();

        List<String> pageLemmasForSearch = lemmaService.getPagesAndLemmas().entrySet().stream()
                .filter(entry -> entry.getKey().getPath().equals(page.getPath()))
                .map(Map.Entry::getValue).flatMap(List::stream)
                .filter(lemma -> lemma.getFrequency()<=50)
                .map(Lemma::getLemma).toList();

        List<String> foundLemmas = foundLemmasFromDB.stream().filter(pageLemmasForSearch::contains).toList();
        List<String> contentWords = lemmaService.getFilteredWordAndLemma(page.getContent()).entrySet().stream()
                .filter(entry -> foundLemmas.contains(entry.getValue())).map(entry -> entry.getKey()).toList();
        String pageContent = page.getContent();

        contentWords.forEach(contentWord->{
        int start = pageContent.toLowerCase().indexOf(contentWord);
        int finish = start + contentWord.length();
            int symbolsBeforeStart = Math.max(pageContent.toLowerCase().indexOf(contentWord) - 20, 0);
            int symbolsAfterFinish = Math.min(start + contentWord.length() + 200, pageContent.length() - 1);
        String substring = pageContent.substring(start- symbolsBeforeStart, finish + symbolsAfterFinish)
                .replaceAll(pageContent.substring(start, finish),
                        "<b>" + pageContent.substring(start, finish) + "</b>");
        snippet.append("...").append(substring).append("...");
        });

        return snippet;
    }
}
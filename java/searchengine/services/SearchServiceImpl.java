package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchPageData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private LemmaService lemmaService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private IndexService indexService;


    @Override
    public SearchResponse getSearch(String query, String searchSiteUrl) {//fixme maybe add @offset and @limit parameters

        SearchResponse response = new SearchResponse();
        List<SearchPageData> dataList = new ArrayList<>();

        Site searchSite = siteService.getSiteByURL(searchSiteUrl);
        List<String> foundLemmas = lemmaService.getFilteredLemmas(query).stream()
                .filter(word -> lemmaService.isLemmaPresent(word)).toList();

        response.setResult(foundLemmas.size() != 0);

        List<Integer> lemmasIds = lemmaService.getLemmasByListOfWords(foundLemmas).stream().map(Lemma::getId)
                .distinct().toList();
        List<Index> allIndexesByLemmaIds = indexService.getAllIndexesByLemmaIds(lemmasIds);
        List<Index> searchSiteIndexes = allIndexesByLemmaIds.stream().filter(index -> index.getPage().getSite()
                .getUrl().equals(searchSiteUrl)).toList();

        boolean searchSiteHasResults = searchSiteIndexes.size() != 0;
        if (response.isResult() && searchSite == null) {
            allIndexesByLemmaIds.forEach(index -> {
                addSearchPageDataInfo(dataList, index);
            });
            response.setCount(indexService.indexesCountByLemmasIds(lemmasIds));
            response.setData(dataList);
        } else if (
//                response.isResult() &&
                searchSite != null && searchSiteHasResults) {
            searchSiteIndexes.forEach(index -> {
                addSearchPageDataInfo(dataList, index);
            });
            response.setCount(searchSiteIndexes.size());
            response.setData(dataList);
        } else if (query.isBlank()) { //fixme have to deal with case when query is null
            response.setError("Задан пустой поисковый запрос");
        } else {
            response.setError("По вашему запросу ничего не нашлось");
        }
        return response;
    }

    @SneakyThrows
    private void addSearchPageDataInfo(List<SearchPageData> dataList, Index index) {
        SearchPageData data = new SearchPageData(index.getPage().getSite().getUrl(),
                index.getPage().getSite().getName(), index.getPage().getPath(), Jsoup.connect(index.getPage().getSite()
                .getUrl()).get().title(),//fixme have to use page's URL instead of site's
                index.getPage().getContent(), 0.8f);
        dataList.add(data);
    }
}
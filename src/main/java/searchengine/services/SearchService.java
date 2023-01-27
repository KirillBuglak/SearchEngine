package searchengine.services;

import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConfigurationProperties(prefix = "search-service")
public class SearchService {

    private double percentOfPages;
    private int numberOfSymbols;
    private final LemmaService lemmaService;
    private final SiteService siteService;
    private final IndexService indexService;
    private final PageService pageService;
    private SortedSet<Lemma> foundFilteredLemmasFromDB;
    private List<SearchPageData> allSitesData;
    private final String savedQuery;


    public SearchService(LemmaService lemmaService,
                         SiteService siteService,
                         IndexService indexService,
                         PageService pageService) {
        this.lemmaService = lemmaService;
        this.siteService = siteService;
        this.indexService = indexService;
        this.pageService = pageService;
        allSitesData = new ArrayList<>();
        savedQuery = "";
    }

    public SearchResponse getSearch(String query, String searchSiteUrl, int offset, int limit) {
        SearchResponse response = new SearchResponse();

        Predicate<String> filterTooCommonLemmas = fLemma -> {
            AtomicInteger numberOfPagesContainingLemma = new AtomicInteger();
            lemmaService.getLemmasByString(fLemma).forEach(lemma ->
                    numberOfPagesContainingLemma.addAndGet(indexService.getAllIndexesByLemma(lemma).size()));
            return (double) numberOfPagesContainingLemma.get() / pageService.getAllPages().size() < percentOfPages;
        };

        Site searchSite = siteService.getSiteByURL(searchSiteUrl);
        foundFilteredLemmasFromDB = lemmaService.getSortedLemmasByListOfWords(lemmaService.getFilteredLemmas(query)
                .stream().filter(filterTooCommonLemmas).toList());

        Set<Lemma> firstLemmasForAllSites = new HashSet<>();
        siteService.getAllSites().forEach(site -> foundFilteredLemmasFromDB.stream()
                .filter(lemma -> lemma.getSite().equals(site)).findFirst().ifPresent(firstLemmasForAllSites::add));

        Set<Page> allPagesByFirstLemma = firstLemmasForAllSites.stream().map(indexService::getAllIndexesByLemma)
                .flatMap(Collection::stream).map(Index::getPage).collect(Collectors.toSet());
        if (query.isBlank()) {
            response.setError("Задан пустой поисковый запрос");
            return response;
        } else if (foundFilteredLemmasFromDB.isEmpty() || (searchSite != null
                && foundFilteredLemmasFromDB.stream().filter(lemma -> lemma.getSite().equals(searchSite)).toList()
                .isEmpty())) {
            response.setError("По вашему запросу ничего не нашлось");
            return response;
        } else if (searchSite != null) {
            Set<Page> searchSitePagesByFirstLemma = allPagesByFirstLemma.stream()
                    .filter(page -> page.getSite().equals(searchSite)).collect(Collectors.toSet());
            List<SearchPageData> data = allSitesData.isEmpty() && !query.equals(savedQuery)
                    ?addSearchPageDataInfo(getPageRelInfo(searchSitePagesByFirstLemma))
                    :allSitesData.stream().filter(d->d.getSite().equals(searchSite.getUrl()))
                    .collect(Collectors.toList());
            response.setData(data.subList(offset, Math.min((offset + limit), data.size())));
            response.setCount(data.size());
            response.setResult(response.getCount() != 0);
        } else {
            allSitesData = addSearchPageDataInfo(getPageRelInfo(allPagesByFirstLemma));
            response.setData(allSitesData.subList(offset, Math.min((offset + limit), allSitesData.size())));
            response.setCount(allSitesData.size());
            response.setResult(response.getCount() != 0);
        }
        return response;
    }

    private List<SearchPageData> addSearchPageDataInfo(Map<Page, Float> pageRelInfo) {
        List<SearchPageData> dataList = new ArrayList<>();
        pageRelInfo.forEach((page, relevance) -> {
            StringBuilder snippet = buildSnippet(page);
            if (snippet.toString().equals("")){
                return;
            }
            SearchPageData data = null;
            try {
                data = new SearchPageData(page.getSite().getUrl(),
                        page.getSite().getName(), page.getPath(), Jsoup.connect(page
                        .getFullPath()).get().title(), snippet.toString(), relevance);
            } catch (IOException e) {
                log.error("Error in addSearchPageDataInfo method, {}", e.toString());
            }
            dataList.add(data);
        });
        Collections.sort(dataList);
        return dataList;
    }

    private Map<Page, Float> getPageRelInfo(Set<Page> pagesByFirstLemma) {
        Map<Page, Float> pagesAndRanks = new HashMap<>();
        pagesByFirstLemma.forEach(page -> {
            float rank = foundFilteredLemmasFromDB.stream().map(indexService::getAllIndexesByLemma)
                    .flatMap(Collection::stream)
                    .filter(index -> index.getPage().equals(page)).map(Index::getRank).reduce(0f, Float::sum);
            pagesAndRanks.put(page, rank);
        });
        float divisor = pagesAndRanks.values().stream().reduce(Math::max).orElse(0f);
        return pagesAndRanks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() / divisor,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    private StringBuilder buildSnippet(Page page) {

        StringBuilder snippet = new StringBuilder();
        String pageContent = page.getContent();
        Set<Lemma> foundFilteredLemmasByPage = getFoundFilteredLemmasByPage(page);
        SortedMap<Integer, Integer> indexAndWordLength = getTextIndexAndWordLengthSorted(foundFilteredLemmasByPage,
                pageContent);

        indexAndWordLength.forEach((index, wordLength) -> {

            String word = pageContent.substring(index, index + wordLength);

            if (snippet.toString().toLowerCase().contains(word)
                    && getProperIndex(snippet.toString().toLowerCase(), word) != -1) {
                snippet.replace(snippet.toString().indexOf(word), snippet.toString().indexOf(word) + wordLength,
                        "<b>" + word + "</b>");
            } else if (snippet.length() < numberOfSymbols) {
                snippet.append(snippet.length() > 0 ? "" : "...")
                        .append(pageContent.subSequence(Math.max(index - 20, 0), index))
                        .append("<b>")
                        .append(word)
                        .append("</b>")
                        .append(pageContent.subSequence(index + wordLength,
                                Math.min(index + wordLength + (indexAndWordLength.size() > 2 ? 50 : 100),
                                        pageContent.length() - 1)))
                        .append("...");
            }
        });

        return snippet;
    }

    private Set<Lemma> getFoundFilteredLemmasByPage(Page page) {
        return foundFilteredLemmasFromDB.stream()
                .filter(lemma -> indexService.getIndexByPageAndLemma(page, lemma) != null)
                .map(lemma -> indexService.getIndexByPageAndLemma(page, lemma))
                .map(Index::getLemma).collect(Collectors.toSet());
    }

    private SortedMap<Integer, Integer> getTextIndexAndWordLengthSorted(Set<Lemma> foundFilteredLemmasByPage,
                                                                        String pageContent) {
        Set<String> contentWords = lemmaService.getFilteredWordAndLemma(pageContent).entrySet().stream()
                .filter(entry -> foundFilteredLemmasByPage.stream().map(Lemma::getLemma).toList()
                        .contains(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
        Map<Integer, Integer> textIndexAndContentWord = new HashMap<>();
        contentWords.forEach(word -> {
            int index = getProperIndex(pageContent.toLowerCase(), word);
            textIndexAndContentWord.put(index, word.length());
        });
        return new TreeMap<>(textIndexAndContentWord);
    }

    private int getProperIndex(String lowerCaseContent, String word) {
        try {
            int index = lowerCaseContent.indexOf(word);
            while ((!lowerCaseContent.substring(index == 0 ? index : index - 1,
                            Math.min((index + word.length() + 1), lowerCaseContent.length() - 1))
                    .matches(lemmaService.getRegex() + "+" + word + lemmaService.getRegex() + "*"))
                    || (!lowerCaseContent.substring(index == 0 ? index : index - 1,
                            Math.min((index + word.length() + 1), lowerCaseContent.length() - 1))
                    .matches(lemmaService.getRegex() + "*" + word + lemmaService.getRegex() + "+"))) {
                index = lowerCaseContent.indexOf(word, index + 1);
            }
            return index;
        } catch (Exception e) {
            return -1;
        }
    }

    public double getPercentOfPages() {
        return percentOfPages;
    }

    public void setPercentOfPages(double percentOfPages) {
        this.percentOfPages = percentOfPages;
    }

    public int getNumberOfSymbols() {
        return numberOfSymbols;
    }

    public void setNumberOfSymbols(int numberOfSymbols) {
        this.numberOfSymbols = numberOfSymbols;
    }
}
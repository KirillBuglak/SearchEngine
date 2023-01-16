package searchengine.dto.search;

import java.util.Comparator;

public class SearchPageData implements Comparable<SearchPageData> {
    private String site;
    private final String siteName;
    private final String uri;
    private final String title;
    private final String snippet;
    private final float relevance;

    public SearchPageData(String site, String siteName, String uri, String title, String snippet, float relevance) {
        this.site = site;
        this.siteName = siteName;
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public float getRelevance() {
        return relevance;
    }

    @Override
    public int compareTo(SearchPageData s) {
        return Comparator.comparing(SearchPageData::getRelevance).reversed()
                .thenComparing(SearchPageData::getSiteName)
                .thenComparing(SearchPageData::getTitle)
                .thenComparing(SearchPageData::getUri)
                .compare(this, s);
    }
    public String getSnippet() {
        return snippet;
    }
}

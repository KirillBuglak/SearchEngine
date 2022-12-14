package searchengine.dto.search;

import java.util.Comparator;

public class SearchPageData implements Comparable<SearchPageData>  {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private float relevance;

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

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public float getRelevance() {
        return relevance;
    }

    public void setRelevance(float relevance) {
        this.relevance = relevance;
    }
    @Override
    public int compareTo(SearchPageData s) {
        return Comparator.comparing(SearchPageData::getRelevance).reversed()
                .thenComparing(SearchPageData::getSiteName)
                .thenComparing(SearchPageData::getTitle)
                .thenComparing(SearchPageData::getUri)
                .compare(this, s);
    }
}

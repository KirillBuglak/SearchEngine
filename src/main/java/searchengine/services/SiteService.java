package searchengine.services;

import searchengine.model.Site;
import searchengine.model.Status;

import java.util.List;

public interface SiteService {
    void saveSiteIndexing(String url, String name);
    void saveSiteIndexedOrFailed(String url);
    boolean isThereTheSite(String pagePath);
    Site getSiteByURL(String url);
    Site getSiteByPagePath(String pagePath);
    List<Site> getAllSites();
    void deleteAllSites();
    public void saveSiteWithNewStatus(Site site, Status status);
}

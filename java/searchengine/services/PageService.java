package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Set;

public interface PageService {
    void savePage(Site site, String path, int code, String content);

    void save(Page page);

    void saveAllPages(Set<Page> pages);

    void deleteAllPages();

    void deleteThePageByPath(String path);

    Page getPageByPath(String path);

    List<Page> getAllPages();
    List<Page> getAllPagesBySite(Site site);
}

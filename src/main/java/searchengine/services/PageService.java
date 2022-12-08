package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.util.List;
import java.util.Set;

@Service
public class PageService {
    @Autowired
    private PageRepository pageRepository;

    public void savePage(Site site, String path, int code, String content) {
        if (pageRepository.findByPath(path) == null) {
            Page page = new Page(site, path, code, content);
            pageRepository.save(page);
        }
    }

    public void save(Page page) {
        pageRepository.save(page);
    }

    public void saveAllPages(Set<Page> pages) {
        pageRepository.saveAllAndFlush(pages);
    }

    public void deleteAllPages() {
        pageRepository.deleteAll();
    }

    public void deleteThePageByPath(String path) {
        pageRepository.delete(getPageByPath(path));
    }

    public Page getPageByPath(String path) {
        return pageRepository.findByPath(path);
    }

    public List<Page> getAllPages() {
        return pageRepository.findAll();
    }

    public List<Page> getAllPagesBySite(Site site) {
        return pageRepository.findAllBySite(site);
    }

}

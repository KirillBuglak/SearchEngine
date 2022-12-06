package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;

import java.util.List;
import java.util.Set;

@Service
public class PageServiceImpl implements PageService {
    @Autowired
    private PageRepository pageRepository;

    @Override
    public void savePage(Site site, String path, int code, String content) {
        if (pageRepository.findByPath(path) == null) {
            Page page = new Page(site, path, code, content);
            pageRepository.save(page);
        }
    }

    @Override
    public void save(Page page) {
        pageRepository.save(page);
    }

    @Override
    public void saveAllPages(Set<Page> pages) {
        pageRepository.saveAllAndFlush(pages);
    }

    @Override
    public void deleteAllPages() {
        pageRepository.deleteAll();
    }

    @Override
    public void deleteThePageByPath(String path) {
        pageRepository.delete(getPageByPath(path));
    }

    @Override
    public Page getPageByPath(String path) {
        return pageRepository.findByPath(path);
    }

    @Override
    public List<Page> getAllPages() {
        return pageRepository.findAll();
    }

    @Override
    public List<Page> getAllPagesBySite(Site site) {
        return pageRepository.findAllBySite(site);
    }

}

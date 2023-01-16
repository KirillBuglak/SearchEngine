package searchengine.services.modelServices;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.util.List;
import java.util.Set;

@Service
@ConfigurationProperties(prefix = "jsoup-settings")
public class PageService {
    private String userAgent;
    private String referrer;
    private final PageRepository pageRepository;

    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public void save(Page page) {
        pageRepository.save(page);
    }

    public void deleteAllPages() {
        pageRepository.deleteAllInBatch();
    }

    public void deleteThePageByPathAndSite(String path, Site site) {
        pageRepository.delete(getPageByPathAndSite(path, site));
    }

    public Page getPageByPathAndSite(String path, Site site) {
        return pageRepository.findByPathAndSite(path, site);
    }

    public List<Page> getAllPages() {
        return pageRepository.findAll();
    }

    public Set<Page> getAllPagesBySite(Site site) {
        return pageRepository.findAllBySite(site);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }
}

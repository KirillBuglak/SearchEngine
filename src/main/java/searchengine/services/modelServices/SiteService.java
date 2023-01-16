package searchengine.services.modelServices;

import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.StopIndexingResponse;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.SiteRepository;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class SiteService {
    private final SiteRepository siteRepository;
    private final SitesList sites;

    public SiteService(SiteRepository siteRepository, SitesList sites) {
        this.siteRepository = siteRepository;
        this.sites = sites;
    }

    public void updateTimeOfSite(Site site) {
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    public void saveSitesIndexing() {
        sites.getSites().forEach(site -> {
            if (siteRepository.findByUrl(site.getUrl()) != null) {
                return;
            }
            siteRepository.save(new Site(site.getUrl(), site.getName()));
        });
    }

    public void saveSiteIndexedOrFailed(Site site) {
        if (site.getLastError().equals("")) {
            saveSiteWithNewStatus(site, Status.INDEXED);
        } else {
            saveSiteWithNewStatus(site, Status.FAILED);
        }
    }

    private void saveSiteWithNewStatus(Site site, Status status) {
        site.setStatus(status);
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    public Site getSiteByPageUrl(String url) {
        return siteRepository.findAll().stream()
                .filter(site -> url.contains(site.getUrl())).findFirst().orElse(null);
    }

    public Site getSiteByURL(String url) {
        return siteRepository.findByUrl(url);
    }

    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    public void deleteAllSites() {
        siteRepository.deleteAll();
    }

}

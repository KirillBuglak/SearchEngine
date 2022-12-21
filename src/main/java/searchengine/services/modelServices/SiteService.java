package searchengine.services.modelServices;

import org.springframework.stereotype.Service;
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

    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public void updateTimeOfSite(Site site) {
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    public void saveSiteIndexing(String url, String name) {
        if (siteRepository.findByName(name) != null) {
            return;
        }
        siteRepository.save(new Site(url, name));
    }

    public void saveSiteIndexedOrFailed(Site site) {
        if (site.getLastError() == null) {
            saveSiteWithNewStatus(site, Status.INDEXED);
        } else {
            saveSiteWithNewStatus(site, Status.FAILED);
        }
    }

    public void saveSiteWithNewStatus(Site site, Status status) {
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
    public static boolean stoppedByUser(Site site) {
        if(StopIndexingResponse.getResult()) {
            if (!(Objects.equals(site.getLastError(), "Индексация остановлена пользователем"))) {
                site.setLastError("Индексация остановлена пользователем");
            }
            return true;
        }
        return false;
    }
}

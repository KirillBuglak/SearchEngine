package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.util.Date;
import java.util.List;

@Service
public class SiteServiceImpl implements SiteService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageService pageService;
    @Autowired
    private IndexService indexService;

    @Override
    public void saveSiteIndexing(String url, String name) {
        if (siteRepository.findByName(name) != null) {//fixme may need to delete it cause stopIndexing may clear sites table
            return;
        }
        siteRepository.save(new Site(url, name));
    }

    @Override
    public void saveSiteIndexedOrFailed(String url) {
        if (areAllPagesIndexed(siteRepository.findByUrl(url))) {
            saveSiteWithNewStatus(siteRepository.findByUrl(url), Status.INDEXED);
        } else {
            saveSiteWithNewStatus(siteRepository.findByUrl(url), Status.FAILED);
        }
    }

    @Override
    public void saveSiteWithNewStatus(Site site, Status status) {
        site.setStatus(status);
        site.setStatusTime(new Date());
        siteRepository.save(site);
    }

    private boolean areAllPagesIndexed(Site site) {
        List<Index> indexes = pageService.getAllPagesBySite(site).stream()
                .map(page -> indexService.getAllIndexesByPage(page)).flatMap(List::stream).toList();
        return indexes.size() != 0 && indexes.stream().map(Index::getPage).distinct().toList().size()
                == pageService.getAllPagesBySite(site).size();//fixme may need to check it with sites.size()
    }

    @Override
    public boolean isThereTheSite(String pagePath) {
        if (pagePath.length() == 0) {
            return false;
        }
        int start = pagePath.indexOf("//") + 2;//fixme may need regex
        int end = pagePath.indexOf("/", start);
        return siteRepository.findByUrl(pagePath.substring(0, end)) != null;
    }

    @Override
    public Site getSiteByURL(String url) {
        return siteRepository.findByUrl(url);
    }

    @Override
    public Site getSiteByPagePath(String pagePath) {
        int start = pagePath.indexOf("//") + 2;//fixme may need regex
        int end = pagePath.indexOf("/", start);
        return siteRepository.findByUrl(pagePath.substring(0, end));
    }

    @Override
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    @Override
    public void deleteAllSites() {
        siteRepository.deleteAll();
    }
}

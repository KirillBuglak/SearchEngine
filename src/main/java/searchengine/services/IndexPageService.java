package searchengine.services;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.PageLemmaIndexDBSave;
import searchengine.dto.IndexPageResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

@Service
public class IndexPageService {
    private static final String pagePathRegex = "https?://([-\\w.+=&?#$%]+/?)+";//todo modify this
    private final PageService pageService;
    private final SiteService siteService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final PageLemmaIndexDBSave pageLemmaIndexDBSave;

    public IndexPageService(PageService pageService,
                            SiteService siteService,
                            LemmaService lemmaService,
                            IndexService indexService,
                            PageLemmaIndexDBSave pageLemmaIndexDBSave) {
        this.pageService = pageService;
        this.siteService = siteService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.pageLemmaIndexDBSave = pageLemmaIndexDBSave;
    }

    @SneakyThrows
    public IndexPageResponse getIndexPage(String url) {
        IndexPageResponse response = new IndexPageResponse();
        Site site = siteService.getSiteByPageUrl(url);
        if (site != null) {
            String pagePath = url.substring(url.indexOf(site.getUrl()) + site.getUrl().length() - 1);
            Page pageFormDB = pageService
                    .getPageByPath(pagePath);
            if (pageFormDB != null) {//fixme && may need to check it by status - just calling this page
                indexService.deleteIndexesByPage(pageFormDB);
                lemmaService.deleteLemmasByPage(pageFormDB);
                pageService.deleteThePageByPath(pagePath);
            }
            pageLemmaIndexDBSave.requestDoc(url);//fixme may need to get rid of this
            Page newPage = new Page(site, pagePath, pageLemmaIndexDBSave.getStatusCode(),
                    pageLemmaIndexDBSave.getContent().toString());
            pageService.save(newPage);
                Thread thread = new Thread(() -> {
            lemmaService.saveLemmas(pageService.getPageByPath(pagePath));//fixme work on it
            indexService.saveIndexes(pageService.getPageByPath(pagePath));
            response.setResult(true);
            siteService.saveSiteWithNewStatus(site, Status.INDEXED);
                });
                thread.start();
                thread.join();
        } else if (!url.matches(pagePathRegex)) {
            response.setError("Проверьте правильность ввода адреса страницы");
        } else {//fixme may need to add if with a check for http status
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return response;
    }
}
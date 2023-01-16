package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.PageLemmaIndexDBSave;
import searchengine.dto.IndexPageResponse;
import searchengine.dto.StartIndexingResponse;
import searchengine.dto.StopIndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.modelServices.IndexService;
import searchengine.services.modelServices.LemmaService;
import searchengine.services.modelServices.PageService;
import searchengine.services.modelServices.SiteService;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IndexingService {
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final StopIndexingResponse stopResponse;
    private final StartIndexingResponse startResponse;
    private final PageLemmaIndexDBSave justGetFields;

    public IndexingService(SiteService siteService,
                           PageService pageService,
                           LemmaService lemmaService,
                           IndexService indexService,
                           PageLemmaIndexDBSave justGetFields
    ) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.justGetFields = justGetFields;
        stopResponse = new StopIndexingResponse();
        startResponse = new StartIndexingResponse();
    }

    public IndexPageResponse getIndexPage(String url) {
        IndexPageResponse pageResponse = new IndexPageResponse();
        if (url.isBlank() || getPageStatusCode(url) != 200 || !url.matches(justGetFields.getPageRegEx())) {
            pageResponse.setError("Проверьте правильность ввода адреса страницы");
        } else if (siteService.getAllSites().stream().map(Site::getUrl).filter(url::contains).toList().size() == 0) {
            pageResponse.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        } else {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                Site site = siteService.getSiteByPageUrl(url);
                if (site != null) {
                    String pagePath = url.substring(url.indexOf(site.getUrl()) + site.getUrl().length());
                    Page pageFormDB = pageService
                            .getPageByPathAndSite(pagePath, site);
                    if (pageFormDB != null) {
                        indexService.deleteIndexesByPage(pageFormDB);
                        lemmaService.deleteLemmasByPage(pageFormDB);
                        pageService.deleteThePageByPathAndSite(pagePath,site);
                        log.info("Page \"{}\" with it's lemmas and indexes are deleted", pagePath);
                    }
                    Page newPage = new Page(site, pagePath, 0, null);
                    justGetFields.setPage(newPage);
                    justGetFields.setPageCodeAndContent(newPage, justGetFields.getConnection(url));
                    pageService.save(newPage);
                    lemmaService.saveLemmas(newPage);
                    indexService.saveIndexes(newPage);
                    siteService.saveSiteIndexedOrFailed(site);
                }
            });
            executorService.shutdown();
            pageResponse.setResult(true);
        }
        getStopIndexing();
        return pageResponse;
    }

    public StartIndexingResponse getStartIndexing() {
        if ((stopResponse.isResult() || stopResponse.getError() != null) && !startResponse.isResult()) {
            startResponsePositive();
            deleteEverything();
            siteService.saveSitesIndexing();
            siteService.getAllSites().forEach(site -> {
                Thread thread = new Thread(() -> saveToDB(site), site.getName());
                thread.start();
            });
        } else {
            startResponseNegative("Индексация уже запущена");
        }
        return startResponse;
    }

    private void startResponsePositive() {
        stopResponse.setResult(false);
        startResponse.setResult(true);
        startResponse.setError(null);
    }

    private void startResponseNegative(String errorMessage) {
        startResponse.setResult(false);
        startResponse.setError(errorMessage);
        stopResponse.setError(null);
    }

    public StopIndexingResponse getStopIndexing() {
        if ((startResponse.isResult() || startResponse.getError() != null) && !stopResponse.isResult()
        ) {
            startResponse.setResult(false);
            stopResponse.setResult(true);
            stopResponse.setError(null);
        } else {
            stopResponse.setResult(false);
            stopResponse.setError("Индексация не запущена или завершена");
            startResponse.setError(null);
        }
        return stopResponse;
    }

    private void deleteEverything() {
        if (pageService.getAllPages().size() != 0) {
            indexService.deleteAllIndexes();
            lemmaService.deleteAllLemmas();
            pageService.deleteAllPages();
            siteService.deleteAllSites();
            log.info("All pages, lemmas and indexes are deleted");
        }
    }

    private void saveToDB(Site site) {
        long start = System.currentTimeMillis();
        ForkJoinPool pool = new ForkJoinPool();
        PageLemmaIndexDBSave task = new PageLemmaIndexDBSave(pageService, lemmaService, indexService, siteService);
        task.setPage(new Page(site, site.getUrl(), 0, null));
        try {
            pool.execute(task);
            task.get();
        } catch (Exception e) {
            stoppedExceptionally(site, task);
            log.error("Error in saveToDB method, {}", e.toString());
        } finally {
            pool.shutdown();
            siteService.saveSiteIndexedOrFailed(site);
        }
        log.info("Site {} is indexed, count of pages - {}", site.getUrl(), pageService.getAllPagesBySite(site).size());
        log.info("Time Spent - {}s", (float) (System.currentTimeMillis() - start) / 1000);
        lastSiteIndexed();
    }

    private void lastSiteIndexed() {
        if (siteService.getAllSites().stream().filter(Site -> Site.getStatus()!= Status.INDEXING)
                .collect(Collectors.toSet()).size() == siteService.getAllSites().size()) {
        getStopIndexing();
        }
    }

    private void stoppedExceptionally(Site site, ForkJoinTask<?> task) {
        task.cancel(true);
        site.setLastError("Индексация остановлена по причине ошибки приложения");
    }

    private int getPageStatusCode(String url) {
        int pageStatusCode;
        try {
            pageStatusCode = justGetFields.getConnection(url).execute().statusCode();
        } catch (IOException e) {
            pageStatusCode = 404;
            log.error("Error in getPageStatusCode method, {}", e.toString());
        }
        return pageStatusCode;
    }
}
package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.PageLemmaIndexDBSave;
import searchengine.config.SitesList;
import searchengine.dto.IndexPageResponse;
import searchengine.dto.StartIndexingResponse;
import searchengine.dto.StopIndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.modelServices.IndexService;
import searchengine.services.modelServices.LemmaService;
import searchengine.services.modelServices.PageService;
import searchengine.services.modelServices.SiteService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class IndexingService {
    private final static List<Thread> threads = new ArrayList<>();//fixme maybe threadGroups
    private final SitesList sites;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final StopIndexingResponse stopResponse;
    private final StartIndexingResponse startResponse;

    public IndexingService(SitesList sites,
                           SiteService siteService,
                           PageService pageService,
                           LemmaService lemmaService,
                           IndexService indexService,
                           StopIndexingResponse stopResponse,
                           StartIndexingResponse startResponse) {
        this.sites = sites;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.stopResponse = stopResponse;
        this.startResponse = startResponse;
    }

    public IndexPageResponse getIndexPage(String url) {
        IndexPageResponse response = new IndexPageResponse();
        PageLemmaIndexDBSave justGetFields = new PageLemmaIndexDBSave(this.pageService, this.lemmaService,
                this.indexService, this.siteService);
        if (getPageStatusCode(url) != 200 || !url.matches(justGetFields.getPageRegEx())) {
            response.setError("Проверьте правильность ввода адреса страницы");
        } else if (siteService.getAllSites().stream().map(Site::getUrl).filter(url::contains).toList().size() == 0) {
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        } else {
            response.setResult(true);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                Site site = siteService.getSiteByPageUrl(url);
                if (site != null) {
                    String pagePath = url.substring(url.indexOf(site.getUrl()) + site.getUrl().length() - 1);
                    Page pageFormDB = pageService
                            .getPageByPath(pagePath);
                    if (pageFormDB != null) {
                        indexService.deleteIndexesByPage(pageFormDB);
                        lemmaService.deleteLemmasByPage(pageFormDB);
                        pageService.deleteThePageByPath(pagePath);
                    }
                    justGetFields.requestDoc(url);
                    Page newPage = new Page(site, pagePath, justGetFields.getStatusCode(),
                            justGetFields.getContent().toString());
                    pageService.save(newPage);
//fixme maybe synchronise
                    lemmaService.saveLemmas(newPage);
                    indexService.saveIndexes(newPage);
//fixme maybe synchronise
                    siteService.saveSiteIndexedOrFailed(site);
                }
            });
        }
        return response;
    }

    public StartIndexingResponse getStartIndexing() {
        if ((stopResponse.isResult() || stopResponse.getError() != null) && !startResponse.isResult()) {
            stopResponse.setResult(false);
            startResponse.setResult(true);
            startResponse.setError(null);
            deleteEverything();
            saveSitesIndexing();
            siteService.getAllSites().forEach(site -> {
                Thread thread = new Thread(() -> saveToDB(site), site.getName());
                threads.add(thread);
                thread.start();
            });
        } else {
            startResponseNegative("Индексация уже запущена");
        }
        return startResponse;
    }

    private void startResponseNegative(String errorMessage) {
        startResponse.setResult(false);
        startResponse.setError(errorMessage);
        stopResponse.setError(null);
    }

    public StopIndexingResponse getStopIndexing() {
        if ((startResponse.isResult() || startResponse.getError() != null) && !stopResponse.isResult()
                && threads.stream().filter(Thread::isAlive).toList().size() != 0) {
//            threads.stream().filter(Thread::isAlive).forEach(Thread::interrupt);
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
        }
    }

    private void saveSitesIndexing() {
        sites.getSites().forEach(siteConf -> siteService.saveSiteIndexing(siteConf.getUrl(), siteConf.getName()));
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
            e.printStackTrace();
            stoppedExceptionally(site, task);
//            task.cancel(true);
//            threads.forEach(Thread::interrupt);
            return;
        } finally {
            pool.shutdown();
            siteService.saveSiteIndexedOrFailed(site);
            startResponseNegative(site.getLastError());
            pressStopHere();
        }
        System.err.println(site.getUrl() + " - " + pageService.getAllPages().size() + " - SIZE");
        System.out.println("Time Spent - " + (System.currentTimeMillis() - start));
    }

    private void stoppedExceptionally(Site site, ForkJoinTask<?> task) {
        System.err.println("STOPPED EXCEPTIONALLY - " + Thread.currentThread().getName());
        task.cancel(true);
        site.setLastError("Индексация остановлена по причине ошибки");
    }

    private void pressStopHere() {
        try { //fixme may be put it in the final block with no check like isaLastSite
            //fixme press stopIndexing here
            Jsoup.connect("http://localhost:8080/api/stopIndexing").ignoreContentType(true)
                    .method(Connection.Method.GET).data("result", "true").execute();
            //fixme try to pass json object through url->HTTP method
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private int getPageStatusCode(String url) {
        return Jsoup.connect(url)
                .userAgent(pageService.getUserAgent())
                .referrer(pageService.getReferrer())
                .timeout(20000).execute().statusCode();
    }
}
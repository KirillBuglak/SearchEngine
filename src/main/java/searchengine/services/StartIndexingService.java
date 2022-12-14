package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.PageLemmaIndexDBSave;
import searchengine.config.SitesList;
import searchengine.dto.StartIndexingResponse;
import searchengine.dto.StopIndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

@Service
public class StartIndexingService {
    private final List<Thread> threads = new ArrayList<>();
    private final SitesList sites;
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final StopIndexingResponse stopResponse;
    private final StartIndexingResponse startResponse;

    public StartIndexingService(SitesList sites,
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

    public List<Thread> getThreads() {
        return threads;
    }

    private void deleteEverything() {
        if (indexService.getAllIndexes().size() != 0) {
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
            return;
        } finally {
            startResponseNegative(site.getLastError());
            pool.shutdown();
        }
        System.err.println(site.getUrl() + " - " + pageService.getAllPages().size() + " - SIZE");
        siteService.saveSiteWithNewStatus(site, Status.INDEXED);
        System.out.println("Time Spent - " + (System.currentTimeMillis() - start));
        if (isaLastSite(site)) {
            try {
                //fixme press stopIndexing here
                Jsoup.connect("http://localhost:8080/api/stopIndexing").ignoreContentType(true)
                        .method(Connection.Method.GET).data("result", "true").execute();
                //fixme try to pass json object through url->HTTP method
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean stoppedManually(Site site, ForkJoinTask<?> task) {
        if (Thread.currentThread().isInterrupted() && !startResponse.isResult()) {
            System.err.println("STOPPED - " + Thread.currentThread().getName());
            task.cancel(true);
            site.setLastError("Индексация остановлена пользователем");
            siteService.saveSiteWithNewStatus(site, Status.FAILED);
            return true;
        }
        return false;
    }

    private void stoppedExceptionally(Site site, ForkJoinTask<?> task) {
        System.err.println("STOPPED EXCEPTIONALLY - " + Thread.currentThread().getName());
        task.cancel(true);
        site.setLastError("Индексация остановлена по причине ошибки");
        siteService.saveSiteWithNewStatus(site, Status.FAILED);
    }

    private boolean isaLastSite(Site site) {
        return siteService.getAllSites().get(siteService.getAllSites().size() - 1) == site;
    }
}
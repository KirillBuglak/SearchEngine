package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.config.runAndFork.Recc;
import searchengine.config.runAndFork.ReccWithDBCheck;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.dto.stopIndexing.StopIndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class StartIndexingService {
    @Autowired
    private SitesList sites;
    @Autowired
    private SiteService siteService;
    @Autowired
    private PageService pageService;
    @Autowired
    private LemmaService lemmaService;
    @Autowired
    private IndexService indexService;
    @Autowired
    private StopIndexingResponse stopResponse;
    @Autowired
    private StartIndexingResponse startResponse;
    private List<Thread> threads = new ArrayList<>();
    private boolean finished;

    @SneakyThrows
    public StartIndexingResponse getStartIndexing() {
        if ((stopResponse.isResult() || stopResponse.getError() != null) && !startResponse.isResult()) {
            stopResponse.setResult(false);
            startResponse.setResult(true);
            startResponse.setError(null);
            deleteEverything();
            //saving indexing sites
            saveSitesIndexing();//fixme why i can't put it in mainLogicForTheSite?
            //saving indexing sites
            siteService.getAllSites().forEach(site -> {
                Thread thread = new Thread(() -> mainLogicForTheSite2(site), site.getName());//todo mainLogic change
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

    public boolean isFinished() {
        return finished;
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

    @SneakyThrows
    private void mainLogicForTheSite(Site site) {
        long start = System.currentTimeMillis();
        //add sites to database
        //getting all pages
        ForkJoinPool pool = new ForkJoinPool();
        Recc task = new Recc(new Page(site, site.getUrl(), 0, null), null);
        Set<Page> thisSitePages = pool.invoke(task);
        pool.shutdown();
        //getting all pages
        //save pages
//            savePagesHQLWorksOnlyWithEmbeddedId(thisSitePages);
//        pageService.saveAllPages(thisSitePages);
        //save pages
        thisSitePages.forEach(page -> {//fixme try to implement CompletableFuture to see the difference
            if (stoppedManually(site, task)) return;
            try {
                CompletableFuture.supplyAsync(
                        ()-> {
            //fixme save status time for site
            //fixme may need to use fork join pool for lemmas and indexes saves -> the same that created pages -> invokeAll or Smth
            //save page
            pageService.save(page);
            //save page
            //save lemmas
            lemmaService.saveLemmas(page);
            //save lemmas
            //save indexes
            indexService.saveIndex(page);
            //save indexes
                            return true;
                        },Executors.newWorkStealingPool()).get();
            } catch (Exception e) {
                e.printStackTrace();
                stoppedExceptionally(site,task);
            }
            finally {
                startResponseNegative(site.getLastError());
            }
        });
        System.err.println(site.getUrl() + " - " + thisSitePages.size() + " - SIZE");//fixme not the right SIZE
        siteService.saveSiteWithNewStatus(site, Status.INDEXED);
        System.out.println("Time Spent - " + (System.currentTimeMillis() - start));
//add sites to database
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
    @SneakyThrows
    private void mainLogicForTheSite2(Site site) {
        long start = System.currentTimeMillis();
            ForkJoinPool pool = new ForkJoinPool();
            ReccWithDBCheck task = new ReccWithDBCheck(new Page(site, site.getUrl(), 0, null),this.pageService,this.lemmaService,this.indexService);
        try {
            pool.invoke(task);
        }catch (Exception e){
            e.printStackTrace();
            stoppedExceptionally(site,task);
            return;
        }
        finally {
            startResponseNegative(site.getLastError());
        }
        pool.shutdown();
        System.err.println(site.getUrl() + " - " + pageService.getAllPages().size() + " - SIZE");//fixme not the right SIZE
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

    private boolean stoppedManually(Site site, ForkJoinTask task) {
        if (Thread.currentThread().isInterrupted() && !startResponse.isResult()) {
            System.err.println("STOPPED - " + Thread.currentThread().getName());
            task.cancel(true);
            site.setLastError("Индексация остановлена пользователем");
            siteService.saveSiteWithNewStatus(site, Status.FAILED);
            return true;
        }
        return false;
    }
    private void stoppedExceptionally(Site site, ForkJoinTask task) {
            System.err.println("STOPPED EXCEPTIONALLY - " + Thread.currentThread().getName());
            task.cancel(true);
            site.setLastError("Индексация остановлена по причине ошибки");
            siteService.saveSiteWithNewStatus(site, Status.FAILED);
    }

//    private void savePagesHQLWorksOnlyWithEmbeddedId(Set<Page> thisSitePages) {
//        Transaction transaction = null;
//        int batchSize = 2;
//        int i = 0;
//        try {
//            Session session = HibernateUtil.getSessionFactory().openSession();
//            session.setJdbcBatchSize(3);
//            transaction = session.beginTransaction();
//            for (Page page : thisSitePages) {
//                session.persist(page);
//                if (i > 0 && i % batchSize == 0) {
//                    session.flush();
//                    session.clear();
//                }
//                i++;
//            }
//            transaction.commit();
//            session.close();
//        } catch (Exception e) {
//            System.err.println(e.getMessage());
////                if (transaction != null && transaction.isActive()) {
////                    transaction.rollback();
////                }
//        }
//    }

    private boolean isaLastSite(Site site) {
        return siteService.getAllSites().get(siteService.getAllSites().size() - 1) == site;
    }
}
package TestsToDelete.runAndFork;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
//@Component
public class Runn extends Thread {//fixme may be RunnableFuture<T>
    private Site site;
    @Autowired
    private SiteService siteService;
    @Autowired
    private PageService pageService;

    @SneakyThrows
    public Runn(Site site) {
        this.site = site;
    }
    public Runn(){}

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        ForkJoinPool pool = new ForkJoinPool();
        Recc task = new Recc(new Page(site, site.getUrl(), 0, null), null);//fixme set code and content inside
        //getting all pages
        Set<Page> thisSitePages = pool.invoke(task);
        pool.shutdown();
        //getting all pages
        if (Thread.currentThread().isInterrupted()) {
            System.err.println("STOPPED");
            return;
        }
        //save pages
//            savePagesHQLWorksOnlyWithEmbeddedId(thisSitePages);
        pageService.saveAllPages(thisSitePages);//fixme may be done in another thread with thing below
        //save pages
        thisSitePages.forEach(page -> {//fixme may be done in another thread with thing above or by ExecutorService or synchronized - wat, notify
//fixme save status time for site
//                Thread thread2 = new Thread(() -> {
            //save lemmas
//                    lemmaService.saveLemmas(page);
            //save lemmas
            //save indexes
//                    indexService.saveIndex(page, lemmaService.getLemmasBySiteUrl(site.getUrl()));
            //save indexes
//                });
//                thread2.start();
//                try {
//                    thread2.join();
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
        });
        System.err.println(site.getUrl() + " - " + thisSitePages.size() + " - SIZE");//fixme not the right SIZE
        System.out.println("FINE");
        //save indexed or failed site
        siteService.saveSiteIndexedOrFailed(site.getUrl());
        System.out.println("Time Spent - " + (System.currentTimeMillis() - start));
        //save indexed or failed site
//add sites to database
        try {
            //fixme press stopIndexing here
            Jsoup.connect("http://localhost:8080/api/stopIndexing").ignoreContentType(true)
                    .method(Connection.Method.GET).data("result", "true").execute();
            //fixme try to pass json object through url->HTTP method
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public Site getSite() {
        return site;
    }
}

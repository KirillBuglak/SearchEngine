package TestsToDelete.runAndFork;

import lombok.SneakyThrows;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class Callable implements java.util.concurrent.Callable<Set<Page>> {//fixme may be RunnableFuture<T>
    private ExecutorService service;
    private Thread thread;
    private Site site;
    private Set<Page> pages;

    @SneakyThrows
    public Callable(Site site) {
        pages = new HashSet<>();
        this.site = site;
//        thread = new Thread(this);
//        thread.start();
//        thread.join();
            service = Executors.newCachedThreadPool();

    }

    @Override
    public Set<Page> call() {
        ForkJoinPool pool = new ForkJoinPool();
//        Recc task = new Recc(new Page(site,site.getUrl(),999,"site content"));
//        pool.invoke(task);
//        pool.shutdown();
//        pages.addAll(task.getPages());
//        System.err.println(site.getUrl() + " - " + pages.size() + " - SIZE");//fixme not the right SIZE
        return pages;
    }

    public Set<Page> getPages() {
        return pages;
    }

    public ExecutorService getService() {
        return service;
    }

    public Thread getThread() {
        return thread;
    }

    public Site getSite() {
        return site;
    }
}

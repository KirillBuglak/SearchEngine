package TestsToDelete.runnablePlusFJP;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Set;
import java.util.concurrent.*;

public class SiteRunnableWithForkJoin implements Runnable {//fixme may be RunnableFuture<T>
    private ExecutorService service;
    private Thread thread;
    private Site site;
    volatile static Set<Page> pages = ConcurrentHashMap.newKeySet();

    public SiteRunnableWithForkJoin(Site site) {
        this.site = site;
        thread = new Thread(this);
        thread.start();
//        service = Executors.newCachedThreadPool();
//        service.submit(this);
    }

    @Override
    public void run() {
        ForkJoinPool pool = new ForkJoinPool();
        RecTaskTest task = new RecTaskTest(site);
        pool.execute(task);
//        pages.addAll(pool.invoke(task));
        pool.shutdown();
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
}

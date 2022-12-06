package TestsToDelete.WebCrawler;

import java.util.concurrent.ForkJoinPool;

public class MainForRecAction {
//    static String site = "https://playback.ru";
    static String site = "https://skillbox.ru";
//    static String site = "https://astral.ru/";
//    static String site = "https://kontur.ru/";
//    static String site = "https://dombulgakova.ru";
//    static String site = "https://lenta.ru";

    public static void main(String[] args) {
        RecActionGetListOfPages.setSiteURL(site);
        RecActionGetListOfPages action = new RecActionGetListOfPages(site);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(action);
        pool.shutdown();
        System.err.println(action.getPageDataBase().size());
    }
}

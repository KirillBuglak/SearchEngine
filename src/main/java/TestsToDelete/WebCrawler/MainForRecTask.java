package TestsToDelete.WebCrawler;

import lombok.SneakyThrows;

import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public class MainForRecTask {
//    static String site = "https://playback.ru";
//    static String site = "https://skillbox.ru";
//    static String site = "https://astral.ru/";
//    static String site = "https://kontur.ru/";
//    static String site = "https://dombulgakova.ru";
    static String site = "https://lenta.ru";

    public static void main(String[] args) {
        RecTaskGetListOfPages createNode = new RecTaskGetListOfPages(site);
        RecTaskGetListOfPages.setSiteURL(site);
        ForkJoinPool pool = new ForkJoinPool();
        Set<String> invoke = pool.invoke(createNode);
        System.err.println(invoke.size());
        pool.shutdown();
    }

    @SneakyThrows
    public static void main2(String[] args) {
        RecTaskGetListOfPages createNode = new RecTaskGetListOfPages(site);
        ForkJoinPool pool = new ForkJoinPool();
        Set<String> invoke = pool.invoke(createNode);
        System.err.println(invoke.size());
        pool.shutdown();
//        System.err.println("Site - " + invoke.getURL());
//        System.err.println(createNode.getPageDataBase().size());
//        System.err.println(createNode.getPageDataBase().size());
//        System.out.println("Site children number - " + invoke.getChildren().size());
//        System.out.println("Childrens' children number sum- " + invoke.getChildren().stream()
//                .map(child -> child.getChildren().size()).reduce(Integer::sum).get());
//        invoke.getChildren().stream().filter(child -> child.getChildren().size() > 1).findFirst().get().getChildren()
//                .forEach(child -> System.err.println("Child's links" + " - " + child.getURL()));
    }
}

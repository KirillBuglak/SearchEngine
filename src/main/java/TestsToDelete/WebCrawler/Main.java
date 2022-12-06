package TestsToDelete.WebCrawler;

public class Main {
    public static void main(String[] args) {
//        String site = "https://dombulgakova.ru";
        String site = "https://lenta.ru";
//        String site = "https://playback.ru";
        Page page = new PageImplCreatedItself(site);
        Integer integer = page.getChildren().stream().map(child -> child.getChildren().size()).reduce(Integer::sum).get();
        System.out.println(integer);
    }
}

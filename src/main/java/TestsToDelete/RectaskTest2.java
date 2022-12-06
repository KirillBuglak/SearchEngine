package TestsToDelete;

import lombok.SneakyThrows;
import org.apache.catalina.Executor;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class RectaskTest2 {
    @SneakyThrows
    public static void main(String[] args) {
//        String link = "https://kontur.ru/";
//        String link = "https://astral.ru/";
        String link = "https://skillbox.ru/";
        Node node = new Node();
        node.setLink(link);
        MyRecTask task = new MyRecTask(node);
        ForkJoinPool pool = new ForkJoinPool();
        String invoke = pool.invoke(task);
        pool.shutdown();
        System.out.println(invoke);
    }

    static class MyRecTask extends RecursiveTask<String> {
        ExecutorService executorService = Executors.newWorkStealingPool();
        Node node;

        public MyRecTask(Node node) {
            this.node = node;
        }

        @Override
        protected String compute() {
            String parentLink = node.getLink();
            Set<String> linksWithNo404 = getLinksWithNo404(parentLink);
            Set<MyRecTask> tasks = new HashSet<>();
            executorService.execute(() -> {
                linksWithNo404.forEach(link -> {
                    Node child = new Node();
                    child.setLink(link);
                    node.getChildren().add(child);
                    MyRecTask task = new MyRecTask(child);
                    task.fork();
                    tasks.add(task);
                });
            });
            tasks.forEach(task -> parentLink.concat("\n" + task.join()));
            return parentLink;
        }
    }

    public static class Node {
        String link;
        List<Node> children = new ArrayList<>();

        public Node() {
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void setChildren(List<Node> children) {
            this.children = children;
        }
    }

    @SneakyThrows
    public static Set<String> getLinksWithNo404(String siteUrl) {
        String pageRegEx = "https?://([-\\w.+=&?#$%]+/?)+[^png]?";
        Thread.sleep(500);
        return Jsoup.connect(siteUrl).userAgent("Mozilla/5.0 (Windows; U; WindowsNT5.1; en-US; rv1.8.1.6) " +
                        "Gecko/20070725 Firefox/2.0.0.6").referrer("http://www.google.com")
                .ignoreContentType(true).get().getElementsByAttribute("href").stream()
                .map(element -> element.attr("href")).filter(element -> element.matches(pageRegEx))
                .filter(element -> getStatusCode(element) != 404).collect(Collectors.toSet());
    }

    private static int getStatusCode(String link) {
        try {
            return Jsoup.connect(link).ignoreContentType(true).ignoreHttpErrors(true)
//                    .followRedirects(false)
                    .execute().statusCode();
        } catch (IOException e) {
            return 404;
        }
    }
}

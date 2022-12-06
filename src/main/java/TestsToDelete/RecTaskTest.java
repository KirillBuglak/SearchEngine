package TestsToDelete;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class RecTaskTest {
    @SneakyThrows
    public static void main(String[] args) {
        String siteUrl = "https://skillbox.ru/";
        ExecutorService executorService = Executors.newCachedThreadPool();
//        SiteNode siteNode = executorService.submit(() -> new SiteNode(siteUrl)).get();
        Node siteNode = new SiteNode();
        siteNode.setSiteUrl(siteUrl);
//        NodeCreation creation = new NodeCreation(siteNode);
        GettingLinks gettingLinks = new GettingLinks(siteNode);
//        System.err.println(siteNode.getContext());
//        siteNode.getChildren().forEach(child -> System.out.println(child.getURL()));
//        System.exit(7);
//        HashMap<String, Integer> linksWithStatuses = new HashMap<>();
//        mainPageLinks.forEach(link -> linksWithStatuses.put(link, getStatusCode(link)));
//        linksWithStatuses.forEach((k, v) -> System.out.println(k + " - " + v));
//        System.exit(7);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        String invoke = forkJoinPool.invoke(gettingLinks);
        System.out.println(invoke);
        forkJoinPool.shutdown();
//        StringBuilder textFromAllPages = forkJoinPool.invoke(new MyRecTask(siteUrl));
//        System.out.println(textFromAllPages);
//        forkJoinPool.shutdown();
    }


    private static int getStatusCode(String link) {
        try {
            return Jsoup.connect(link).ignoreContentType(true).ignoreHttpErrors(true)
                    .followRedirects(false).execute().statusCode();
        } catch (IOException e) {
            return 404;
        }
    }

    /**********************************************************************************************/
    static class NodeCreation extends RecursiveTask<Node> {
        private final Node siteNode;

        public NodeCreation(Node siteNode) {
            this.siteNode = siteNode;
        }

        protected Node compute() {
            List<NodeCreation> taskList = new ArrayList<>();
            List<String> linksWithNo404 = siteNode.getLinksWithNo404(siteNode.getURL());
            List<Node> children = siteNode.getChildren();
            linksWithNo404.forEach(link -> {
                Node child = new SiteNode();
                child.setSiteUrl(link);
                NodeCreation task = new NodeCreation(child);
                task.fork();
                taskList.add(task);
            });
            taskList.forEach(task -> children.add(task.join()));
            return siteNode;
        }
    }

    /**********************************************************************************************/
    static class GettingLinks extends RecursiveTask<String> {
        private final Node siteNode;

        public GettingLinks(Node siteNode) {
            this.siteNode = siteNode;
        }

        @Override
        protected String compute() {
            String siteUrl = siteNode.getURL();
            List<String> siteLinks = siteNode.getLinksWithNo404(siteUrl);
            List<GettingLinks> taskList = new ArrayList<>();
            siteLinks.forEach(link -> {
                Node child = new SiteNode();
                child.setSiteUrl(link);
                GettingLinks task = new GettingLinks(child);
                task.fork();
                taskList.add(task);
            });
            taskList.forEach(task -> siteUrl.concat("\t" + task.join()));
            return siteUrl;
        }
    }

    /**********************************************************************************************/
    static class SiteNode implements Node {
        private String siteUrl;
        private List<Node> children = new ArrayList<>();

        public SiteNode(String siteUrl) {//todo adds all links
            this.siteUrl = siteUrl;
            List<String> siteLinks = getLinksWithNo404(siteUrl);
            siteLinks.forEach(siteLink -> children.add(new SiteNode(siteLink)));
        }

        /*******************************///fixme two constructors bellow - just to test first page children
//        public SiteNode(String siteUrl) {
//            this.siteUrl = siteUrl;
//            children = new ArrayList<>();
//            List<String> siteLinks = getLinksWithNo404(siteUrl);
//            siteLinks.forEach(siteLink -> {
//                SiteNode child = new SiteNode();
//                child.setSiteUrl(siteLink);
//                children.add(child);
//            });
//        }
        public SiteNode() {
        }

        /*******************************/
        @Override
        public List<Node> getChildren() {
            return children;
        }

        @Override
        public String getURL() {
            return siteUrl;
        }

        @Override
        public void setSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
        }

        @Override
        public void setChildren(List<Node> children) {
            this.children = children;
        }

        @SneakyThrows
        @Override
        public String getContext() {
            return String.valueOf(Jsoup.connect(siteUrl).get());//fixme maybe change to get().text() or smth
        }

        @SneakyThrows
        public List<String> getLinksWithNo404(String siteUrl) {
            String pageRegEx = "https?://([-\\w.+=&?#$%]+/?)+";
            return Jsoup.connect(siteUrl).get().getElementsByAttribute("href").stream()
                    .map(element -> element.attr("href")).filter(element -> element.matches(pageRegEx))
                    .filter(element -> getStatusCode(element) != 404).toList();
        }
    }

    /**********************************************************************************************/
    interface Node {
        List<Node> getChildren();

        String getURL();

        void setSiteUrl(String siteUrl);

        void setChildren(List<Node> children);

        String getContext();

        List<String> getLinksWithNo404(String siteUrl);
    }
}

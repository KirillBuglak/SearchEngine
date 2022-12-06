package TestsToDelete.WebCrawler;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoCrawler implements Runnable {
    Thread thread;
    ExecutorService executorService;
    private static final int MAX_DEPTH = 3;//fixme has to work on it
    private String siteURL;
    private final int start;
    private String pageRegEx;
    //    private String pageRegEx = "https?://([-\\w.+=&?#$%]+/?)+";
    private Set<String> visitedLinks = new HashSet<>();

    private int id;

    public VideoCrawler(String siteURL, int id) {
        this.siteURL = siteURL;
        start = siteURL.indexOf("//") + 2;
        pageRegEx = siteURL + "([-\\w.+=&?#$%]+/?)+";
        this.id = id;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        recursiveCrawl(1, siteURL);
        System.err.println(visitedLinks.size() + " - " + siteURL);
    }

    private void recursiveCrawl(int level, String url) {
        if (level <= MAX_DEPTH) {
            Document document = request(url);
            if (document != null) {
                document.select("a[href]").forEach(element -> {//fixme
                    String link = element.absUrl("href");
                    if (link.contains(siteURL) && !visitedLinks.contains(link)) {//fixme contains param url or siteUrl
                        System.out.println(link);
                        recursiveCrawl(level + 1, link);
                    }
                });
            }
        }
    }

    @SneakyThrows
    private Document request(String url) {

        Connection connection = Jsoup.connect(url);
        Document document = null;
        try {
            document = connection.timeout(2000)//fixme crucial on program time
//                .ignoreContentType(true)
//                .ignoreHttpErrors(true).followRedirects(false)//fixme may not need this
                    .get();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
//        if (statusCode == 404) {
//            return null;
//        }
//        System.out.println("ID = " + id + " link = " + url + "\n" + (document!=null?document.title():"Error") + " - " + statusCode);
        visitedLinks.add(url);
        return document;
    }

    public Thread getThread() {
        return thread;
    }
}

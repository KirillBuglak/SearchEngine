package TestsToDelete.WebCrawler;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RunnableStyle implements Runnable {
    private static final int MAX_DEPTH = 1;//fixme has to work on it
    private String siteURL;
    private String pageRegEx;
//    private String pageRegEx = "https?://([-\\w.+=&?#$%]+/?)+";

    private Set<String> visitedLinks = new HashSet<>();
    private int id;

    public RunnableStyle(String siteURL, int id) {
        this.siteURL = siteURL;
        pageRegEx = siteURL + "([-\\w.+=&?#$%]+/?)+";
        this.id = id;
    }

    @Override
    public void run() {
        crawl(0, siteURL);
    }

    private void crawl(int level, String siteURL) {
        if (level <= MAX_DEPTH) {//fixme isn't needed - delete it
            Document document = request(siteURL);
            if (document != null) {
                Set<String> links = document.getElementsByAttribute("href").stream()
                        .map(element -> element.attr("href")).filter(element -> element.matches(pageRegEx))
                        .filter(element -> getStatusCode(element) != 404).collect(Collectors.toSet());
                links.forEach(link -> {
                    if (!visitedLinks.contains(link)) {
                        crawl(level + 1, link);
                    }
                });
            }
        }
    }

    @SneakyThrows
    private Document request(String siteURL) {
        Connection connection = Jsoup.connect(siteURL).ignoreContentType(true);
        Document document = connection.get();
        if (connection.response().statusCode() == 404) {
            return null;
        }
        System.out.println("ID = " + id + " link = " + siteURL + "\n" + document.title());
        visitedLinks.add(siteURL);
        return document;
    }

    private static int getStatusCode(String link) {
        try {
            return Jsoup.connect(link)
                    .followRedirects(false).execute().statusCode();
        } catch (IOException e) {
            return 404;
        }
    }
}

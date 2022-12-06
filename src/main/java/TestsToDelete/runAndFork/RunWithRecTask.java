package TestsToDelete.runAndFork;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class RunWithRecTask implements Runnable {

    Thread thread;
    ExecutorService executorService;
    private static final int MAX_DEPTH = 3;//fixme has to work on it
    private Page page;
    private Site site;
    private String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";
    private Set<Page> pages = new HashSet<>();


    @SneakyThrows
    public RunWithRecTask(Site site) {
        this.site = site;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        recursiveTask(1, site.getUrl());
        System.err.println(pages.size() + " - " + site.getUrl());
    }

    private void recursiveTask(int level, String pageUrl) {
//        if (level <= MAX_DEPTH) {
            Document document = requestDocAndSavePage(pageUrl);
            Elements elements = null;
            if (document != null) {
                elements = document.select("a[href]");
            }
            if (elements != null)
                elements.forEach(element -> {
                    String link = element.absUrl("href");
                    if (link.contains(pageUrl)//fixme contains param pageUrl or siteUrl
                            && link.matches(pageRegEx)
                            && pages.stream().noneMatch(page -> page.getPath().equals(link))) {
                        System.out.println(link);
                        recursiveTask(level + 1, link);
                    }
                });
//            }
        }

    @SneakyThrows
    private Document requestDocAndSavePage(String pageUrl) {
        Connection connection = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; https://www.google.com/bot.html) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com");
        Document document = null;
        StringBuilder content = new StringBuilder();
        try {
            document = connection
                    .timeout(2000)//fixme crucial on program time
                    .get();
            document.getAllElements().stream()
                    .map(element -> Arrays.toString(element.text().getBytes(StandardCharsets.UTF_8)))
                    .filter(string -> !string.isBlank()).toList().forEach(string -> content.append(string).append("\n"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        pages.add(new Page(site,pageUrl,999,content.toString()));//fixme work on content
        return document;
    }

    public Thread getThread() {
        return thread;
    }
}

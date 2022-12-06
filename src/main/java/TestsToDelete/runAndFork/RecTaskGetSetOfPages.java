package TestsToDelete.runAndFork;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Page;
import searchengine.model.Site;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class RecTaskGetSetOfPages extends RecursiveTask<Set<Page>> {
    private final String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";
    private static Site site;
    volatile static Set<Page> pageDataBase = ConcurrentHashMap.newKeySet();
    private final Page page;
    private Document document;
    private String content;

    public RecTaskGetSetOfPages(Page page) {
        this.page = page;
        pageDataBase.add(page);
    }

    @SneakyThrows
    public RecTaskGetSetOfPages() {
        Connection connect = Jsoup.connect(site.getUrl());
        page = new Page(site, site.getUrl(), connect.execute().statusCode(), connect.get().text());
    }

    @SneakyThrows
    @Override
    protected Set<Page> compute() {
        List<RecTaskGetSetOfPages> tasks = new ArrayList<>();
        Thread.sleep(500);
        Connection connection = Jsoup.connect(page.getPath())
                .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; https://www.google.com/bot.html) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com");
        try {
            document = connection
                    .timeout(2000)//fixme crucial on program time
                    .get();
            content = document.getAllElements().stream()
                    .map(element -> Arrays.toString(element.text().getBytes(StandardCharsets.UTF_8))).filter(string -> !string.isBlank()).reduce((s1, s2) -> s1 + "\n" + s2).get();
            System.err.println(page.getPath());
        } catch (Exception e) {
            document = null;
            System.err.println("Check one - " + e.getMessage());//fixme delete this when there's no need
        }
            if (document != null) {
                document.select("a[href]").forEach(element -> {
                    String link = element.absUrl("href");
                        if (link.contains(page.getPath())//fixme may need to change to pageURL/siteURL
                                && link.matches(pageRegEx)
/**fixme concurrent error*/ && pageDataBase.stream().noneMatch(page -> page.getPath().equals(link))) {//fixme may need to get rid of / at the end of a page here
                            Page newPage = new Page(site, link, connection.response().statusCode(), content);
                            pageDataBase.add(newPage);//fixme has to check in db not in visitedLinks
                            RecTaskGetSetOfPages task = new RecTaskGetSetOfPages(newPage);
                            task.fork();
                            tasks.add(task);
                        }
                });
            }

        tasks.forEach(ForkJoinTask::join);
        return pageDataBase;
    }

    public static void setSite(Site site) {
        RecTaskGetSetOfPages.site = site;
    }
}

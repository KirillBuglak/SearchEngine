package TestsToDelete.WebCrawler;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class RecTaskGetListOfPages extends RecursiveTask<Set<String>> {
    private final String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";
    private static String siteURL;
    static Set<String> pageDataBase = new HashSet<>();
    String pageURL;

    public RecTaskGetListOfPages(String pageURL) {
        this.pageURL = pageURL;
        pageDataBase.add(pageURL);
    }

    @SneakyThrows
    @Override
    protected Set<String> compute() {
        List<RecTaskGetListOfPages> tasks = new ArrayList<>();
        Thread.sleep(500);
        Connection connection = Jsoup.connect(pageURL)
                .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; https://www.google.com/bot.html) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com");
        Document document;
        try {
            document = connection
                    .timeout(5000)//fixme crucial on program time
                    .get();
        } catch (Exception e) {
            document = null;
            System.err.println(e.getMessage());//fixme delete this when there's no need
        }
        if (document != null) {
            document.select("a[href]").forEach(element -> {
                String link = element.absUrl("href");
                if (link.contains(pageURL)//fixme may need to change to pageURL/siteURL
                        && link.matches(pageRegEx)
                        && !pageDataBase.contains(link)) {//fixme may need to get rid of / at the end of a page here
                    pageDataBase.add(link);//fixme has to check in db not in visitedLinks
//                    try {
//                        pageService.savePage(siteService.getSiteByURL(siteURL), pageURL
//                                , connection.response().statusCode(), connection.get().text());
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
                    RecTaskGetListOfPages task = new RecTaskGetListOfPages(link);
                    task.fork();
                    tasks.add(task);
                    System.out.println(link);
                }
            });
        }
        tasks.forEach(ForkJoinTask::join);
        return pageDataBase;
    }
    public static void setSiteURL(String siteURL) {
        RecTaskGetListOfPages.siteURL = siteURL;
    }
}

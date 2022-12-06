package TestsToDelete.WebCrawler;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class RecActionGetListOfPages extends RecursiveAction {
    private static String siteURL;

    public static void setSiteURL(String siteUrl) {
        siteURL = siteUrl;
    }
    private final String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";
    static Set<String> pageDataBase = new HashSet<>();
    String url;
    public RecActionGetListOfPages(String url) {
        this.url = url;
    }

    @SneakyThrows
    @Override
    protected void compute() {
        List<RecActionGetListOfPages> tasks = new ArrayList<>();
        Thread.sleep(500);
        Connection connection = Jsoup.connect(url)
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
                if (link.contains(url)//fixme may need to change to pageURL/siteURL
                        && link.matches(pageRegEx)
                        && !pageDataBase.contains(link)) {//fixme may need to get rid of / at the end of a page here
                    pageDataBase.add(link);//fixme has to check in db not in visitedLinks
                    RecActionGetListOfPages task = new RecActionGetListOfPages(link);
                    System.out.println(link + " - " + siteURL);
                    task.fork();
                    tasks.add(task);
                }
            });
        }
        tasks.forEach(ForkJoinTask::join);
    }
    public Set<String> getPageDataBase() {
        return pageDataBase;
    }
}

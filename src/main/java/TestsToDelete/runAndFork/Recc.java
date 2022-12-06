package TestsToDelete.runAndFork;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

public class Recc extends RecursiveTask<Set<Page>> {

    private final Recc parentTask;
    private final Page page;
    private final String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";
    private final Set<Page> pages;

    public Recc(Page page, Recc parentTask) {
        pages = ConcurrentHashMap.newKeySet();
        this.page = page;
        this.parentTask = parentTask;
    }

    @Override
    public Set<Page> compute() {
        List<Recc> tasks = new ArrayList<>();
        Document document = requestDoc(page.getPath());
        Elements elements = null;
        if (document != null) {
            elements = document.select("a[href]");
        }
        if (elements != null) {
            elements.forEach(element -> {
                String link = element.absUrl("href");
                if (link.contains(page.getPath())//fixme contains param pageUrl or siteUrl
                        && link.matches(pageRegEx)
                        && (pages.stream().noneMatch(page -> page.getPath().equals(link)))
                ) {
                    System.out.println(link);
                    Page newPage = new Page(page.getSite(), link, 0, null);
                    pages.add(newPage);//fixme may need save it below
                    Recc task = new Recc(newPage, this);
                    task.fork();
                    tasks.add(task);
                }
            });
        }
        tasks.forEach(task -> pages.addAll(task.join()));
        return pages;
    }

    @SneakyThrows
    private Document requestDoc(String pageUrl) {//fixme may need to set code and content here for this.page
        //todo may need to sleep here for a while
        Connection connection = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; https://www.google.com/bot.html) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com")
                .timeout(20000);
        Document document;
        StringBuilder content = new StringBuilder();
        int statusCode;
        try {
            statusCode = connection
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute().statusCode();
            document = connection
                    .get();
            document.getAllElements().stream()
                    .map(element -> element.text())//fixme may need to change coding
                    .filter(string -> !string.isBlank()).toList().forEach(string -> content.append(string).append("\n"));
        } catch (Exception e) {
            document = null;
            statusCode = 404;
            content.append("Error - no content");
            System.err.println(e.getMessage());
        }
        page.setCode(statusCode);
        page.setContent(content.toString());
        pages.add(page);
        return document;
    }

    public Set<Page> getPages() {
        return pages;
    }
}
package TestsToDelete.WebCrawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class PageImplCreatedItself implements Page {
    private static int Level;
    private final String siteURL;
    private final List<Page> children;

        public PageImplCreatedItself(String siteURL) {
        children = new ArrayList<>();
        this.siteURL = siteURL;
//        Level++;
//        if (Level <= 100000) {
            Connection connection = Jsoup.connect(siteURL);
            Document document = null;
            int statusCode;
            try {
                document = connection.timeout(2000)//fixme crucial on program time
                        .ignoreHttpErrors(true)
                        .get();
                statusCode = connection.response().statusCode();
            } catch (Exception e) {
                statusCode = 404;
                System.err.println(e.getMessage());
            }
            if (document != null) {
                document.select("a[href]").forEach(element -> {
                    String link = element.absUrl("href");
                    if (link.contains(siteURL)) {
//                        System.out.println(link + " site - " + siteURL);
                        children.add(new PageImpl(link));
                    }
                });
            }
//        }
    }

        @Override
        public String getURL () {
            return siteURL;
        }

        @Override
        public List<Page> getChildren () {
            return children;
        }
    }

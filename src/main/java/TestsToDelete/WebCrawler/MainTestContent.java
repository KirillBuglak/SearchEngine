package TestsToDelete.WebCrawler;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainTestContent {
    public static void main(String[] args) {
        String pageRegEx = "https?://([-\\w.+=&?#$%]+/?)+";
        List<String> sitesUrls = new ArrayList<>(List.of("https://www.lenta.ru", "https://www.playback.ru", "https://skillbox.ru/"));
        sitesUrls.forEach(siteUrl -> {
            try {
                Elements siteElements = Jsoup.connect(siteUrl).timeout(500)
                        .ignoreHttpErrors(true).ignoreContentType(true)
                        .get().getElementsByAttribute("href");
                Set<String> siteLinks = siteElements.stream().map(element -> element.absUrl("href"))
                        .filter(link -> link.matches(pageRegEx))
                        .filter(link -> getStatusCode(link)!=404)
                        .collect(Collectors.toSet());
                siteLinks.forEach(System.out::println);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static int getStatusCode(String link) {
        try {
            return Jsoup.connect(link).timeout(100)
                    .followRedirects(false).execute().statusCode();
        } catch (Exception e) {
            return 404;
        }
    }
}

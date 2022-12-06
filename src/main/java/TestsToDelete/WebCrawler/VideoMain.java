package TestsToDelete.WebCrawler;

import java.util.ArrayList;
import java.util.List;

public class VideoMain {
    public static void main(String[] args) {
        List<VideoCrawler> videoCrawlers = new ArrayList<>();
        List<String> links = new ArrayList<>(List.of("https://skillbox.ru", "https://dombulgakova.ru", "https://playback.ru"));
        links.forEach(link -> videoCrawlers.add(new VideoCrawler(link, links.indexOf(link))));
        videoCrawlers.forEach(crawler -> {
            try {
                crawler.getThread().join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

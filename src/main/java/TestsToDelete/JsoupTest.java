package TestsToDelete;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;

public class JsoupTest {
    @SneakyThrows
    public static void main(String[] args) throws IOException {
//        String page = "https://lenta.ru/rubrics/world242/";
//        String page2 = "https://lenta.ru/";
//        String page3 = "http://www.site.com";
//        String page4 = "http://localhost:8080";
//        System.out.println(Jsoup.connect(page3).ignoreHttpErrors(true).followRedirects(false).execute().statusCode());
//        System.out.println(Jsoup.connect(page2).get().title());
//        System.out.println(Jsoup.connect(page3).execute().statusCode());
//        URL url = new URL("http://localhost:8080/api/startIndexing");
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        connection.setRequestMethod("GET");
//        connection.setRequestProperty("result", String.valueOf(true));
//        System.out.println(connection.getResponseCode());
//        connection.disconnect();
        Thread thread = new Thread(()-> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("hey");});
        thread.start();
//        thread.join();
        System.out.println("Here");
    }
}

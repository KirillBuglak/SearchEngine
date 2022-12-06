package TestsToDelete.WebCrawler;

import java.util.List;

public interface Page {
    String getURL();
    List<Page> getChildren();
}

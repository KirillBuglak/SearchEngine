package TestsToDelete.WebCrawler;

import java.util.ArrayList;
import java.util.List;

public class PageImpl implements Page {
    private final String siteURL;
    private final List<Page> children;

    public PageImpl(String siteURL) {
        this.siteURL = siteURL;
        children = new ArrayList<>();
    }

    @Override
    public String getURL() {
        return siteURL;
    }

    @Override
    public List<Page> getChildren() {
        return children;
    }
}

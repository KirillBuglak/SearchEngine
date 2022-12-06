package TestsToDelete.runAndFork;

import searchengine.model.Page;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalDBTest {
    private static Set<Page> pages = ConcurrentHashMap.newKeySet();

    public static Set<Page> getPages() {
        return pages;
    }
}

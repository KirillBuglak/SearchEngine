package searchengine.config;

import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.HashMap;
import java.util.List;

public class PagesAndLemmas {
    private static final HashMap<Page, List<Lemma>> pagesAndLemmas;
    static {
        pagesAndLemmas = new HashMap<>();
    }

    public static void deletePageByPagePath(String pagePath){
        pagesAndLemmas.remove(pagesAndLemmas.keySet().stream()
                .filter(page -> page.getPath().equals(pagePath)).findFirst().get());
    }
}

package searchengine.services;

import java.util.ArrayList;
import java.util.List;

public class TestTextsForSites {
    public static final List<String> text = new ArrayList<>();

    public static List<String> getList() {
        text.add("бирюзы Бирюзы ");
        text.add("Круглой Двадцати двадцати");
        text.add("мне побегу ");
        text.add("сверху раздетым ");
        text.add("работающим присев к ");
        text.add("бирюзы или не пожалуй эй");
        return text;
    }
}

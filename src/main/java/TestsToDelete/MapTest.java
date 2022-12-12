package TestsToDelete;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapTest {
    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>();

        map.put("a", 12);
        map.put("b", 24);
        map.put("c", 15);
        LinkedHashMap<String, Integer> collect = map.entrySet().stream().sorted((o1, o2) -> o2.getValue() - o1.getValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        System.out.println(collect);
    }
}
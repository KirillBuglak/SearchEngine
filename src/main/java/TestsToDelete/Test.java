package TestsToDelete;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Predicate;

public class Test {
    private static final String text = " e5e5 бирюзы Бирюзы Круглой Двадцати двадцати мне " +
            "побегу сверху раздетым работающим присев к или не пожалуй эй";
    private static final String regex = "[^Ё-ё\\d]";

    public static void main(String[] args) throws IOException {

        LuceneMorphology morphology = new RussianLuceneMorphology();
        Predicate<String> filterOutAnds = mInfo -> (mInfo.contains("|l") || mInfo.contains("|n")
                || mInfo.contains("|p") || mInfo.contains("|q") || mInfo.contains("|o"));

        List<String> lemmas = new java.util.ArrayList<>(Arrays.stream(text.trim().toLowerCase().split(regex))
                .<String>mapMulti((word, consumer) -> morphology.getMorphInfo(word).stream().filter(filterOutAnds)
                        .forEach(e -> consumer.accept(word))).distinct().map(morphology::getNormalForms)//fixme may need put .distinct() before .map()
                .flatMap(List::stream).toList());
        lemmas.forEach(System.out::println);



    }
}

package TestsToDelete;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

public class Test2 {
        private static final String text = " e5e бирюзы Бирюзой Круглой Двадцати двадцати мне " +
                "побегу сверху раздетым работающим присев к или не пожалуй эй";
    private static final String regex = "[^Ё-ё\\d]";

    public static void main(String[] args) throws IOException {
        LuceneMorphology morphology = new RussianLuceneMorphology();
        Predicate<String> filterOutAnds = mInfo -> !(mInfo.contains("|l") || mInfo.contains("|n")
                || mInfo.contains("|p") || mInfo.contains("|q") || mInfo.contains("|o"));


        List<List<String>> lists = Arrays.stream(text.toLowerCase().split(regex))
                .map(word -> morphology.getNormalForms(word))
//                .flatMap(List::stream)
                .toList();
        Iterator<List<String>> iterator = lists.iterator();
        List<String> strings = lists.stream().filter(list -> list.size() == 1 || !(iterator.next()
                        .equals(iterator.next()))).flatMap(List::stream).toList();
        List<String> strings1 = strings.stream().<String>mapMulti((word, consumer) -> morphology.getMorphInfo(word).stream()
                .filter(filterOutAnds).forEach(m ->consumer.accept(word))).toList();
        List<String> strings2 = strings.stream().filter(str -> !(morphology.getMorphInfo(str).get(0).contains("|l")
        || morphology.getMorphInfo(str).get(0).contains("|n") || morphology.getMorphInfo(str).get(0).contains("|p")
        || morphology.getMorphInfo(str).get(0).contains("|q") || morphology.getMorphInfo(str).get(0).contains("|o")
//        || morphology.getMorphInfo(str).get(1).contains("|l")//fixme have to check for get(1)
//        || morphology.getMorphInfo(str).get(1).contains("|n") || morphology.getMorphInfo(str).get(1).contains("|p")
//        || morphology.getMorphInfo(str).get(1).contains("|q") || morphology.getMorphInfo(str).get(1).contains("|o")
                )).toList();
        strings2.forEach(System.out::println);
    }
}

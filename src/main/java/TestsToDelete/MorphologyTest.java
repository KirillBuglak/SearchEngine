package TestsToDelete;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;

public class MorphologyTest {
    private static final String text = " ть ул домъий л б в ы бег быстрой двадцатому кинул медленно";
    private static final String regex = "[^Ё-ё\\d]";

    public static void main(String[] args) throws IOException {
        LuceneMorphology morphology = new RussianLuceneMorphology();
        Arrays.stream(text.trim().toLowerCase().split(regex)).filter(morphology::checkString).forEach(word-> System.out.println(morphology.getNormalForms(word)));
    }
}

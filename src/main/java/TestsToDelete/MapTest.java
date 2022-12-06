package TestsToDelete;

import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class MapTest {
    public static void main(String[] args) {
        List<Lemma> lemmas = new ArrayList<>();
        Page page = new Page();
        page.setId(1);
        page.setPath("path");
        page.setSite(new Site("url","name"));
        page.setCode(200);
        page.setContent("content");
        Lemma newLemma = new Lemma(page.getSite(), "lemma", 1);
        lemmas.add(newLemma);
        Lemma lemma = lemmas.stream().filter(Lemma -> Lemma.getLemma().equals("lemma")).findFirst().get();
lemma.setFrequency(3);
lemmas.forEach(lemma1-> System.out.println(lemma1.getFrequency()));
    }
}

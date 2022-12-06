package searchengine.services;

import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.HashMap;
import java.util.List;

public interface LemmaService {
    void saveLemmas(Page page);

    boolean isLemmaPresent(String word);

    void deleteAllLemmas();

    void deleteLemmasByPagePath(String pagePath);

    List<Lemma> getAllLemmas();

    List<Lemma> getLemmasBySiteUrl(String siteUrl);

    List<Lemma> getLemmasByPagePath(String pagePath);

    List<String> getFilteredLemmas(String text);

    List<Lemma> getLemmasByWord(String word);

    List<Lemma> getLemmasByListOfWords(List<String> words);

    HashMap<Page, List<Lemma>> getPagesAndLemmas();
}

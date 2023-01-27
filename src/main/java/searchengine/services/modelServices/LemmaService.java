package searchengine.services.modelServices;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.LemmaRepository;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class LemmaService {
    private final LemmaRepository lemmaRepository;
    private static final HashMap<Page, Set<Lemma>> pagesAndLemmas;
    private final String regex = "[^Ё-ё\\d]";
    private static final LuceneMorphology morphology;
    private static final Predicate<String> neededWords;
    private static final Predicate<String> realWords;

    static {
        pagesAndLemmas = new HashMap<>();
        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        neededWords = word -> word.length() > 1 && (morphology.getMorphInfo(word).stream()
                .anyMatch(mInfo ->
                        mInfo.contains("|A")
                                || mInfo.contains("|Y")
                                || mInfo.contains("|i")
                                || mInfo.contains("|a")
                                || mInfo.contains("|j")
                ));
        realWords = word -> {
            List<String> normalForms;
            try {
                normalForms = morphology.getNormalForms(word);
            } catch (Exception e) {
                normalForms = null;
            }
            return normalForms != null;
        };
    }

    @Autowired
    public LemmaService(LemmaRepository lemmaRepository) {
        this.lemmaRepository = lemmaRepository;
    }

    public Set<Lemma> getLemmasByString(String lemma) {
        return lemmaRepository.findByLemma(lemma);
    }

    public void saveLemmas(Page page) {
        Set<Lemma> lemmasPerPage = new HashSet<>();
        List<String> lemmas = getFilteredLemmas(page.getContent());
        lemmas.forEach(lemma -> {
            Optional<Lemma> perPageOptional = lemmasPerPage.stream().filter(Lemma -> Lemma.getLemma()
                            .replace('ё', 'е').equals(lemma.replace('ё', 'е')))
                    .findFirst();
            if (perPageOptional.isPresent()) {
                Lemma perPageLemma = perPageOptional.get();
                lemmasPerPage.remove(perPageLemma);
                perPageLemma.setFrequency(perPageLemma.getFrequency() + 1);
                lemmasPerPage.add(perPageLemma);
            } else {
                lemmasPerPage.add(new Lemma(page.getSite(), lemma, 1));
            }
        });
        Set<Lemma> dbLemmasWithoutID = lemmasPerPage.stream()
                .filter(lemma -> lemmaRepository.findByLemmaAndSite(lemma.getLemma(), lemma.getSite()) != null)
                .collect(Collectors.toSet());
        Set<Lemma> dbLemmasToUpdateFreq = dbLemmasWithoutID.stream()
                .map(lemma -> lemmaRepository.findByLemmaAndSite(lemma.getLemma(), lemma.getSite()))
                .collect(Collectors.toSet());
        if (!dbLemmasToUpdateFreq.isEmpty()) {
            dbLemmasToUpdateFreq.forEach(lemma -> lemma.setFrequency(lemma.getFrequency() + 1));
            lemmaRepository.saveAll(dbLemmasToUpdateFreq);
        }
        Set<Lemma> addToPagesAndLemmas = new HashSet<>(lemmasPerPage);
        dbLemmasWithoutID.forEach(lemmasPerPage::remove);
        lemmasPerPage.forEach(lemma -> lemma.setFrequency(1));
        lemmaRepository.saveAll(lemmasPerPage);
        addToPagesAndLemmas.forEach(lemma -> lemma
                .setId(lemmaRepository.findByLemmaAndSite(lemma.getLemma(), page.getSite()).getId()));
        pagesAndLemmas.put(page, addToPagesAndLemmas);
    }


    public List<String> getFilteredLemmas(String text) {
        ArrayList<String> words = new ArrayList<>(Arrays.stream(text.trim().toLowerCase().split(regex))
                .filter(realWords).filter(neededWords).toList());
        return words.stream().map(morphology::getNormalForms).flatMap(List::stream).toList();
    }

    public HashMap<String, String> getFilteredWordAndLemma(String text) {
        HashMap<String, String> wordAndLemma = new HashMap<>();
        Set<String> words = Arrays.stream(text.trim().toLowerCase().split(regex))
                .filter(realWords).filter(neededWords).collect(Collectors.toSet());
        words.forEach(word -> wordAndLemma.put(word, morphology.getNormalForms(word).get(0)));
        return wordAndLemma;
    }

    public SortedSet<Lemma> getSortedLemmasByListOfWords(List<String> words) {
        return words.stream().map(lemmaRepository::findByLemma).flatMap(Set::stream)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static HashMap<Page, Set<Lemma>> getPagesAndLemmas() {
        return pagesAndLemmas;
    }

    public void deleteAllLemmas() {
        lemmaRepository.deleteAllInBatch();
    }

    public void deleteLemmasByPage(Page page) {
        Set<String> pageLemmas = new HashSet<>(getFilteredLemmas(page.getContent()));
        pageLemmas.forEach(lemma -> {
            Lemma lemmaToChange = lemmaRepository.findByLemmaAndSite(lemma, page.getSite());
            lemmaToChange.setFrequency(lemmaToChange.getFrequency() - 1);
            if (lemmaToChange.getFrequency() == 0) {
                lemmaRepository.delete(lemmaToChange);
            } else {
                lemmaRepository.save(lemmaToChange);
            }
        });
    }

    public Set<Lemma> getLemmasBySiteUrl(String siteUrl) {
        return lemmaRepository.findBySiteUrl(siteUrl);
    }

    public String getRegex() {
        return regex;
    }
}

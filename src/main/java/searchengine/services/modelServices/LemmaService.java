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
    private volatile int numberOfPagesForLemma;
    private final IndexService indexService;
    private final LemmaRepository lemmaRepository;
    private static final HashMap<Page, Set<Lemma>> pagesAndLemmas; //fixme is there a delete here when there's delete from bd, use it whenever possible to make the program work faster
    private final String regex = "[^Ё-ё\\d]";
    private static final LuceneMorphology morphology;
    private static final Predicate<String> neededWords;//fixme work on it ть
    private static final Predicate<String> realWords;

    static {
        pagesAndLemmas = new HashMap<>();
        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        neededWords = word -> (morphology.getMorphInfo(word).stream()
                .anyMatch(mInfo ->
                {
                    String nForm = mInfo.substring(0, mInfo.indexOf("|"));
                    return (mInfo.contains("|A")
//                            && nForm.length() > 2
                    ) //fixme length is variable here
                            || mInfo.contains("|Y")
                            || mInfo.contains("|i")
                            || (mInfo.contains("|a")
//                            && nForm.length() > 2
                    ) //fixme length is variable here
                            || mInfo.contains("|j");
                }));
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
    public LemmaService(IndexService indexService, LemmaRepository lemmaRepository) {
        this.indexService = indexService;
        this.lemmaRepository = lemmaRepository;
    }

    public void saveLemmas(Page page) {//fixme - may not need to even save too common lemmas
        Set<Lemma> lemmasPerPage = new HashSet<>();
        List<String> lemmas = getFilteredLemmas(page.getContent());
        lemmas.forEach(lemma -> {
            if (SiteService.stoppedByUser(page.getSite())) {
                return;
            }
            numberOfPagesForLemma = indexService.getIndexesByLemma_lemma(lemma).size();
            Lemma newLemma = new Lemma(page.getSite(), lemma, 1);
            Lemma dbLemma = lemmaRepository.findByLemmaAndSite(lemma, page.getSite());
            Optional<Lemma> perPageOptional = lemmasPerPage.stream().filter(Lemma -> Lemma.getLemma().equals(lemma))
                    .findFirst();
            if (dbLemma != null) {
                dbLemma.setFrequency(numberOfPagesForLemma + 1);
                lemmaRepository.save(dbLemma);
                if (perPageOptional.isPresent()) {
                    Lemma perPageLemma = perPageOptional.get();
                    lemmasPerPage.remove(perPageLemma);
                    perPageLemma.setFrequency(perPageLemma.getFrequency() + 1);
                    lemmasPerPage.add(perPageLemma);
                } else {
                    newLemma.setId(lemmaRepository.findByLemmaAndSite(dbLemma.getLemma(), dbLemma.getSite()).getId());
                    lemmasPerPage.add(newLemma);
                }
            } else {
                lemmaRepository.save(newLemma);
                lemmasPerPage.add(newLemma);
            }
        });
        if (pagesAndLemmas.keySet().stream().map(Page::getPath).anyMatch(path -> path.equals(page.getPath()))) {
            pagesAndLemmas.keySet().stream().filter(key -> key.getPath().equals(page.getPath()))
                    .toList().forEach(pagesAndLemmas::remove);
        }
        pagesAndLemmas.put(page, lemmasPerPage);
    }


    public List<String> getFilteredLemmas(String text) {//fixme DONT MAKE IT RETURN SET
        ArrayList<String> words = new ArrayList<>(Arrays.stream(text.trim().toLowerCase().split(regex))
                .filter(realWords).filter(neededWords).toList());

        List<String> lemmas = words.stream().map(morphology::getNormalForms).flatMap(List::stream).toList();
        return lemmas;
    }

    public HashMap<String, String> getFilteredWordAndLemma(String text) {
        HashMap<String, String> wordAndLemma = new HashMap<>();
        ArrayList<String> words = Arrays.stream(text.trim().toLowerCase().split(regex))
                .filter(realWords).filter(neededWords).distinct().collect(Collectors.toCollection(ArrayList::new));
        words.forEach(word -> wordAndLemma.put(word, morphology.getNormalForms(word).get(0)));//fixme may need only the first word
        return wordAndLemma;
    }

    public SortedSet<Lemma> getSortedLemmasByListOfWords(List<String> words) {
        return words.stream().map(lemmaRepository::findByLemma).flatMap(Set::stream)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static HashMap<Page, Set<Lemma>> getPagesAndLemmas() {
        return pagesAndLemmas;
    }

    public boolean isLemmaPresent(String lemma) {//fixme look closer at this meth
        try {
            LuceneMorphology morphology = new RussianLuceneMorphology();
            List<String> normalForms = morphology.getNormalForms(lemma);
            for (String form : normalForms) {
                if (lemmaRepository.findByLemma(form).size() != 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public void deleteAllLemmas() {
        lemmaRepository.deleteAll();
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
}

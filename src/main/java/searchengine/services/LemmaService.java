package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.LemmaRepository;

import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class LemmaService {

    @Autowired
    private PageService pageService;//fixme may need to delete it cause of style - use only lemmaRepository
    @Autowired
    private LemmaRepository lemmaRepository;
    private static final HashMap<Page, List<Lemma>> pagesAndLemmas;
    private static boolean lemmasPerPageSaved;

    static {
        pagesAndLemmas = new HashMap<>();
        lemmasPerPageSaved = false;
    }

    @SneakyThrows
    public void saveLemmas(Page page) {
        List<Lemma> lemmasPerPage = new ArrayList<>();
        List<String> lemmas = getFilteredLemmas(page.getContent());
        lemmas.forEach(lemma -> {
            int numberOfPagesForLemma = pagesAndLemmas.entrySet().stream()
                    .filter(entry -> entry.getValue().stream().map(Lemma::getLemma).toList().contains(lemma))
                    .toList().size();
            Lemma newLemma = new Lemma(page.getSite(), lemma, 1);
            Lemma repositoryLemma = lemmaRepository.findByLemmaAndSite(lemma, page.getSite());
            Optional<Lemma> perPageOptional = lemmasPerPage.stream().filter(Lemma -> Lemma.getLemma().equals(lemma)).findFirst();
            if (repositoryLemma != null) {
                repositoryLemma.setFrequency(numberOfPagesForLemma + 1);
                lemmaRepository.save(repositoryLemma);
                if (perPageOptional.isPresent()) {
                    Lemma perPageLemma = perPageOptional.get();
                    lemmasPerPage.remove(perPageLemma);
                    perPageLemma.setFrequency(perPageLemma.getFrequency() + 1);
                    lemmasPerPage.add(perPageLemma);
                } else {
                    lemmasPerPage.add(repositoryLemma);
                }
            } else {
                lemmaRepository.save(newLemma);
                lemmasPerPage.add(lemmaRepository.findByLemmaAndSite(lemma,page.getSite()));
            }
        });
        pagesAndLemmas.put(page, lemmasPerPage);//fixme have to set id's to lemmas in this map
    }

    @SneakyThrows
    public List<String> getFilteredLemmas(String text) {
        String regex = "[^Ё-ё\\d]";
        LuceneMorphology morphology = new RussianLuceneMorphology();
        Predicate<String> neededWords = word -> (morphology.getMorphInfo(word).stream()
                .anyMatch(mInfo ->
                        (mInfo.contains("|A") && mInfo.length() > 2) //fixme length is variable here
                                || mInfo.contains("|Y")
                                || mInfo.contains("|i")
                                || (mInfo.contains("|a") && mInfo.length() > 3)
                                || mInfo.contains("|j")));
        Predicate<String> realWords = word -> {
            List<String> normalForms;
            try {
                normalForms = morphology.getNormalForms(word);
            } catch (Exception e) {
                normalForms = null;
            }
            return normalForms != null;
        };
        ArrayList<String> words = new ArrayList<>(Arrays.stream(text.trim().toLowerCase().split(regex))
                .filter(realWords).filter(neededWords).toList());//fixme has to get rid of 5fr7668 type of input

        List<String> lemmas = words.stream().map(morphology::getNormalForms).flatMap(List::stream).toList();
        return lemmas;
    }

    @SneakyThrows
    public HashMap<String, String> getFilteredWordAndLemma(String text) {
        String regex = "[^Ё-ё\\d]";
        LuceneMorphology morphology = new RussianLuceneMorphology();
        Predicate<String> neededWords = word -> (morphology.getMorphInfo(word).stream()
                .anyMatch(mInfo ->
                        (mInfo.contains("|A") && mInfo.length() > 2) //fixme length is variable here
                                || mInfo.contains("|Y")
                                || mInfo.contains("|i")
                                || (mInfo.contains("|a") && mInfo.length() > 3)
                                || mInfo.contains("|j")));
        Predicate<String> realWords = word -> {
            List<String> normalForms;
            try {
                normalForms = morphology.getNormalForms(word);
            } catch (Exception e) {
                normalForms = null;
            }
            return normalForms != null;
        };
        HashMap<String, String> wordAndLemma = new HashMap<>();
        ArrayList<String> words = new ArrayList<>(Arrays.stream(text.trim().toLowerCase().split(regex))
                .filter(realWords).filter(neededWords).toList());
        words.forEach(word -> wordAndLemma.put(word, morphology.getNormalForms(word).get(0)));
        return wordAndLemma;
    }

    public List<Lemma> getLemmasByListOfWords(List<String> words) {//fixme need distinct lemmas
        return words.stream().map(word -> lemmaRepository.findByLemma(word)).distinct().flatMap(List::stream)
                .sorted().toList();
    }

    public static HashMap<Page, List<Lemma>> getPagesAndLemmas() {
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

    public void deleteLemmasByPagePath(String pagePath) {
        Page page = pageService.getPageByPath(pagePath);
        List<String> pageLemmas = getFilteredLemmas(pageService.getPageByPath(pagePath).getContent());
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

    public List<Lemma> getAllLemmas() {
        return lemmaRepository.findAll();
    }

    public List<Lemma> getLemmasBySiteUrl(String siteUrl) {
        return lemmaRepository.findBySiteUrl(siteUrl);
    }

    public List<Lemma> getLemmasByPagePath(String pagePath) {
        int start = pagePath.indexOf("//") + 2;//fixme may need regex
        int end = pagePath.indexOf("/", start);
        return lemmaRepository.findBySiteUrl(pagePath.substring(0, end));
    }

    public static boolean isLemmasPerPageSaved() {
        return lemmasPerPageSaved;
    }

    public static void setLemmasPerPageSaved(boolean lemmasPerPageSaved) {
        LemmaService.lemmasPerPageSaved = lemmasPerPageSaved;
    }
}

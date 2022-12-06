package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.LemmaRepository;
import searchengine.model.Page;

import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private static final String WORD_TYPE_REGEX = "\\W\\w&&[^а-яА-Я\\s]";
    @Autowired
    private PageService pageService;//fixme may need to delete it cause of style - use only lemmaRepository
    @Autowired
    private LemmaRepository lemmaRepository;

    final HashMap<Page, List<Lemma>> pageAndLemmas = new HashMap<>();

    @SneakyThrows
    @Override
    public void saveLemmas(Page page) {
        List<Lemma> lemmasPerPage = new ArrayList<>();
        List<String> lemmas = getFilteredLemmas(page.getContent());
        lemmas.forEach(lemma -> {
            Lemma newLemma = new Lemma(page.getSite(), lemma, 1);
            Lemma repositoryLemma = lemmaRepository.findByLemmaAndSite(lemma, page.getSite());
            Optional<Lemma> perPageOptional = lemmasPerPage.stream().filter(Lemma -> Lemma.getLemma().equals(lemma)).findFirst();
            if (repositoryLemma != null) {
                repositoryLemma.setFrequency(repositoryLemma.getFrequency() + 1);
                lemmaRepository.save(repositoryLemma);
                if (perPageOptional.isPresent()) {
                    Lemma perPageLemma = perPageOptional.get();
                    perPageLemma.setFrequency(perPageLemma.getFrequency() + 1);
                } else {
                    lemmasPerPage.add(newLemma);
                }
            } else {
                lemmaRepository.save(newLemma);
                lemmasPerPage.add(newLemma);
            }
        });
        pageAndLemmas.put(page, lemmasPerPage);
    }

    @SneakyThrows
    @Override
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

    @Override
    public List<Lemma> getLemmasByWord(String word) {
        return lemmaRepository.findByLemma(word);
    }

    @Override
    public List<Lemma> getLemmasByListOfWords(List<String> words) {//fixme need distinct lemmas
        return words.stream().map(word -> lemmaRepository.findByLemma(word)).distinct().flatMap(List::stream).toList();
    }

    public HashMap<Page, List<Lemma>> getPagesAndLemmas() {
        return pageAndLemmas;
    }

    @Override
    public boolean isLemmaPresent(String word) {//fixme look closer at this meth
        try {
            LuceneMorphology morphology = new RussianLuceneMorphology();
            List<String> normalForms = morphology.getNormalForms(word);
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

    @Override
    public void deleteAllLemmas() {
        lemmaRepository.deleteAll();
    }

    @Override
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

    @Override
    public List<Lemma> getAllLemmas() {
        return lemmaRepository.findAll();
    }

    @Override
    public List<Lemma> getLemmasBySiteUrl(String siteUrl) {
        return lemmaRepository.findBySiteUrl(siteUrl);
    }

    @Override
    public List<Lemma> getLemmasByPagePath(String pagePath) {
        int start = pagePath.indexOf("//") + 2;//fixme may need regex
        int end = pagePath.indexOf("/", start);
        return lemmaRepository.findBySiteUrl(pagePath.substring(0, end));
    }

}

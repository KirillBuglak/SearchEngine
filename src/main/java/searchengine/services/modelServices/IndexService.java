package searchengine.services.modelServices;

import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IndexService {
    private final IndexRepository indexRepository;

    public IndexService(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public void saveIndexes(Page page) {
        Set<Lemma> lemmasPerPage = LemmaService.getPagesAndLemmas().get(page);
        lemmasPerPage.forEach(lemma -> {
            if (SiteService.stoppedByUser(page.getSite())) {
                return;
            }
                indexRepository.save(new Index(page, lemma, lemma.getFrequency()));
        });
    }
    public Index getIndexByPageAndLemma(Page page, Lemma lemma){
        return indexRepository.findByPageAndLemma(page, lemma);
    }
    public void deleteAllIndexes() {
        indexRepository.deleteAll();
    }

    public void deleteIndexesByPage(Page page) {
        indexRepository.deleteAll(getAllIndexesByPage(page));
    }

    public Set<Index> getAllIndexesByLemmas(List<Lemma> lemmas) {
        Set<Integer> lemmasIds = lemmas.stream().map(Lemma::getId)
                .collect(Collectors.toSet());
        return lemmasIds.stream().map(indexRepository::findAllByLemma_Id)
                .flatMap(List::stream).collect(Collectors.toSet());
    }
    public Set<Index> getAllIndexesByLemma(Lemma lemma) {
        return indexRepository.findAllByLemma_Lemma(lemma.getLemma());
    }

    public Set<Index> getIndexesByLemma_lemma(String lemma) {
        return indexRepository.findAllByLemma_Lemma(lemma);
    }

    public Set<Index> getAllIndexesByPage(Page page) {
        return indexRepository.findAllByPage(page);
    }

}

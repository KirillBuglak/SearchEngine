package searchengine.services.modelServices;

import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;

import java.util.List;

@Service
public class IndexService {
    private final IndexRepository indexRepository;

    public IndexService(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public void saveIndexes(Page page) {
        List<Lemma> lemmasPerPage = LemmaService.getPagesAndLemmas().get(page);
        lemmasPerPage.forEach(lemma -> {
            if (SiteService.stoppedByUser(page.getSite())) {
                return;
            }
                indexRepository.save(new Index(page, lemma, lemma.getFrequency()));
        });
    }

    public void deleteAllIndexes() {
        indexRepository.deleteAll();
    }

    public void deleteIndexesByPage(Page page) {
        List<Index> allByPage = indexRepository.findAllByPage(page);
        indexRepository.deleteAll(allByPage);
    }


    public List<Index> getAllIndexesByLemmaIds(List<Integer> lemmasIds) {
        return lemmasIds.stream().map(indexRepository::findAllByLemma_Id)
                .distinct().flatMap(List::stream).toList();
    }

    public List<Index> getIndexesByLemma_lemma(String lemma) {
        return indexRepository.findAllByLemma_Lemma(lemma);
    }

    public List<Index> getAllIndexesByPage(Page page) {
        return indexRepository.findAllByPage(page);
    }

}

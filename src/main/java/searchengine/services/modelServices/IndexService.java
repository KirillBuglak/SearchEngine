package searchengine.services.modelServices;

import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;
import searchengine.services.IndexingService;

import java.util.HashSet;
import java.util.Set;

@Service
public class IndexService {
    private final IndexRepository indexRepository;

    public IndexService(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public void saveIndexes(Page page) {
        Set<Index> indexesToDB = new HashSet<>();
        Set<Lemma> lemmasPerPage = LemmaService.getPagesAndLemmas().get(page);
        lemmasPerPage.forEach(lemma -> {
            indexesToDB.add(new Index(page, lemma, lemma.getFrequency()));
        });
        indexRepository.saveAllAndFlush(indexesToDB);
    }

    public Index getIndexByPageAndLemma(Page page, Lemma lemma) {
        return indexRepository.findByPageAndLemma(page, lemma);
    }

    public void deleteAllIndexes() {
        indexRepository.deleteAllInBatch();
    }

    public void deleteIndexesByPage(Page page) {
        indexRepository.deleteAll(getAllIndexesByPage(page));
    }

    public Set<Index> getAllIndexesByLemma(Lemma lemma) {
        return indexRepository.findAllByLemma(lemma);
    }

    public Set<Index> getAllIndexesByPage(Page page) {
        return indexRepository.findAllByPage(page);
    }

}

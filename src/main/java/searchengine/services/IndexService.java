package searchengine.services;

import lombok.SneakyThrows;
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

    public List<Index> getAllIndexes() {
        return indexRepository.findAll();
    }

    @SneakyThrows
    public void saveIndexes(Page page) {
        List<Lemma> lemmasPerPage = LemmaService.getPagesAndLemmas().get(page);
        lemmasPerPage.forEach(lemma -> {
            indexRepository.save(new Index(page, lemma, lemma.getFrequency()));//fixme have to save lemmas first before indexes
        });
    }

    public void deleteAllIndexes() {
        indexRepository.deleteAll();
    }

    public void deleteIndexes(List<Index> indexesToDelete) {
        indexRepository.deleteAll(indexesToDelete);
    }

    public void deleteIndexesByPage(Page page) {
        List<Index> allByPage = indexRepository.findAllByPage(page);
        indexRepository.deleteAll(allByPage);
    }

    public int indexesCountByLemmasIds(List<Integer> lemmasIds) {
        return lemmasIds.stream().mapToInt(lid -> indexRepository.findAllByLemma_Id(lid).size()).sum();
    }

    public List<Index> getAllIndexesByLemmaIds(List<Integer> lemmasIds) {
        return lemmasIds.stream().map(indexRepository::findAllByLemma_Id)
                .distinct().flatMap(List::stream).toList();
    }

    public List<Index> getAllIndexesByPage(Page page) {
        return indexRepository.findAllByPage(page);
    }

    public Index getIndexByPageAndLemma(Page page, Lemma lemma) {
        return indexRepository.findByPageAndLemma(page,lemma);
    }
}

package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositories.IndexRepository;

import java.util.List;

@Service
public class IndexService {
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    LemmaService lemmaService;
    @Autowired
    PageService pageService;


    public List<Index> getAllIndexes() {
        return indexRepository.findAll();
    }

    public void saveIndex(Page page) {
        List<Lemma> lemmasPerPage = lemmaService.getPagesAndLemmas().get(page);
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
        return lemmasIds.stream().map(id -> indexRepository.findAllByLemma_Id(id))
                .distinct().flatMap(List::stream).toList();
    }

    public List<Index> getAllIndexesByPage(Page page) {
        return indexRepository.findAllByPage(page);
    }

    public Index getIndexByPageAndLemma(Page page, Lemma lemma) {
        return indexRepository.findByPageAndLemma(page,lemma);
    }
}

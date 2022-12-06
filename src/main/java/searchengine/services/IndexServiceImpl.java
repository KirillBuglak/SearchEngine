package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.util.HashMap;
import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    LemmaService lemmaService;


    @Override
    public List<Index> getAllIndexes() {
        return indexRepository.findAll();
    }

    @Override
    public void saveIndex(Page page) {
        List<Lemma> lemmasPerPage = lemmaService.getPagesAndLemmas().get(page);
        lemmasPerPage.forEach(lemma -> {
            indexRepository.save(new Index(page, lemma, lemma.getFrequency()));//fixme have to save lemmas first before indexes
        });
    }

    @Override
    public void deleteAllIndexes() {
        indexRepository.deleteAll();
    }

    @Override
    public void deleteIndexes(List<Index> indexesToDelete) {
        indexRepository.deleteAll(indexesToDelete);
    }

    @Override
    public void deleteIndexesByPage(Page page) {
        List<Index> allByPage = indexRepository.findAllByPage(page);
        indexRepository.deleteAll(allByPage);
    }

    @Override
    public int indexesCountByLemmasIds(List<Integer> lemmasIds) {
        return lemmasIds.stream().mapToInt(lid -> indexRepository.findAllByLemma_Id(lid).size()).sum();
    }

    @Override
    public List<Index> getAllIndexesByLemmaIds(List<Integer> lemmasIds) {
        return lemmasIds.stream().map(id -> indexRepository.findAllByLemma_Id(id))
                .distinct().flatMap(List::stream).toList();
    }

    @Override
    public List<Index> getAllIndexesByPage(Page page) {
        return indexRepository.findAllByPage(page);
    }
}

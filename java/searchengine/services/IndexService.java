package searchengine.services;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

public interface IndexService {
    List<Index> getAllIndexes();
    void saveIndex(Page page);
    void deleteAllIndexes();
    void deleteIndexes(List<Index> indexesToDelete);
    void deleteIndexesByPage(Page page);
    int indexesCountByLemmasIds(List<Integer> lemmasIds);
    List<Index> getAllIndexesByLemmaIds(List<Integer> lemmasIds);
    List<Index> getAllIndexesByPage(Page page);
}

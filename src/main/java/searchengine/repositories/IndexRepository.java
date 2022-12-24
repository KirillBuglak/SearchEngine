package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Set;

public interface IndexRepository extends JpaRepository<Index,Integer> {
    List<Index> findAllByLemma_Id(int id);//fixme may not need this one
    Set<Index> findAllByLemma_Lemma(String lemma);
    Set<Index> findAllByPage(Page page);
    Index findByPageAndLemma(Page page, Lemma lemma);
}
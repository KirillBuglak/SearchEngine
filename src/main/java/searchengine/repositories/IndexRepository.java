package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

public interface IndexRepository extends JpaRepository<Index,Integer> {
    List<Index> findAllByLemma_Id(int id);
    List<Index> findAllByPage(Page page);
    Index findByPageAndLemma(Page page, Lemma lemma);
}
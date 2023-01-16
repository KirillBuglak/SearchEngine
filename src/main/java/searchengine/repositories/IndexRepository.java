package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.Set;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Set<Index> findAllByLemma(Lemma lemma);

    Set<Index> findAllByPage(Page page);

    Index findByPageAndLemma(Page page, Lemma lemma);
}
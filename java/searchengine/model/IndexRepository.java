package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IndexRepository extends JpaRepository<Index,Integer> {
    Index findByKrank(float rank);
    List<Index> findAllByLemma_Id(int id);
    List<Index> findAllByPage(Page page);
}
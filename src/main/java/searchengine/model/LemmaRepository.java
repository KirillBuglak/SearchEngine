package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    List<Lemma> findByLemma(String lemma);
    List<Lemma> findBySiteUrl(String siteUrl);
    Lemma findByLemmaAndSite(String lemma, Site site);
}

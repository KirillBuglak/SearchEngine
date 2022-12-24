package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Set<Lemma> findByLemma(String lemma);
    Set<Lemma> findBySiteUrl(String siteUrl);
    Lemma findByLemmaAndSite(String lemma, Site site);
}

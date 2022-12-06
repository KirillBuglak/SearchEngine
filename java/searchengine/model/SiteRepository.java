package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteRepository extends JpaRepository<Site,Integer> {
    Site findByName(String name);
    Site findByUrl(String url);
    void deleteByUrl(String url);
}
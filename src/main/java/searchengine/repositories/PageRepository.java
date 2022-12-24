package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findByPath(String path);
    Set<Page> findAllBySite(Site site);
}

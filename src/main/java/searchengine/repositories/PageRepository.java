package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Set;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findByPathAndSite(String path, Site site);

    Set<Page> findAllBySite(Site site);
}

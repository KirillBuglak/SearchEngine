package searchengine.services;

import searchengine.dto.indexPage.IndexPageResponse;

public interface IndexPageService {
    IndexPageResponse getIndexPage(String url);
}

package searchengine.controllers;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexPage.IndexPageResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.stopIndexing.StopIndexingResponse;
import searchengine.services.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private IndexPageService indexPageService;
    @Autowired
    private StopIndexingService stopIndexingService;
    @Autowired
    private StartIndexingService startIndexingService;


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {//fixme ResponseEntity has Status(200) meths
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query, String site, int offset, int limit) {
        return ResponseEntity.ok(searchService.getSearch(query, site));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexPageResponse> indexPage(@Valid @RequestParam String url) {
        return ResponseEntity.ok(indexPageService.getIndexPage(url));
    }

    @SneakyThrows
    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndexingResponse> startIndexing() {
        return ResponseEntity.ok(startIndexingService.getStartIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<StopIndexingResponse> stopIndexing() {
        return ResponseEntity.ok(stopIndexingService.getStopIndexing());
    }
}

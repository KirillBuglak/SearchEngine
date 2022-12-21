package searchengine.controllers;

import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.IndexPageResponse;
import searchengine.dto.StartIndexingResponse;
import searchengine.dto.StopIndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final SearchService searchService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService,
                         SearchService searchService,
                         IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.searchService = searchService;
        this.indexingService = indexingService;
    }

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
        return ResponseEntity.ok(indexingService.getIndexPage(url));
    }

    @SneakyThrows
    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.getStartIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<StopIndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.getStopIndexing());
    }
}

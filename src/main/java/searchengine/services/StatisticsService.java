package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    @Autowired
    private final SitesList sites;
    @Autowired
    private final SiteService siteService;
    @Autowired
    private final PageService pageService;
    @Autowired
    private final LemmaService lemmaService;

    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        sites.getSites().forEach(siteConf -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteConf.getName());
            item.setUrl(siteConf.getUrl());
            int pages;
            int lemmas;
            if (siteService.getAllSites().size() != 0) {
                pages = pageService.getAllPagesBySite(siteService.getSiteByURL(siteConf.getUrl())).size();
                lemmas = lemmaService.getLemmasBySiteUrl(siteConf.getUrl()).size();
                item.setStatus(siteService.getSiteByURL(siteConf.getUrl()).getStatus().toString());
                item.setError(siteService.getSiteByURL(siteConf.getUrl()).getLastError());
                item.setStatusTime(siteService.getSiteByURL(siteConf.getUrl()).getStatusTime().getTime());
            } else {
                pages = 0;
                lemmas = 0;
                item.setStatus("FAILED");
                item.setError("Еще не индексировался");
                item.setStatusTime(new Date().getTime());
            }
            item.setPages(pages);
            item.setLemmas(lemmas);
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        });
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}

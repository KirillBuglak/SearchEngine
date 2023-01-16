package searchengine.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.dto.StopIndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.IndexingService;
import searchengine.services.modelServices.IndexService;
import searchengine.services.modelServices.LemmaService;
import searchengine.services.modelServices.PageService;
import searchengine.services.modelServices.SiteService;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.RecursiveAction;

@Slf4j
@Component
public class PageLemmaIndexDBSave extends RecursiveAction {
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final SiteService siteService;
    private Page page;
    private final String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";

    @Autowired
    public PageLemmaIndexDBSave(PageService pageService,
                                LemmaService lemmaService,
                                IndexService indexService,
                                SiteService siteService) {
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.siteService = siteService;
    }

    @SneakyThrows
    @Override
    public synchronized void compute() {
        if (stoppedByUser(page.getSite())) {
            this.get();
        }
        Document document = requestDoc(page.getFullPath());
        if (String.valueOf(page.getCode()).charAt(0) != 4 && String.valueOf(page.getCode()).charAt(0) != 5) {//fixme make it easier
            pageService.save(page);
            lemmaService.saveLemmas(page);
            indexService.saveIndexes(page);
            siteService.updateTimeOfSite(page.getSite());
        }

        Elements elements = null;
        if (document != null) {
            elements = document.select("a[href]");
        }

        if (elements != null) {
            elements.forEach(element -> {
                String link = element.absUrl("href");
                String pagePath = null;
                Document doc = null;

                if (link.contains(page.getFullPath()) && link.matches(pageRegEx)) {
                    pagePath = link.substring(page.getSite().getUrl().length());
                    try {
                        doc = getConnection(link).get();
                        Connection.Response execute = getConnection(link).execute();
                        execute.contentType();
                    } catch (IOException ignored) {
                    }
                }

                if ((pagePath != null && pageService.getPageByPathAndSite(pagePath, page.getSite()) == null
                        && !link.equals(page.getSite().getUrl()) && doc != null)) {
                    log.info(link);
                    Page newPage = new Page(page.getSite(), pagePath, 0, null);
                    PageLemmaIndexDBSave task = new PageLemmaIndexDBSave(pageService, lemmaService, indexService,
                            siteService);
                    task.setPage(newPage);
                    task.fork();
                    try {
                        task.join();
                    } catch (Exception e) {
                        log.error("Error in compute method, {}", e.toString());
                    }
                }
            });
        }
    }

    public Document requestDoc(String pageUrl) {
        Connection connection = getConnection(pageUrl);
        setPageCodeAndContent(page, connection);
        Document document;
        try {
            document = connection.get();
        } catch (Exception e) {
            document = null;
            log.error("Error in requestDoc method, {}", e.toString());
        }
        return document;
    }

    public void setPageCodeAndContent(Page page, Connection connection) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        StringBuilder content = new StringBuilder();
        int statusCode;
        try {
            statusCode = connection.execute().statusCode();
            connection.get().getAllElements().stream().map(Element::text).filter(string -> !string.isBlank()).toList()
                    .forEach(string -> content.append(string).append("\n"));
        } catch (Exception e) {
            statusCode = 404;
            content.append(e.getMessage());
            log.error("Error in setPageCodeAndContent method, {}", e.toString());
        }
        page.setCode(statusCode);
        page.setContent(content.toString()
//                .replaceAll("<[^>]*>", "")
        );
    }

    public Connection getConnection(String pageUrl) {
        return Jsoup.connect(pageUrl)
                .userAgent(pageService.getUserAgent())
                .referrer(pageService.getReferrer())
                .timeout(10000);
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public String getPageRegEx() {
        return pageRegEx;
    }
    private boolean stoppedByUser(Site site) {
        if (StopIndexingResponse.getResult()) {
            String userStop = "Индексация остановлена пользователем";
            if (!(Objects.equals(site.getLastError(), userStop))) {
                site.setLastError(userStop);
                siteService.saveSiteIndexedOrFailed(site);
            }
            return true;
        }
        return false;
    }
}
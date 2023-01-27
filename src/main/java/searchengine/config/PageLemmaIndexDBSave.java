package searchengine.config;

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
import searchengine.services.modelServices.IndexService;
import searchengine.services.modelServices.LemmaService;
import searchengine.services.modelServices.PageService;
import searchengine.services.modelServices.SiteService;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
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

    @Override
    public synchronized void compute() {
        Site site = page.getSite();
        Document document = requestDoc(page.getFullPath());
        if (document == null) {
            if (pageService.getAllPagesBySite(site).isEmpty()) {
                site.setLastError("Не проиндексировано ни одной страницы");
            }
            return;
        }
        pageService.save(page);
        lemmaService.saveLemmas(page);
        indexService.saveIndexes(page);

        siteService.updateTimeOfSite(site);

        Elements elements = document.select("a[href]");

        elements.forEach(element -> {
            if (stoppedByUser(site)) {
                try {
                    this.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            String link = element.absUrl("href");
            String pagePath;

            if (link.contains(page.getFullPath()) && link.matches(pageRegEx)) {
                pagePath = link.substring(site.getUrl().length());
            } else {
                return;
            }

            if ((pageService.getPageByPathAndSite(pagePath, site) == null && !link.equals(site.getUrl()))) {
                log.info(link);
                Page newPage = new Page(site, pagePath, 0, null);
                PageLemmaIndexDBSave task = new PageLemmaIndexDBSave(pageService, lemmaService, indexService,
                        siteService);
                task.setPage(newPage);
                task.fork();
                try {
                    task.join();
                } catch (Exception e) {
                        log.info("Ошибка во время индексации страницы с URL = " + page.getFullPath());
                }
            }
        });

    }

    public Document requestDoc(String pageUrl) {
        Connection connection = getConnection(pageUrl);
        Document document;
        try {
            document = connection.get();
            setPageCodeAndContent(page, connection);
        } catch (Exception e) {
            return null;
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
            statusCode = connection.ignoreHttpErrors(true).execute().statusCode();
            connection.get().getAllElements().stream().map(Element::text).filter(string -> !string.isBlank()).toList()
                    .forEach(string -> content.append(string).append("\n"));
        } catch (Exception e) {
            return;
        }
        page.setCode(statusCode);
        char firstDigit = String.valueOf(page.getCode()).charAt(0);
        if (firstDigit != 4 && firstDigit != 5) {
            page.setContent(content.toString());
        }
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
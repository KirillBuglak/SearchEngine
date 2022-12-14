package searchengine.config;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.util.concurrent.RecursiveAction;

@Component
public class PageLemmaIndexDBSave extends RecursiveAction {
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final SiteService siteService;
    private Page page;
    private int statusCode;
    private final StringBuilder content = new StringBuilder();
    private final String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";

    @Autowired
    public PageLemmaIndexDBSave(PageService pageService, LemmaService lemmaService, IndexService indexService, SiteService siteService) {
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.siteService = siteService;
    }

    @Override
    public void compute() {
        Document document = requestDoc(page.getPath());
        //saving to db
        page.setCode(statusCode);
        page.setContent(content.toString());
        pageService.save(page);
        if (String.valueOf(page.getCode()).charAt(0) != 4 && String.valueOf(page.getCode()).charAt(0) != 5) {//fixme make it easier
            lemmaService.saveLemmas(page);
            indexService.saveIndexes(page);
            siteService.updateTimeOfSite(page.getSite());
        }
        //saving to db
        Elements elements = null;
        if (document != null) {
            elements = document.select("a[href]");
        }
        if (elements != null) {
            elements.forEach(element -> {
                String link = element.absUrl("href");
                String pagePath = null;
                if (link.contains(page.getPath()) && link.matches(pageRegEx)) {
                    pagePath = link.substring(page.getSite().getUrl().length() - 1);
                }
                if ((pagePath != null && pageService.getPageByPath(pagePath) == null && !link.equals(page.getSite().getUrl()))) {
                    System.out.println(link);
                    Page newPage = new Page(page.getSite(), pagePath, 0, null);
                    PageLemmaIndexDBSave task = new PageLemmaIndexDBSave(this.pageService, this.lemmaService, this.indexService, siteService);
                    task.setPage(newPage);
                    task.fork();
                    try {
                        task.join();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @SneakyThrows
    private Document requestDoc(String pageUrl) {
        //todo may need to sleep here for a while
        Connection connection = Jsoup.connect(pageUrl)
                .userAgent(pageService.getUserAgent())
                .referrer(pageService.getReferrer())
                .timeout(20000);
        Document document;
        try {
            statusCode = connection
                    .ignoreContentType(true)
//                    .ignoreHttpErrors(true)
                    .execute().statusCode();
            document = connection
                    .get();
            document.getAllElements().stream().map(Element::text).filter(string -> !string.isBlank()).toList()
                    .forEach(string -> content.append(string).append("\n"));
        } catch (Exception e) {
            document = null;
//            statusCode = 404;
            content.append(e.getMessage());
            System.err.println(e.getMessage());
        }
        return document;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
}
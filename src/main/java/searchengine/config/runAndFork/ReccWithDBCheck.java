package searchengine.config.runAndFork;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.services.IndexService;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.util.concurrent.RecursiveAction;

//@Component
public class ReccWithDBCheck extends RecursiveAction {
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final SiteService siteService;
    private final Page page;
    private int statusCode;
    private final StringBuilder content = new StringBuilder();
    private final String pageRegEx = "https?://([-\\w.+=&?$%]+/?)+";

    public ReccWithDBCheck(Page page, PageService pageService, LemmaService lemmaService, IndexService indexService, SiteService siteService) {
        this.page = page;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.siteService = siteService;
    }
//fixme add services in all constructors

    //    @Autowired
//    public ReccWithDBCheck(PageService pageService, LemmaService lemmaService, IndexService indexService) {
//        this.pageService = pageService;
//        this.lemmaService = lemmaService;
//        this.indexService = indexService;
//    }
    @Override
    public void compute() {
        Document document = requestDoc(page.getSite().getUrl().equals(page.getPath())
                ? page.getPath() : page.getSite().getUrl() + page.getPath().substring(1));
        //fixme saving to db
        page.setCode(statusCode);
        page.setContent(content.toString());
        pageService.save(page);
        if (String.valueOf(page.getCode()).charAt(0) != 4 && String.valueOf(page.getCode()).charAt(0) != 5) {//fixme make it easier
            lemmaService.saveLemmas(page);
            indexService.saveIndexes(page);
            siteService.updateTimeOfSite(page.getSite());
        }
        //fixme saving to db
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
                    ReccWithDBCheck task = new ReccWithDBCheck(newPage, this.pageService, this.lemmaService, this.indexService, siteService);
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
    private Document requestDoc(String pageUrl) {//fixme may need to set code and content here for this.page
        //todo may need to sleep here for a while
        Connection connection = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; https://www.google.com/bot.html) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com")
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
}
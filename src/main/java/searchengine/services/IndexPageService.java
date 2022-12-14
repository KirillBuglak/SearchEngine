package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.IndexPageResponse;
import searchengine.model.Index;
import searchengine.model.Page;

import java.util.List;

@Service
public class IndexPageService {
    private static final String pagePathRegex = "https?://([-\\w.+=&?#$%]+/?)+";//todo modify this
    private final PageService pageService;
    private final SiteService siteService;
    private final LemmaService lemmaService;
    private final IndexService indexService;

    public IndexPageService(PageService pageService,
                            SiteService siteService,
                            LemmaService lemmaService,
                            IndexService indexService) {
        this.pageService = pageService;
        this.siteService = siteService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }

    private static final String content = "бирюзы Бирюзы Круглой Двадцати двадцати мне " +
            "побегу сверху раздетым работающим присев к или не пожалуй эй";

    @SneakyThrows
    public IndexPageResponse getIndexPage(String pagePath) {
        int statusCode = Jsoup.connect(pagePath).ignoreHttpErrors(true).followRedirects(false)
                .execute().statusCode();
        IndexPageResponse response = new IndexPageResponse();
        response.setResult(true);
        boolean result = siteService.isThereTheSite(pagePath);
        if (result) {
            Page pageFormRep = pageService.getPageByPath(pagePath);
            if (pageFormRep != null) {//fixme && may need to check it by status - just calling this page
                List<Index> indexesToDelete = indexService.getAllIndexesByPage(pageFormRep);
                indexService.deleteIndexes(indexesToDelete);
                lemmaService.deleteLemmasByPagePath(pagePath);
                pageService.deleteThePageByPath(pagePath);
            }
            pageService.savePage(siteService.getSiteByPagePath(pagePath), pagePath, statusCode, content);
            lemmaService.saveLemmas(pageService.getPageByPath(pagePath));
            indexService.saveIndexes(pageService.getPageByPath(pagePath));
        } else if (!pagePath.matches(pagePathRegex)) {
            response.setResult(false);
            response.setError("Проверьте правильность ввода адреса страницы");
        } else {//fixme may need to add if with a check for http status
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return response;
    }
}
package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.util.Date;

@Controller
public class DefaultController {

    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    @Autowired
    private SiteRepository siteRepository;
    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
package searchengine.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.services.IndexService;

@Component
public class TestAutowired {
    @Autowired
    IndexService indexService;
}

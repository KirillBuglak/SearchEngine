package searchengine.services;

import searchengine.config.runAndFork.Recc;
import searchengine.dto.startIndexing.StartIndexingResponse;

import java.util.List;

public interface StartIndexingService {
    StartIndexingResponse getStartIndexing();//todo may need to introduce some params
    List<Thread> getThreads();
    public boolean isFinished();
}

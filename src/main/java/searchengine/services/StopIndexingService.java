package searchengine.services;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.startIndexing.StartIndexingResponse;
import searchengine.dto.stopIndexing.StopIndexingResponse;

@Service
public class StopIndexingService {
    @Autowired
    private StopIndexingResponse stopResponse;
    @Autowired
    private StartIndexingResponse startResponse;
    @Autowired
    private StartIndexingService startIndexingService;

    @SneakyThrows
    public StopIndexingResponse getStopIndexing() {
        if ((startResponse.isResult() || startResponse.getError() != null) && !stopResponse.isResult()
                && startIndexingService.getThreads().stream().filter(Thread::isAlive).toList().size() != 0) {//
            startResponse.setResult(false);
            stopResponse.setResult(true);
            stopResponse.setError(null);
//            if (startIndexingService.getThreads().stream().filter(Thread::isAlive).toList().size()!=0) {
            startIndexingService.getThreads().forEach(Thread::interrupt);
//            }
        } else {
            stopResponse.setResult(false);
            stopResponse.setError("Индексация не запущена или завершена");
            startResponse.setError(null);
        }
        return stopResponse;
    }
}
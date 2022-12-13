package searchengine.services;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import searchengine.dto.StartIndexingResponse;
import searchengine.dto.StopIndexingResponse;

@Service
public class StopIndexingService {
    private final StopIndexingResponse stopResponse;
    private final StartIndexingResponse startResponse;
    private final StartIndexingService startIndexingService;

    public StopIndexingService(StopIndexingResponse stopResponse,
                               StartIndexingResponse startResponse,
                               StartIndexingService startIndexingService) {
        this.stopResponse = stopResponse;
        this.startResponse = startResponse;
        this.startIndexingService = startIndexingService;
    }

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
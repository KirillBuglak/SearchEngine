package searchengine.dto.stopIndexing;

import org.springframework.stereotype.Component;

@Component
public class StopIndexingResponse {
    private static volatile boolean result;
    private static volatile String error;

    public StopIndexingResponse() {
        result = true;
    }


    public boolean isResult() {
        return result;
    }

    public void setResult(boolean res) {
        result = res;
    }

    public String getError() {
        return error;
    }

    public void setError(String err) {
        error = err;
    }
}

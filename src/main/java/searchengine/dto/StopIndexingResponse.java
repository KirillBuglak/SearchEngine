package searchengine.dto;

import org.springframework.stereotype.Component;

@Component
public class StopIndexingResponse extends CommonResponse {
    private volatile boolean result;
    public StopIndexingResponse() {
        result = true;
    }
    @Override
    public boolean isResult() {
        return result;
    }
    @Override
    public void setResult(boolean result) {
        this.result = result;
    }
}

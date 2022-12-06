package searchengine.dto.indexPage;

public class IndexPageResponse {
    private boolean result;
    private String error;

    public IndexPageResponse() {
    }

    public IndexPageResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

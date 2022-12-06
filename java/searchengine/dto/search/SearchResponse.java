package searchengine.dto.search;

import java.util.List;

public class SearchResponse {
    private boolean result;
    private String error;
    private int count;
    private List<SearchPageData> data;

    public SearchResponse() {
    }

    public SearchResponse(boolean result, String error, int count, List<SearchPageData> data) {
        this.result = result;
        this.error = error;
        this.count = count;
        this.data = data;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<SearchPageData> getData() {
        return data;
    }

    public void setData(List<SearchPageData> data) {
        this.data = data;
    }
}

package searchengine.dto.search;

import searchengine.dto.CommonResponse;

import java.util.List;

public class SearchResponse extends CommonResponse {
    private int count;
    private List<SearchPageData> data;//fixme sort it by relevance

    public SearchResponse() {
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

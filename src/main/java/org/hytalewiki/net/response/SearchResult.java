package org.hytalewiki.net.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchResult {
    private List<SearchEntry> pages = new ArrayList<>();

    public SearchResult() {
    }

    public List<SearchEntry> getPages() {
        return Collections.unmodifiableList(pages);
    }
}

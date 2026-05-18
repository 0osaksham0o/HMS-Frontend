package com.hospital.frontend.model;

import java.util.List;
import java.util.Map;

public class PageResult {
    private List<Map<String, Object>> items;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
    public int getPrevPage() { return currentPage - 1; }
    public int getNextPage() { return currentPage + 1; }
}

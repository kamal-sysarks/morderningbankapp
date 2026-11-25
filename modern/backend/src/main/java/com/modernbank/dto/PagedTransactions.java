package com.modernbank.dto;

import com.modernbank.model.Transaction;
import java.util.List;

public class PagedTransactions {
    private List<Transaction> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public PagedTransactions(List<Transaction> content, long totalElements, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }

    public List<Transaction> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}

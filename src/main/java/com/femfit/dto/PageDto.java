package com.femfit.dto;

import lombok.Getter;
import java.util.List;

/**
 * Generic pagination wrapper returned by service layer.
 *
 * @param <T> the type of items on the page
 */
@Getter
public class PageDto<T> {

    private final List<T> content;
    private final int currentPage;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;

    public PageDto(List<T> content, int currentPage, int pageSize, int totalItems) {
        this.content = content;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    /** @return true if there is a next page */
    public boolean hasNext() { return currentPage < totalPages; }

    /** @return true if there is a previous page */
    public boolean hasPrev() { return currentPage > 1; }
}
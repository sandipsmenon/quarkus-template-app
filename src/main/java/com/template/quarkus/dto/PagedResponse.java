package com.template.quarkus.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response wrapper")
public class PagedResponse<T> {

    @Schema(description = "List of items in this page")
    public List<T> content;

    @Schema(description = "Current page number (0-based)", example = "0")
    public int page;

    @Schema(description = "Number of items per page", example = "10")
    public int size;

    @Schema(description = "Total number of items across all pages", example = "42")
    public long totalElements;

    @Schema(description = "Total number of pages", example = "5")
    public int totalPages;

    @Schema(description = "Whether this is the first page", example = "true")
    public boolean first;

    @Schema(description = "Whether this is the last page", example = "false")
    public boolean last;

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        PagedResponse<T> response = new PagedResponse<>();
        response.content = content;
        response.page = page;
        response.size = size;
        response.totalElements = totalElements;
        response.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        response.first = page == 0;
        response.last = page >= response.totalPages - 1;
        return response;
    }
}

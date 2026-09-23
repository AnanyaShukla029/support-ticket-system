package com.ttn.supporttickets.ticket.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pagination envelope for list endpoints.
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
}

package com.diego.plazoleta.infrastructure.input.rest.dto;

import java.util.List;

public record RestaurantPageResponse(List<RestaurantListItem> content, int page, int size,
                                     long totalElements, int totalPages) {
}

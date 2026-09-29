package com.diego.plazoleta.domain.model;

import java.util.List;

public record RestaurantPage(List<Restaurant> items, int page, int size, long totalElements, int totalPages) {
}

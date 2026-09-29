package com.diego.plazoleta.domain.model;

public record Dish(Long id, Long restaurantId, String name, Integer price, String description,
                   String imageUrl, String category, boolean active) {
}

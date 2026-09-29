package com.diego.plazoleta.infrastructure.input.rest.dto;

public record DishResponse(Long id, Long restaurantId, String name, Integer price, String description,
                           String imageUrl, String category, boolean active) {
}

package com.diego.plazoleta.infrastructure.input.rest.dto;

public record RestaurantResponse(Long id, String name, String nit, String address, String phone,
                                 String logoUrl, String ownerId) {
}

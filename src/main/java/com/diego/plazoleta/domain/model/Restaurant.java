package com.diego.plazoleta.domain.model;

public record Restaurant(Long id, String name, String nit, String address, String phone, String logoUrl,
                         String ownerId) {
}

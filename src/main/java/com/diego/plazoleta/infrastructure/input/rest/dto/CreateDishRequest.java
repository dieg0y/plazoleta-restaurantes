package com.diego.plazoleta.infrastructure.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateDishRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Positive Integer price,
        @NotBlank @Size(max = 1000) String description,
        @NotBlank @Size(max = 500) String imageUrl,
        @NotBlank @Size(max = 80) String category,
        Boolean active) {
}

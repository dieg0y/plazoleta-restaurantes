package com.diego.plazoleta.infrastructure.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateDishRequest(@NotNull @Positive Integer price,
                               @NotBlank @Size(max = 1000) String description) {
}

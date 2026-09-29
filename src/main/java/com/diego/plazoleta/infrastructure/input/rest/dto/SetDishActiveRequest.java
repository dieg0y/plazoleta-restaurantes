package com.diego.plazoleta.infrastructure.input.rest.dto;

import jakarta.validation.constraints.NotNull;

public record SetDishActiveRequest(@NotNull Boolean active) {
}

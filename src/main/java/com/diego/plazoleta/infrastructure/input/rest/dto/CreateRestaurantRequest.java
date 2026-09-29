package com.diego.plazoleta.infrastructure.input.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRestaurantRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "\\d+", message = "NIT must contain digits only") @Size(max = 30) String nit,
        @NotBlank @Size(max = 250) String address,
        @NotBlank @Pattern(regexp = "^(?:[0-9]{1,13}|\\+[0-9]{1,12})$", message = "Phone must be numeric, optionally prefixed by +, and at most 13 characters") String phone,
        @NotBlank @Size(max = 500) String logoUrl,
        @NotBlank @Pattern(regexp = "\\d+", message = "Owner user ID must be numeric") @Size(max = 19) String ownerId) {
}

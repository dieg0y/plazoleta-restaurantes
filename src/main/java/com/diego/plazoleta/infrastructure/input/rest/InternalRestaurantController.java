package com.diego.plazoleta.infrastructure.input.rest;

import com.diego.plazoleta.application.usecase.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class InternalRestaurantController {
    private final RestaurantService restaurantService;

    public InternalRestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Operation(summary = "Check whether a restaurant belongs to the authenticated proprietor (HU06 integration)")
    @GetMapping("/internal/restaurantes/{restaurantId}/propietarios/{proprietorId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public boolean isRestaurantOwnedBy(@PathVariable Long restaurantId, @PathVariable String proprietorId,
                                       @AuthenticationPrincipal Jwt jwt) {
        return proprietorId.equals(jwt.getSubject())
                && restaurantService.isRestaurantOwnedBy(restaurantId, proprietorId);
    }
}

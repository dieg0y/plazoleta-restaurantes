package com.diego.plazoleta.infrastructure.input.rest;

import com.diego.plazoleta.application.usecase.RestaurantService;
import com.diego.plazoleta.domain.model.Restaurant;
import com.diego.plazoleta.infrastructure.input.rest.dto.*;
import com.diego.plazoleta.infrastructure.input.rest.mapper.DishRequestMapper;
import com.diego.plazoleta.infrastructure.input.rest.mapper.RestaurantRequestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class RestaurantController {
    private final RestaurantService service;
    private final RestaurantRequestMapper restaurantMapper;
    private final DishRequestMapper dishMapper;

    public RestaurantController(RestaurantService service, RestaurantRequestMapper restaurantMapper,
                                DishRequestMapper dishMapper) {
        this.service = service;
        this.restaurantMapper = restaurantMapper;
        this.dishMapper = dishMapper;
    }

    @Operation(summary = "Create a restaurant (HU02)")
    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RestaurantResponse createRestaurant(@Valid @RequestBody CreateRestaurantRequest request,
                                               @RequestHeader("Authorization") String authorization) {
        Restaurant created = service.createRestaurant(restaurantMapper.toDomain(request), authorization);
        return restaurantMapper.toResponse(created);
    }

    @Operation(summary = "List restaurants alphabetically (HU09)")
    @GetMapping("/restaurants")
    @PreAuthorize("hasRole('CLIENTE')")
    public RestaurantPageResponse listRestaurants(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        var result = service.listRestaurants(page, size);
        List<RestaurantListItem> items = result.items().stream()
                .map(restaurant -> new RestaurantListItem(restaurant.name(), restaurant.logoUrl())).toList();
        return new RestaurantPageResponse(items, result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    @Operation(summary = "Create a dish for a restaurant (HU03)")
    @PostMapping("/restaurants/{restaurantId}/dishes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROPIETARIO')")
    public DishResponse createDish(@PathVariable Long restaurantId, @Valid @RequestBody CreateDishRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return dishMapper.toResponse(service.createDish(restaurantId, jwt.getSubject(), dishMapper.toDomain(request)));
    }

    @Operation(summary = "Update only dish price and description (HU04)")
    @PatchMapping("/dishes/{dishId}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public DishResponse updateDish(@PathVariable Long dishId, @Valid @RequestBody UpdateDishRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return dishMapper.toResponse(service.updateDish(dishId, jwt.getSubject(), request.price(), request.description()));
    }

    @Operation(summary = "Activate or deactivate an owned dish (HU07)")
    @PatchMapping("/dishes/{dishId}/status")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public DishResponse setDishActive(@PathVariable Long dishId, @Valid @RequestBody SetDishActiveRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return dishMapper.toResponse(service.setDishActive(dishId, jwt.getSubject(), request.active()));
    }
}

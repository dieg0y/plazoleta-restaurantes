package com.diego.plazoleta.infrastructure.input.rest.mapper;

import com.diego.plazoleta.domain.model.Restaurant;
import com.diego.plazoleta.infrastructure.input.rest.dto.CreateRestaurantRequest;
import com.diego.plazoleta.infrastructure.input.rest.dto.RestaurantResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RestaurantRequestMapper {
    Restaurant toDomain(CreateRestaurantRequest request);
    RestaurantResponse toResponse(Restaurant restaurant);
}

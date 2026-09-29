package com.diego.plazoleta.infrastructure.input.rest.mapper;

import com.diego.plazoleta.domain.model.Dish;
import com.diego.plazoleta.infrastructure.input.rest.dto.CreateDishRequest;
import com.diego.plazoleta.infrastructure.input.rest.dto.DishResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DishRequestMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurantId", ignore = true)
    @Mapping(target = "active", expression = "java(request.active() == null || request.active())")
    Dish toDomain(CreateDishRequest request);
    DishResponse toResponse(Dish dish);
}

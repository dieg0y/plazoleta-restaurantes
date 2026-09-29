package com.diego.plazoleta.infrastructure.output.jpa.mapper;

import com.diego.plazoleta.domain.model.Dish;
import com.diego.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DishEntityMapper {
    @Mapping(target = "restaurantId", source = "restaurant.id")
    Dish toDomain(DishEntity entity);

    @Mapping(target = "restaurant", ignore = true)
    DishEntity toEntity(Dish dish);
}

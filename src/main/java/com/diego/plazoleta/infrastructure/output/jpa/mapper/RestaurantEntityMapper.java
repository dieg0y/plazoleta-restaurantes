package com.diego.plazoleta.infrastructure.output.jpa.mapper;

import com.diego.plazoleta.domain.model.Restaurant;
import com.diego.plazoleta.infrastructure.output.jpa.entity.RestaurantEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RestaurantEntityMapper {
    Restaurant toDomain(RestaurantEntity entity);
    RestaurantEntity toEntity(Restaurant restaurant);
}

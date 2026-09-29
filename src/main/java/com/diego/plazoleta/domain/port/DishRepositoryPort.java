package com.diego.plazoleta.domain.port;

import com.diego.plazoleta.domain.model.Dish;
import java.util.Optional;

public interface DishRepositoryPort {
    Dish save(Dish dish);
    Optional<Dish> findById(Long id);
}

package com.diego.plazoleta.domain.port;

import com.diego.plazoleta.domain.model.Restaurant;
import com.diego.plazoleta.domain.model.RestaurantPage;
import java.util.Optional;

public interface RestaurantRepositoryPort {
    Restaurant save(Restaurant restaurant);
    Optional<Restaurant> findById(Long id);
    RestaurantPage findAllAlphabetically(int page, int size);
}

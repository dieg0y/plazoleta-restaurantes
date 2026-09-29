package com.diego.plazoleta.application.usecase;

import com.diego.plazoleta.domain.exception.ForbiddenOperationException;
import com.diego.plazoleta.domain.exception.InvalidInputException;
import com.diego.plazoleta.domain.exception.NotFoundException;
import com.diego.plazoleta.domain.model.Dish;
import com.diego.plazoleta.domain.model.Restaurant;
import com.diego.plazoleta.domain.model.RestaurantPage;
import com.diego.plazoleta.domain.port.DishRepositoryPort;
import com.diego.plazoleta.domain.port.RestaurantRepositoryPort;
import com.diego.plazoleta.domain.port.UserValidationPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {
    private static final int MAX_PAGE_SIZE = 100;
    private final RestaurantRepositoryPort restaurants;
    private final DishRepositoryPort dishes;
    private final UserValidationPort userValidation;

    public RestaurantService(RestaurantRepositoryPort restaurants, DishRepositoryPort dishes,
                             UserValidationPort userValidation) {
        this.restaurants = restaurants;
        this.dishes = dishes;
        this.userValidation = userValidation;
    }

    @Transactional
    public Restaurant createRestaurant(Restaurant restaurant, String authorization) {
        validateRestaurantName(restaurant.name());
        if (restaurant.nit() == null || !restaurant.nit().matches("\\d+")) {
            throw new InvalidInputException("NIT must contain digits only");
        }
        if (restaurant.phone() == null || !restaurant.phone().matches("^(?:[0-9]{1,13}|\\+[0-9]{1,12})$")) {
            throw new InvalidInputException("Phone must contain digits and optionally a leading + (13 characters maximum)");
        }
        if (!isValidOwnerId(restaurant.ownerId())) {
            throw new InvalidInputException("Owner user ID must be a positive numeric user ID within the supported range");
        }
        if (!userValidation.isRestaurantOwner(restaurant.ownerId(), authorization)) {
            throw new InvalidInputException("The specified user is not a restaurant owner");
        }
        return restaurants.save(restaurant);
    }

    public boolean isRestaurantOwnedBy(Long restaurantId, String proprietorId) {
        return restaurants.findById(restaurantId)
                .map(restaurant -> restaurant.ownerId().equals(proprietorId))
                .orElse(false);
    }

    public RestaurantPage listRestaurants(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidInputException("Page must be non-negative and size must be between 1 and 100");
        }
        return restaurants.findAllAlphabetically(page, size);
    }

    @Transactional
    public Dish createDish(Long restaurantId, String currentOwnerId, Dish dish) {
        Restaurant restaurant = requireRestaurant(restaurantId);
        checkOwner(restaurant, currentOwnerId);
        return dishes.save(new Dish(null, restaurantId, dish.name(), dish.price(), dish.description(),
                dish.imageUrl(), dish.category(), dish.active()));
    }

    @Transactional
    public Dish updateDish(Long dishId, String currentOwnerId, Integer price, String description) {
        Dish existing = requireDish(dishId);
        Restaurant restaurant = requireRestaurant(existing.restaurantId());
        checkOwner(restaurant, currentOwnerId);
        return dishes.save(new Dish(existing.id(), existing.restaurantId(), existing.name(), price, description,
                existing.imageUrl(), existing.category(), existing.active()));
    }

    @Transactional
    public Dish setDishActive(Long dishId, String currentOwnerId, boolean active) {
        Dish existing = requireDish(dishId);
        Restaurant restaurant = requireRestaurant(existing.restaurantId());
        checkOwner(restaurant, currentOwnerId);
        return dishes.save(new Dish(existing.id(), existing.restaurantId(), existing.name(), existing.price(),
                existing.description(), existing.imageUrl(), existing.category(), active));
    }

    private Restaurant requireRestaurant(Long id) {
        return restaurants.findById(id).orElseThrow(() -> new NotFoundException("Restaurant not found"));
    }

    private Dish requireDish(Long id) {
        return dishes.findById(id).orElseThrow(() -> new NotFoundException("Dish not found"));
    }

    private void checkOwner(Restaurant restaurant, String currentOwnerId) {
        if (currentOwnerId == null || !restaurant.ownerId().equals(currentOwnerId)) {
            throw new ForbiddenOperationException("Only the restaurant owner may manage its dishes");
        }
    }

    private void validateRestaurantName(String name) {
        if (name == null || name.isBlank() || name.trim().matches("\\d+")) {
            throw new InvalidInputException("Restaurant name must not be blank or contain only digits");
        }
    }

    private boolean isValidOwnerId(String ownerId) {
        if (ownerId == null || !ownerId.matches("\\d{1,19}")) {
            return false;
        }
        try {
            return Long.parseLong(ownerId) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}

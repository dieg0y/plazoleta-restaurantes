package com.diego.plazoleta.infrastructure.output.jpa.adapter;

import com.diego.plazoleta.domain.model.Dish;
import com.diego.plazoleta.domain.port.DishRepositoryPort;
import com.diego.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import com.diego.plazoleta.infrastructure.output.jpa.mapper.DishEntityMapper;
import com.diego.plazoleta.infrastructure.output.jpa.repository.DishJpaRepository;
import com.diego.plazoleta.infrastructure.output.jpa.repository.RestaurantJpaRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class DishPersistenceAdapter implements DishRepositoryPort {
    private final DishJpaRepository repository;
    private final RestaurantJpaRepository restaurantRepository;
    private final DishEntityMapper mapper;

    public DishPersistenceAdapter(DishJpaRepository repository, RestaurantJpaRepository restaurantRepository,
                                  DishEntityMapper mapper) {
        this.repository = repository;
        this.restaurantRepository = restaurantRepository;
        this.mapper = mapper;
    }

    @Override
    public Dish save(Dish dish) {
        DishEntity entity = mapper.toEntity(dish);
        entity.setRestaurant(restaurantRepository.getReferenceById(dish.restaurantId()));
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Dish> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}

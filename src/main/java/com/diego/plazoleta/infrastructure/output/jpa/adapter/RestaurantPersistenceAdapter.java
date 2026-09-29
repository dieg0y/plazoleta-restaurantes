package com.diego.plazoleta.infrastructure.output.jpa.adapter;

import com.diego.plazoleta.domain.model.Restaurant;
import com.diego.plazoleta.domain.model.RestaurantPage;
import com.diego.plazoleta.domain.port.RestaurantRepositoryPort;
import com.diego.plazoleta.infrastructure.output.jpa.entity.RestaurantEntity;
import com.diego.plazoleta.infrastructure.output.jpa.mapper.RestaurantEntityMapper;
import com.diego.plazoleta.infrastructure.output.jpa.repository.RestaurantJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class RestaurantPersistenceAdapter implements RestaurantRepositoryPort {
    private final RestaurantJpaRepository repository;
    private final RestaurantEntityMapper mapper;

    public RestaurantPersistenceAdapter(RestaurantJpaRepository repository, RestaurantEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        return mapper.toDomain(repository.save(mapper.toEntity(restaurant)));
    }

    @Override
    public Optional<Restaurant> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public RestaurantPage findAllAlphabetically(int page, int size) {
        Page<RestaurantEntity> result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
        return new RestaurantPage(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}

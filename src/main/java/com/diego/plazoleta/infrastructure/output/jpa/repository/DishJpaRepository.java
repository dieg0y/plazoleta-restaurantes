package com.diego.plazoleta.infrastructure.output.jpa.repository;

import com.diego.plazoleta.infrastructure.output.jpa.entity.DishEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishJpaRepository extends JpaRepository<DishEntity, Long> {
}

package com.ecommerce.api.repository;

import com.ecommerce.api.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);
}

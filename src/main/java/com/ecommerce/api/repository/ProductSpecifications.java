package com.ecommerce.api.repository;

import com.ecommerce.api.domain.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(
            String search,
            Long categoryId,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            Boolean featured,
            Boolean inStock
    ) {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("category", jakarta.persistence.criteria.JoinType.INNER);
                query.distinct(true);
            }

            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                var categoryJoin = root.join("category", jakarta.persistence.criteria.JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(categoryJoin.get("name")), pattern),
                        cb.like(cb.lower(categoryJoin.get("slug")), pattern)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (Boolean.TRUE.equals(featured)) {
                predicates.add(cb.isTrue(root.get("featured")));
            }
            if (Boolean.TRUE.equals(inStock)) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

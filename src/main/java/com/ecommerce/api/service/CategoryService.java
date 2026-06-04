package com.ecommerce.api.service;
import com.ecommerce.api.domain.Category;
import com.ecommerce.api.dto.category.CategoryRequest;
import com.ecommerce.api.dto.category.CategoryResponse;
import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    @Transactional(readOnly = true)
    @Cacheable("categories")
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
            .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug())).toList();
    }
    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.findBySlug(request.slug()).isPresent()) {
            throw new BadRequestException("Category slug already exists");
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(request.name()).slug(request.slug()).build());
        return new CategoryResponse(saved.getId(), saved.getName(), saved.getSlug());
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getById(id);
        categoryRepository.findBySlug(request.slug()).filter(c -> !c.getId().equals(id))
                .ifPresent(c -> { throw new BadRequestException("Category slug already exists"); });
        category.setName(request.name());
        category.setSlug(request.slug());
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        categoryRepository.deleteById(id);
    }
}

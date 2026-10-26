package com.campusflow.category.service;

import com.campusflow.category.dto.CategoryRequest;
import com.campusflow.category.dto.CategoryResponse;
import com.campusflow.category.entity.Category;
import com.campusflow.category.repository.CategoryRepository;
import com.campusflow.common.exception.ApiException;
import com.campusflow.item.repository.ItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("id")).stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Category getEntity(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Category not found"));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())
                || categoryRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("A category with this name or slug already exists");
        }
        Category category = categoryRepository.save(Category.builder()
                .name(request.name().trim())
                .slug(request.slug())
                .description(request.description())
                .iconUrl(request.iconUrl())
                .build());
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)
                || categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw ApiException.conflict("A category with this name or slug already exists");
        }
        category.setName(request.name().trim());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setIconUrl(request.iconUrl());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getEntity(id);
        if (itemRepository.existsByCategoryId(id)) {
            throw ApiException.conflict("Cannot delete a category that still has items");
        }
        categoryRepository.delete(category);
    }
}

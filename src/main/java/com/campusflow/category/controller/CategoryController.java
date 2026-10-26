package com.campusflow.category.controller;

import com.campusflow.category.dto.CategoryResponse;
import com.campusflow.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Categories", description = "Item categories")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "List all categories")
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }
}

package com.tracker.controller;

import com.tracker.dto.CategoryDTO;
import com.tracker.security.SecurityUtils;
import com.tracker.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Expense and Income category management")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create a new category (USER and ADMIN only)")
    public ResponseEntity<CategoryDTO> createCategory(@RequestBody CategoryDTO categoryDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(categoryService.createCategory(userId, categoryDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all categories for the authenticated user")
    public ResponseEntity<List<CategoryDTO>> getCategories() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.getCategoriesByUserId(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        CategoryDTO dto = CategoryDTO.builder().build();
        var entity = categoryService.getCategoryEntity(id, userId);
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setDescription(entity.getDescription());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update a category (USER and ADMIN only)")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.updateCategory(userId, id, categoryDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category (ADMIN only)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        categoryService.deleteCategory(userId, id);
        return ResponseEntity.noContent().build();
    }
}

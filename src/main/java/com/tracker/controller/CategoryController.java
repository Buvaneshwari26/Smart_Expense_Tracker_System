package com.tracker.controller;

import com.tracker.dto.CategoryDTO;
import com.tracker.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Expense and Income category management")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<CategoryDTO> createCategory(@RequestParam Long userId,
                                                       @RequestBody CategoryDTO categoryDTO) {
        return new ResponseEntity<>(categoryService.createCategory(userId, categoryDTO), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all categories for a user")
    public ResponseEntity<List<CategoryDTO>> getCategories(@RequestParam Long userId) {
        return ResponseEntity.ok(categoryService.getCategoriesByUserId(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID")
    public ResponseEntity<CategoryDTO> getCategoryById(@RequestParam Long userId, @PathVariable Long id) {
        CategoryDTO dto = CategoryDTO.builder().build();
        var entity = categoryService.getCategoryEntity(id, userId);
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setDescription(entity.getDescription());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<CategoryDTO> updateCategory(@RequestParam Long userId, @PathVariable Long id,
                                                       @RequestBody CategoryDTO categoryDTO) {
        return ResponseEntity.ok(categoryService.updateCategory(userId, id, categoryDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a category")
    public ResponseEntity<Void> deleteCategory(@RequestParam Long userId, @PathVariable Long id) {
        categoryService.deleteCategory(userId, id);
        return ResponseEntity.noContent().build();
    }
}

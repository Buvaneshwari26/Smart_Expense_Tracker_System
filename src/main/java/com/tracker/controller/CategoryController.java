package com.tracker.controller;

import com.tracker.dto.CategoryDTO;
import com.tracker.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(
            @RequestParam Long userId,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO response = categoryService.createCategory(userId, categoryDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getCategories(@RequestParam Long userId) {
        List<CategoryDTO> response = categoryService.getCategoriesByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO response = categoryService.updateCategory(userId, id, categoryDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(
            @PathVariable Long id,
            @RequestParam Long userId) {
        categoryService.deleteCategory(userId, id);
        return ResponseEntity.ok("Category deleted successfully.");
    }
}

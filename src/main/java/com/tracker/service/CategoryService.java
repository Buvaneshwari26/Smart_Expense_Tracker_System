package com.tracker.service;

import com.tracker.dto.CategoryDTO;
import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import com.tracker.model.Category;
import com.tracker.model.User;
import com.tracker.repository.CategoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Transactional
    public CategoryDTO createCategory(Long userId, CategoryDTO categoryDTO) {
        User user = userService.getUserEntity(userId);

        // Check if category name already exists for this user (case-insensitive)
        if (categoryRepository.existsByNameIgnoreCaseAndUserId(categoryDTO.getName(), userId)) {
            throw new BadRequestException("Category with name '" + categoryDTO.getName() + "' already exists for this user.");
        }

        Category category = Category.builder()
                .name(categoryDTO.getName())
                .type(categoryDTO.getType())
                .description(categoryDTO.getDescription())
                .user(user)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToDTO(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getCategoriesByUserId(Long userId) {
        // Ensure user exists
        userService.getUserEntity(userId);

        List<Category> categories = categoryRepository.findByUserId(userId);
        return categories.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public CategoryDTO updateCategory(Long userId, Long categoryId, CategoryDTO categoryDTO) {
        Category category = getCategoryEntity(categoryId, userId);

        // If name changes, check for unique name conflict
        if (!category.getName().equalsIgnoreCase(categoryDTO.getName()) &&
                categoryRepository.existsByNameIgnoreCaseAndUserId(categoryDTO.getName(), userId)) {
            throw new BadRequestException("Category with name '" + categoryDTO.getName() + "' already exists for this user.");
        }

        category.setName(categoryDTO.getName());
        category.setType(categoryDTO.getType());
        category.setDescription(categoryDTO.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return mapToDTO(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = getCategoryEntity(categoryId, userId);
        // Delete category
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long id, Long userId) {
        // ADMIN users can access any category regardless of ownership
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        }
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id + " for user id: " + userId));
    }

    /** Fetch a category by ID only (no userId filter) — for ADMIN/AUDITOR read access. */
    @Transactional(readOnly = true)
    public Category getCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private CategoryDTO mapToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .description(category.getDescription())
                .build();
    }
}

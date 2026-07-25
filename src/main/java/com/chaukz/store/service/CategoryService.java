package com.chaukz.store.service;

import com.chaukz.store.dto.request.CategoryRequest;
import com.chaukz.store.dto.response.CategoryResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.CategoryMapper;
import com.chaukz.store.model.Category;
import com.chaukz.store.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse getById(Long id) {
        Category category = findCategoryOrThrow(id);
        return categoryMapper.toResponse(category);
    }

    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);
        categoryMapper.updateEntity(category, request);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    public void delete(Long id) {
        Category category = findCategoryOrThrow(id);
        categoryRepository.delete(category);
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}

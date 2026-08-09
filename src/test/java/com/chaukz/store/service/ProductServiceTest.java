package com.chaukz.store.service;

import com.chaukz.store.dto.request.ProductRequest;
import com.chaukz.store.dto.response.PageResponse;
import com.chaukz.store.dto.response.ProductResponse;
import com.chaukz.store.exception.ResourceNotFoundException;
import com.chaukz.store.mapper.ProductMapper;
import com.chaukz.store.model.Category;
import com.chaukz.store.model.Product;
import com.chaukz.store.repository.CategoryRepository;
import com.chaukz.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProductService. The actual SQL a Specification produces
 * can only be proven against a real database (a @DataJpaTest concern) -
 * what's tested here is that ProductService asks the repository correctly
 * and maps whatever comes back correctly, which is ProductService's job.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    private ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        ProductMapper productMapper = new ProductMapper(categoryRepository);
        productService = new ProductService(productRepository, productMapper);

        category = new Category();
        category.setId(1L);
        category.setName("Shoes");

        product = new Product();
        product.setId(5L);
        product.setCategory(category);
        product.setName("Running Sneaker");
        product.setPrice(new BigDecimal("899.99"));
        product.setStockQuantity(50);
        product.setActive(true);
    }

    @Test
    void getAll_delegatesToRepositoryAndMapsResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> response =
                productService.getAll(1L, null, null, null, null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("Running Sneaker");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void create_looksUpCategoryAndSaves() {
        ProductRequest request = new ProductRequest(
                1L, "New Shoe", "desc", "SKU-1", "Nova",
                new BigDecimal("500.00"), 10, true);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        ProductResponse response = productService.create(request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.categoryName()).isEqualTo("Shoes");
    }

    @Test
    void create_categoryDoesNotExist_throwsResourceNotFound() {
        ProductRequest request = new ProductRequest(
                999L, "New Shoe", "desc", "SKU-1", "Nova",
                new BigDecimal("500.00"), 10, true);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_removesTheProduct() {
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        productService.delete(5L);

        org.mockito.Mockito.verify(productRepository).delete(product);
    }
}

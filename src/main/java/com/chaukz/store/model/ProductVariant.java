package com.chaukz.store.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private String size;

    private String color;

    private BigDecimal price;

    private Integer stockQuantity;

    // Optimistic locking. Hibernate manages this column itself - never
    // set it manually. See V2__add_optimistic_locking_to_product_variants.sql
    // for why it's here.
    @Version
    private Long version;

}
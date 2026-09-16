package com.chaukz.store.repository;

import com.chaukz.store.model.Order;
import com.chaukz.store.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // address is fetched here because OrderMapper reads its actual fields
    // (street, city, etc.) to build the shipping-address string - without
    // this, that's one extra SELECT per order. user is NOT fetched:
    // OrderMapper only ever reads order.getUser().getId(), and calling
    // getId() on a lazy proxy never triggers a query, so eagerly joining
    // it here would just be wasted work.
    @EntityGraph(attributePaths = {"address"})
    List<Order> findByUserId(Long userId);

    @Override
    @EntityGraph(attributePaths = {"address"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"address"})
    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);


}

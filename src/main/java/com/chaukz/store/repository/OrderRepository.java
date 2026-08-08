package com.chaukz.store.repository;

import com.chaukz.store.model.Order;
import com.chaukz.store.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);
}

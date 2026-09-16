package com.chaukz.store.repository;

import com.chaukz.store.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // Batched version: one query for ALL items across a whole page of
    // orders, with productVariant and its product pre-joined. This is
    // the actual N+1 fix - see OrderService.buildResponses().
    //
    // JOIN FETCH is safe here because we're joining *ManyToOne*
    // associations, not a collection. Fetch-joining a *collection*
    // together with a paginated query is a different, much trickier
    // problem - Hibernate can't paginate correctly once a collection
    // join multiplies the row count (this is the classic
    // MultipleBagFetchException / silently-wrong-pagination trap). That's
    // exactly why this batches by an explicit list of order IDs instead
    // of trying to fetch-join straight off a Pageable query.
    @Query("SELECT oi FROM OrderItem oi " +
            "JOIN FETCH oi.productVariant pv " +
            "JOIN FETCH pv.product " +
            "WHERE oi.order.id IN :orderIds")
    List<OrderItem> findByOrderIdInWithVariantAndProduct(@Param("orderIds") List<Long> orderIds);
}
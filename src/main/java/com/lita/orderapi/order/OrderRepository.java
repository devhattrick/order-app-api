package com.lita.orderapi.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<ShopOrder, UUID>, JpaSpecificationExecutor<ShopOrder> {

    Optional<ShopOrder> findByTrackingToken(String trackingToken);

    List<ShopOrder> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    List<ShopOrder> findByTableNoAndStatusInOrderByCreatedAtAsc(String tableNo, Collection<OrderStatus> statuses);

    List<ShopOrder> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant from, Instant to);
}

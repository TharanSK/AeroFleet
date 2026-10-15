package com.aerofleet.repository;

import com.aerofleet.domain.enums.OrderStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {
    List<DeliveryOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status);
    List<DeliveryOrder> findByAssignedDroneId(Long droneId);
    Optional<DeliveryOrder> findByOrderNumber(String orderNumber);
    long countByStatus(OrderStatus status);
}

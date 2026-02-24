package com.sample.purchase.domain.repository;

import com.sample.purchase.domain.model.PurchaseOrder;
import com.sample.purchase.domain.vo.PurchaseOrderId;

import java.util.Optional;

/**
 * 采购订单仓储接口。
 * 定义在 Domain 层，实现在 Infrastructure 层。
 */
public interface PurchaseOrderRepository {

    void save(PurchaseOrder order);

    Optional<PurchaseOrder> findById(PurchaseOrderId id);
}

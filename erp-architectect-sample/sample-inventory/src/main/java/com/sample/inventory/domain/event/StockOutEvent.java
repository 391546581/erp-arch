package com.sample.inventory.domain.event;

import com.sample.common.DomainEvent;
import com.sample.common.vo.Money;
import com.sample.inventory.domain.vo.SnCode;

/**
 * === 设计要点 ===
 * StockOutEvent：出库确认领域事件。
 *
 * 当一个 SN 被确认出库(SOLD)时发布。
 * 消费者：财务中心 — 执行成本结转，计算毛利。
 *
 * 事件的数据设计原则：
 * 1. 只携带消费者需要的最小数据
 * 2. 使用值对象而非基本类型
 * 3. 不可变（所有字段 final）
 */
public class StockOutEvent extends DomainEvent {

    /** 出库的 SN */
    private final SnCode snCode;

    /** 关联的销售订单 ID */
    private final String salesOrderId;

    /** 该 SN 的累计总成本（财务用于计算毛利） */
    private final Money accumulatedCost;

    public StockOutEvent(SnCode snCode, String salesOrderId, Money accumulatedCost) {
        super(snCode.getValue());
        this.snCode = snCode;
        this.salesOrderId = salesOrderId;
        this.accumulatedCost = accumulatedCost;
    }

    public SnCode getSnCode() {
        return snCode;
    }

    public String getSalesOrderId() {
        return salesOrderId;
    }

    public Money getAccumulatedCost() {
        return accumulatedCost;
    }
}

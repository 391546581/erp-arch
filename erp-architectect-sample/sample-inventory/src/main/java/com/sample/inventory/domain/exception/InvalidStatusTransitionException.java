package com.sample.inventory.domain.exception;

import com.sample.inventory.domain.model.InventoryStatus;
import com.sample.inventory.domain.vo.SnCode;

/**
 * === 设计要点 ===
 * 专用领域异常：非法状态转换。
 *
 * 为什么不用通用的 IllegalStateException？
 * 1. 携带业务上下文信息（SN, 当前状态, 目标状态）
 * 2. 全局异常处理器可以根据异常类型返回不同的 HTTP 状态码
 * 3. 日志中可以精确定位问题
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final SnCode snCode;
    private final InventoryStatus currentStatus;
    private final InventoryStatus targetStatus;

    public InvalidStatusTransitionException(
            SnCode snCode,
            InventoryStatus currentStatus,
            InventoryStatus targetStatus) {
        super(String.format(
                "库存项 %s 无法从 [%s] 转换到 [%s]，允许的目标状态: %s",
                snCode, currentStatus, targetStatus, currentStatus.allowedTransitions()));
        this.snCode = snCode;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public SnCode getSnCode() {
        return snCode;
    }

    public InventoryStatus getCurrentStatus() {
        return currentStatus;
    }

    public InventoryStatus getTargetStatus() {
        return targetStatus;
    }
}

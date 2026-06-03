package cn.oa.workflow.core.engine;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 会签汇总结果
 */
@Data
@AllArgsConstructor(staticName = "of")
public class AggregationResult {

    private String action; // APPROVED/REJECTED/WAITING
    private String message;

    public static AggregationResult approved() {
        return of("APPROVED", null);
    }

    public static AggregationResult rejected(String message) {
        return of("REJECTED", message);
    }

    public static AggregationResult waiting() {
        return of("WAITING", null);
    }

    public boolean isApproved() { return "APPROVED".equals(action); }
    public boolean isRejected() { return "REJECTED".equals(action); }
    public boolean isWaiting() { return "WAITING".equals(action); }
}
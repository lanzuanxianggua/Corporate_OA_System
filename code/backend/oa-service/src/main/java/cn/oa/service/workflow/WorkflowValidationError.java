package cn.oa.service.workflow;

/**
 * Structured validation error for configurable workflow definitions.
 */
public class WorkflowValidationError {
    public final String type;
    public final String nodeId;
    public final String message;

    public WorkflowValidationError(String type, String nodeId, String message) {
        this.type = type;
        this.nodeId = nodeId;
        this.message = message;
    }
}

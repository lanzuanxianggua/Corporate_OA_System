package cn.oa.service.workflow;

import cn.hutool.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Parsed graph view of a configurable workflow definition.
 */
public class WorkflowGraph {
    public final int schemaVersion;
    public final Map<String, JSONObject> nodes;
    public final Map<String, List<JSONObject>> outgoing;
    public final List<JSONObject> edges;
    public final List<WorkflowValidationError> errors;
    public final boolean valid;

    public WorkflowGraph(int schemaVersion,
                         Map<String, JSONObject> nodes,
                         Map<String, List<JSONObject>> outgoing,
                         List<JSONObject> edges,
                         List<WorkflowValidationError> errors) {
        this.schemaVersion = schemaVersion;
        this.nodes = nodes;
        this.outgoing = outgoing;
        this.edges = edges;
        this.errors = errors;
        this.valid = errors.isEmpty();
    }

    public boolean isGraph() {
        return schemaVersion >= 2;
    }
}

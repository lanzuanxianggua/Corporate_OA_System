package cn.oa.common.websocket;

/**
 * WebSocket message sender interface.
 * Implemented in oa-web module to decouple from the websocket endpoint.
 */
public interface WebSocketSender {

    /**
     * Send a text message to a specific user identified by empId.
     *
     * @param empId   employee ID
     * @param message JSON string message
     */
    void sendToUser(Long empId, String message);
}

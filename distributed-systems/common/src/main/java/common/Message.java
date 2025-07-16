package common;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String messageId;
    private String correlationId;
    private String type;
    private Map<String, String> data;
    private long timestamp;
    private String senderId;
    
    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }
    
    public Message(String type, Map<String, String> data) {
        this();
        this.type = type;
        this.data = data;
    }
    
    public Message(String type, Map<String, String> data, String correlationId) {
        this(type, data);
        this.correlationId = correlationId;
    }
    
    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
}

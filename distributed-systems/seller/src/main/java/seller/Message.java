package seller;

public class Message {
    public enum Type { RESERVE, CONFIRM, CANCEL }
    
    private Type type;
    private String orderId;
    private String productId;
    private int quantity;
    private String sellerId;
    private String reservationId;
    private boolean success;
    private String reason;
    
    // Konstruktoren
    public Message() {}
    
    // Getter und Setter (identisch mit marketplace.Message)
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

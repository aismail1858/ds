package marketplace;

import java.util.List;

public class Order {
    private String orderId;
    private List<OrderItem> items;
    
    // Getter und Setter
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    // Innere Klasse für Order Items
    public static class OrderItem {
        private String productId;
        private String sellerId;
        private int quantity;
        
        // Konstruktoren
        public OrderItem() {}
        
        public OrderItem(String productId, String sellerId, int quantity) {
            this.productId = productId;
            this.sellerId = sellerId;
            this.quantity = quantity;
        }
        
        // Getter und Setter
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getSellerId() { return sellerId; }
        public void setSellerId(String sellerId) { this.sellerId = sellerId; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}

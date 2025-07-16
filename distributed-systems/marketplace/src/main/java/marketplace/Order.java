package marketplace;

import common.OrderStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Order {
    private final String orderId;
    private final String customerId;
    private final String marketplaceId;
    private final List<OrderItem> items;
    private final AtomicReference<OrderStatus> status;
    private final long createdAt;
    
    public Order(String orderId, String customerId, String marketplaceId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.marketplaceId = marketplaceId;
        this.items = new ArrayList<>();
        this.status = new AtomicReference<>(OrderStatus.CREATED);
        this.createdAt = System.currentTimeMillis();
    }
    
    public void addItem(String productId, int quantity, String sellerId) {
        items.add(new OrderItem(productId, quantity, sellerId));
    }
    
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getMarketplaceId() { return marketplaceId; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); }
    public OrderStatus getStatus() { return status.get(); }
    public void setStatus(OrderStatus newStatus) { status.set(newStatus); }
    public long getCreatedAt() { return createdAt; }
    
    // Innere Klasse für Order Items
    public static class OrderItem {
        private final String productId;
        private final int quantity;
        private final String sellerId;
        
        public OrderItem(String productId, int quantity, String sellerId) {
            this.productId = productId;
            this.quantity = quantity;
            this.sellerId = sellerId;
        }
        
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public String getSellerId() { return sellerId; }
    }
}

package seller;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.HashMap;
import java.util.Properties;

/**
 * Enhanced inventory management with proper concurrency control and reservation timeouts.
 * Prevents race conditions and ensures consistent inventory state across distributed operations.
 */
public class EnhancedInventory {
    private final String sellerId;
    private final Map<String, AtomicInteger> stock;
    private final Map<String, TimedReservation> reservations;
    private final AtomicInteger reservationCounter = new AtomicInteger(0);
    private final ReentrantReadWriteLock inventoryLock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final long reservationTimeoutMs;
    private final int cleanupIntervalSeconds;
    
    /**
     * Creates an enhanced inventory with default settings.
     * @param sellerId The seller identifier
     * @param config Configuration properties
     */
    public EnhancedInventory(String sellerId, Properties config) {
        this.sellerId = sellerId;
        this.stock = new ConcurrentHashMap<>();
        this.reservations = new ConcurrentHashMap<>();
        this.reservationTimeoutMs = Long.parseLong(config.getProperty("reservation.timeout.ms", "300000")); // 5 minutes
        this.cleanupIntervalSeconds = Integer.parseInt(config.getProperty("cleanup.interval.seconds", "60"));
        
        // Initialize stock
        int initialStock = Integer.parseInt(config.getProperty("seller.inventory.size", "50"));
        for (int i = 1; i <= 3; i++) {
            stock.put("P" + i, new AtomicInteger(initialStock));
        }
        
        // Start cleanup task
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredReservations, 
            cleanupIntervalSeconds, 
            cleanupIntervalSeconds, 
            TimeUnit.SECONDS
        );
        
        System.out.println("Enhanced inventory initialized for " + sellerId + 
                         " with " + stock.size() + " products and " + reservationTimeoutMs + "ms reservation timeout");
    }
    
    /**
     * Reserves inventory for a product with atomic operations and timeout.
     * @param productId The product identifier
     * @param quantity The quantity to reserve
     * @return Reservation ID if successful, null if insufficient stock
     */
    public String reserve(String productId, int quantity) {
        if (quantity <= 0) {
            System.out.println("Invalid quantity: " + quantity);
            return null;
        }
        
        Lock writeLock = inventoryLock.writeLock();
        writeLock.lock();
        try {
            // Clean up expired reservations first
            cleanupExpiredReservations();
            
            AtomicInteger available = stock.get(productId);
            if (available == null) {
                System.out.println("Product " + productId + " not found");
                return null;
            }
            
            int currentStock = available.get();
            if (currentStock >= quantity) {
                // Reduce stock atomically
                int newStock = currentStock - quantity;
                if (available.compareAndSet(currentStock, newStock)) {
                    // Create reservation
                    String reservationId = sellerId + "-R" + reservationCounter.incrementAndGet();
                    long expiryTime = System.currentTimeMillis() + reservationTimeoutMs;
                    TimedReservation reservation = new TimedReservation(
                        reservationId, productId, quantity, expiryTime);
                    reservations.put(reservationId, reservation);
                    
                    System.out.println("Reserved " + quantity + "x " + productId + 
                                     " (ID: " + reservationId + ") - remaining stock: " + newStock);
                    return reservationId;
                } else {
                    // Stock changed between check and update, retry
                    System.out.println("Stock changed during reservation, retrying...");
                    return reserve(productId, quantity);
                }
            }
            
            System.out.println("Insufficient stock for " + productId + ": " + currentStock + " < " + quantity);
            return null;
            
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Confirms a reservation, making it permanent.
     * @param reservationId The reservation identifier
     * @return true if confirmation was successful
     */
    public boolean confirm(String reservationId) {
        Lock writeLock = inventoryLock.writeLock();
        writeLock.lock();
        try {
            TimedReservation reservation = reservations.get(reservationId);
            if (reservation != null && !reservation.isExpired() && !reservation.isConfirmed()) {
                reservation.setConfirmed(true);
                System.out.println("Confirmed reservation: " + reservationId);
                return true;
            }
            
            if (reservation == null) {
                System.out.println("Reservation not found: " + reservationId);
            } else if (reservation.isExpired()) {
                System.out.println("Reservation expired: " + reservationId);
            } else if (reservation.isConfirmed()) {
                System.out.println("Reservation already confirmed: " + reservationId);
            }
            
            return false;
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Cancels a reservation and returns stock to inventory.
     * @param reservationId The reservation identifier
     * @return true if cancellation was successful
     */
    public boolean cancel(String reservationId) {
        Lock writeLock = inventoryLock.writeLock();
        writeLock.lock();
        try {
            TimedReservation reservation = reservations.remove(reservationId);
            if (reservation != null && !reservation.isConfirmed()) {
                // Return stock to inventory
                AtomicInteger available = stock.get(reservation.getProductId());
                if (available != null) {
                    int newStock = available.addAndGet(reservation.getQuantity());
                    System.out.println("Cancelled reservation: " + reservationId + 
                                     " - returned " + reservation.getQuantity() + "x " + 
                                     reservation.getProductId() + " - new stock: " + newStock);
                }
                return true;
            }
            
            if (reservation == null) {
                System.out.println("Reservation not found for cancellation: " + reservationId);
            } else if (reservation.isConfirmed()) {
                System.out.println("Cannot cancel confirmed reservation: " + reservationId);
            }
            
            return false;
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Gets current inventory status.
     * @return Map of product IDs to available quantities
     */
    public Map<String, Integer> getInventoryStatus() {
        Lock readLock = inventoryLock.readLock();
        readLock.lock();
        try {
            Map<String, Integer> status = new HashMap<>();
            for (Map.Entry<String, AtomicInteger> entry : stock.entrySet()) {
                status.put(entry.getKey(), entry.getValue().get());
            }
            return status;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Gets current reservation status.
     * @return Map of reservation information
     */
    public Map<String, Object> getReservationStatus() {
        Lock readLock = inventoryLock.readLock();
        readLock.lock();
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("totalReservations", reservations.size());
            
            int activeReservations = 0;
            int expiredReservations = 0;
            int confirmedReservations = 0;
            
            for (TimedReservation reservation : reservations.values()) {
                if (reservation.isConfirmed()) {
                    confirmedReservations++;
                } else if (reservation.isExpired()) {
                    expiredReservations++;
                } else {
                    activeReservations++;
                }
            }
            
            status.put("activeReservations", activeReservations);
            status.put("expiredReservations", expiredReservations);
            status.put("confirmedReservations", confirmedReservations);
            
            return status;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     * @return String representation of inventory status
     */
    public String getStatus() {
        Map<String, Integer> status = getInventoryStatus();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : status.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
        }
        return sb.toString().trim();
    }
    
    /**
     * Cleans up expired reservations and returns stock to inventory.
     */
    private void cleanupExpiredReservations() {
        Lock writeLock = inventoryLock.writeLock();
        writeLock.lock();
        try {
            Iterator<Map.Entry<String, TimedReservation>> iterator = reservations.entrySet().iterator();
            int cleanedCount = 0;
            
            while (iterator.hasNext()) {
                Map.Entry<String, TimedReservation> entry = iterator.next();
                TimedReservation reservation = entry.getValue();
                
                if (reservation.isExpired() && !reservation.isConfirmed()) {
                    // Return stock to inventory
                    AtomicInteger available = stock.get(reservation.getProductId());
                    if (available != null) {
                        available.addAndGet(reservation.getQuantity());
                    }
                    iterator.remove();
                    cleanedCount++;
                }
            }
            
            if (cleanedCount > 0) {
                System.out.println("Cleaned up " + cleanedCount + " expired reservations");
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Shuts down the inventory cleanup executor.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            // Final cleanup before shutdown
            cleanupExpiredReservations();
            
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Enhanced inventory shut down for " + sellerId);
    }
    
    /**
     * Represents a timed reservation with expiry and confirmation state.
     */
    private static class TimedReservation {
        private final String id;
        private final String productId;
        private final int quantity;
        private final long expiryTime;
        private volatile boolean confirmed = false;
        
        public TimedReservation(String id, String productId, int quantity, long expiryTime) {
            this.id = id;
            this.productId = productId;
            this.quantity = quantity;
            this.expiryTime = expiryTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public long getExpiryTime() { return expiryTime; }
        public boolean isConfirmed() { return confirmed; }
        public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
        
        @Override
        public String toString() {
            return String.format("TimedReservation{id='%s', productId='%s', quantity=%d, confirmed=%s, expired=%s}", 
                               id, productId, quantity, confirmed, isExpired());
        }
    }
}
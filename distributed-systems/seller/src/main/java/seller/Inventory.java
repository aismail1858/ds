package seller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Inventory {
    private final String sellerId;
    private final Map<String, AtomicInteger> stock;
    private final Map<String, Reservation> reservations;
    private int reservationCounter = 0;

    public Inventory(String sellerId, java.util.Properties config) {
        this.sellerId = sellerId;
        this.stock = new ConcurrentHashMap<>();
        this.reservations = new ConcurrentHashMap<>();
        int initialStock = Integer.parseInt(config.getProperty("seller.inventory.size", "50"));
        // Initialisiere Lagerbestand für einige Produkte
        stock.put("P1", new AtomicInteger(initialStock));
        stock.put("P2", new AtomicInteger(initialStock));
        stock.put("P3", new AtomicInteger(initialStock));
    }

    /**
     * Versucht, eine bestimmte Menge eines Produkts zu reservieren.
     * Gibt die Reservierungs-ID zurück, wenn erfolgreich, sonst null.
     */
    public synchronized String reserve(String productId, int quantity) {
        AtomicInteger available = stock.get(productId);
        
        if (available == null) {
            System.out.println("Product " + productId + " not found");
            return null;
        }
        
        int currentStock = available.get();
        if (currentStock >= quantity) {
            // Reduziere Bestand
            available.addAndGet(-quantity);
            
            // Erstelle Reservierung
            String reservationId = sellerId + "-R" + (++reservationCounter);
            Reservation reservation = new Reservation(reservationId, productId, quantity);
            reservations.put(reservationId, reservation);
            
            return reservationId;
        }
        
        System.out.println("Insufficient stock: " + currentStock + " < " + quantity);
        return null;
    }
    
    /**
     * Bestätigt eine Reservierung (macht sie permanent)
     */
    public synchronized boolean confirm(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation != null && !reservation.isConfirmed()) {
            reservation.setConfirmed(true);
            return true;
        }
        return false;
    }
    
    /**
     * Storniert eine Reservierung und gibt den Bestand zurück.
     */
    public synchronized boolean cancel(String reservationId) {
        Reservation reservation = reservations.remove(reservationId);
        if (reservation != null && !reservation.isConfirmed()) {
            // Bestand zurückgeben, wenn noch nicht bestätigt
            AtomicInteger available = stock.get(reservation.getProductId());
            if (available != null) {
                available.addAndGet(reservation.getQuantity());
            }
            return true;
        }
        return false;
    }
    
    /**
     * Gibt den aktuellen Lagerbestand zurück.
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, AtomicInteger> entry : stock.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue().get()).append(" ");
        }
        return sb.toString().trim();
    }
    
    // Innere Klasse für Reservierungen
    private static class Reservation {
        private final String id;
        private final String productId;
        private final int quantity;
        private boolean confirmed;
        
        public Reservation(String id, String productId, int quantity) {
            this.id = id;
            this.productId = productId;
            this.quantity = quantity;
            this.confirmed = false;
        }
        
        public String getId() { return id; }
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public boolean isConfirmed() { return confirmed; }
        public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    }
}

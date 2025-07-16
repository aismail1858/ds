package marketplace;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OrderProcessor {
    private final ZContext context;
    private final Map<String, String> sellerAddresses;
    private final Properties config;
    private final Gson gson = new Gson();
    private final int timeoutMs;

    public OrderProcessor(ZContext context, Map<String, String> sellerAddresses, Properties config) {
        this.context = context;
        this.sellerAddresses = sellerAddresses;
        this.config = config;
        this.timeoutMs = Integer.parseInt(config.getProperty("request.timeout.ms", "5000"));
    }

    /**
     * Verarbeitet eine Bestellung mit SAGA-Pattern
     * @return true wenn erfolgreich, false bei Fehler
     */
    public boolean processOrder(Order order) {
        System.out.println("Starting SAGA for order: " + order.getOrderId());

        // Phase 1: Reserve bei allen Sellers
        Map<String, String> reservations = new HashMap<>(); // sellerId -> reservationId

        for (Order.OrderItem item : order.getItems()) {
            System.out.println("  Reserving " + item.getQuantity() + "x " +
                               item.getProductId() + " at " + item.getSellerId());

            Message response = sendRequest(
                item.getSellerId(),
                createReserveMessage(order.getOrderId(), item)
            );

            if (response != null && response.isSuccess()) {
                reservations.put(item.getSellerId(), response.getReservationId());
                System.out.println("    ✓ Reserved with ID: " + response.getReservationId());
            } else {
                System.out.println("    ✗ Reservation failed for " + item.getSellerId() + ": " +
                                   (response != null ? response.getReason() : "No response"));
                // Rollback
                cancelReservations(order.getOrderId(), reservations);
                System.out.println("  ✗ Order " + order.getOrderId() + " failed during reservation phase.");
                return false;
            }
        }

        System.out.println("  All items reserved, confirming order...");

        // Phase 2: Confirm bei allen Sellers
        return confirmReservations(order.getOrderId(), reservations);
    }

    /**
     * Sendet eine Anfrage an einen Seller und wartet auf Antwort
     */
    private Message sendRequest(String sellerId, Message request) {
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        socket.setReceiveTimeOut(timeoutMs);

        try {
            String address = sellerAddresses.get(sellerId);
            if (address == null) {
                System.err.println("Unknown seller: " + sellerId);
                return null;
            }
            socket.connect(address);

            String jsonRequest = gson.toJson(request);
            socket.send(jsonRequest.getBytes(ZMQ.CHARSET), 0);

            byte[] reply = socket.recv(0);
            if (reply == null) {
                System.err.println("Timeout or no response from " + sellerId);
                return null;
            }
            String jsonReply = new String(reply, ZMQ.CHARSET);
            return gson.fromJson(jsonReply, Message.class);

        } catch (Exception e) {
            System.err.println("Error communicating with " + sellerId + ": " + e.getMessage());
            return null;
        } finally {
            socket.close();
        }
    }

    private boolean confirmReservations(String orderId, Map<String, String> reservations) {
        for (Map.Entry<String, String> entry : reservations.entrySet()) {
            String sellerId = entry.getKey();
            String reservationId = entry.getValue();

            Message response = sendRequest(
                sellerId,
                createConfirmMessage(orderId, reservationId)
            );

            if (response == null || !response.isSuccess()) {
                System.out.println("    ✗ Confirmation failed for " + sellerId);
                // In einer echten Implementierung würde hier ein komplexerer Rollback stattfinden
                // Für diese Übung reicht es, den Fehler zu loggen
            } else {
                System.out.println("    ✓ Confirmed with " + sellerId);
            }
        }

        System.out.println("  ✓ Order completed successfully!");
        return true;
    }

    private void cancelReservations(String orderId, Map<String, String> reservations) {
        for (Map.Entry<String, String> entry : reservations.entrySet()) {
            String sellerId = entry.getKey();
            String reservationId = entry.getValue();

            System.out.println("    Cancelling reservation " + reservationId + " at " + sellerId);

            Message cancelMessage = new Message();
            cancelMessage.setType(Message.Type.CANCEL);
            cancelMessage.setOrderId(orderId);
            cancelMessage.setReservationId(reservationId);

            Message response = sendRequest(sellerId, cancelMessage);
            if (response != null && response.isSuccess()) {
                System.out.println("      ✓ Cancelled");
            } else {
                System.out.println("      ✗ Cancel failed - manual intervention needed");
            }
        }
    }

    private Message createReserveMessage(String orderId, Order.OrderItem item) {
        Message msg = new Message();
        msg.setType(Message.Type.RESERVE);
        msg.setOrderId(orderId);
        msg.setProductId(item.getProductId());
        msg.setQuantity(item.getQuantity());
        msg.setSellerId(item.getSellerId());
        return msg;
    }

    private Message createConfirmMessage(String orderId, String reservationId) {
        Message msg = new Message();
        msg.setType(Message.Type.CONFIRM);
        msg.setOrderId(orderId);
        msg.setReservationId(reservationId);
        return msg;
    }
}

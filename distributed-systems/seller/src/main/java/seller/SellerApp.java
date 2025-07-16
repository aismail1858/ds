package seller;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SellerApp {
    private static final String CONFIG_FILE = "config.properties";
    
    private String sellerId;
    private int port;
    private Inventory inventory;
    private FailureSimulator failureSimulator;
    private final Gson gson = new Gson();
    
    public SellerApp() {
        this.sellerId = System.getenv().getOrDefault("SELLER_ID", "defaultSeller");
        this.port = Integer.parseInt(System.getenv().getOrDefault("SELLER_PORT", "6000"));
        Properties config = loadConfig();
        this.inventory = new Inventory(sellerId, config);
        this.failureSimulator = new FailureSimulator(config);
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Seller...");
        new SellerApp().run();
    }
    
    public void run() {
        System.out.println("Seller " + sellerId + " starting on port " + port + "...");
        System.out.println("Initial inventory: " + inventory.getStatus());
        
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://*:" + port);
            
            while (!Thread.currentThread().isInterrupted()) {
                // Warte auf Request
                byte[] request = socket.recv(0);
                if (request != null) {
                    String jsonRequest = new String(request, ZMQ.CHARSET);
                    System.out.println("\nReceived request: " + jsonRequest);
                    
                    String jsonResponse = processRequest(jsonRequest);
                    socket.send(jsonResponse.getBytes(ZMQ.CHARSET), 0);
                }
            }
        } catch (Exception e) {
            System.err.println("Seller " + sellerId + " crashed: " + e.getMessage());
        }
    }
    
    private String processRequest(String jsonRequest) {
        // Simulate no response (crash)
        if (failureSimulator.shouldSimulateNoResponse()) {
            System.out.println("Simulating no response (crash)...");
            // Wir müssen trotzdem antworten wegen REQ/REP Pattern
            // In echter Implementierung würde hier der Socket hängen
            Message response = new Message();
            response.setSuccess(false);
            response.setReason("Simulated crash");
            return gson.toJson(response);
        }
        
        Message request = gson.fromJson(jsonRequest, Message.class);
        
        // Simulate processing delay
        try {
            Thread.sleep(Integer.parseInt(loadConfig().getProperty("seller.processing.delay.ms", "200")));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate processing failure
        if (failureSimulator.shouldSimulateProcessingFailure()) {
            System.out.println("Simulating processing failure...");
            Message response = new Message();
            response.setSuccess(false);
            response.setReason("Simulated processing failure");
            return gson.toJson(response);
        }
        
        // Verarbeite basierend auf Message Type
        Message response = null;
        
        switch (request.getType()) {
            case RESERVE:
                response = handleReserve(request);
                break;
            case CONFIRM:
                response = handleConfirm(request);
                break;
            case CANCEL:
                response = handleCancel(request);
                break;
            default:
                response = createErrorResponse("Unknown message type");
        }
        
        return gson.toJson(response);
    }
    
    private Message handleReserve(Message request) {
        Message response = new Message();
        response.setType(Message.Type.RESERVE);
        response.setOrderId(request.getOrderId());
        response.setProductId(request.getProductId());
        response.setQuantity(request.getQuantity());
        response.setSellerId(sellerId);
        
        // Simulate out of stock
        if (failureSimulator.shouldSimulateOutOfStock()) {
            System.out.println("Simulating out of stock...");
            response.setSuccess(false);
            response.setReason("Simulated out of stock");
            return response;
        }
        
        String reservationId = inventory.reserve(request.getProductId(), request.getQuantity());
        
        if (reservationId != null) {
            response.setSuccess(true);
            response.setReservationId(reservationId);
            System.out.println("Reserved: " + request.getQuantity() + "x " +
                               request.getProductId() + " (ID: " + reservationId + ")");
        } else {
            response.setSuccess(false);
            response.setReason("Insufficient stock");
            System.out.println("Reservation failed: Insufficient stock");
        }
        
        return response;
    }
    
    private Message handleConfirm(Message request) {
        Message response = new Message();
        response.setType(Message.Type.CONFIRM);
        response.setOrderId(request.getOrderId());
        
        boolean confirmed = inventory.confirm(request.getReservationId());
        response.setSuccess(confirmed);
        
        if (confirmed) {
            System.out.println("Confirmed reservation: " + request.getReservationId());
        } else {
            response.setReason("Reservation not found or already confirmed");
            System.out.println("Confirmation failed: " + response.getReason());
        }
        
        return response;
    }
    
    private Message handleCancel(Message request) {
        Message response = new Message();
        response.setType(Message.Type.CANCEL);
        response.setOrderId(request.getOrderId());
        
        boolean cancelled = inventory.cancel(request.getReservationId());
        response.setSuccess(cancelled);
        
        if (cancelled) {
            System.out.println("Cancelled reservation: " + request.getReservationId());
        } else {
            response.setReason("Reservation not found or already cancelled/confirmed");
            System.out.println("Cancellation failed: " + response.getReason());
        }
        
        return response;
    }
    
    private Message createErrorResponse(String reason) {
        Message response = new Message();
        response.setSuccess(false);
        response.setReason(reason);
        return response;
    }
    
    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(CONFIG_FILE)) {
            props.load(is);
        } catch (IOException e) {
            System.err.println("Could not load config, using defaults: " + e.getMessage());
            // Defaults
            props.setProperty("seller.inventory.size", "100");
            props.setProperty("seller.processing.delay.ms", "200");
            props.setProperty("failure.no.response", "0.05");
            props.setProperty("failure.processing", "0.10");
        }
        return props;
    }
}
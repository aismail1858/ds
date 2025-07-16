package marketplace;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MarketplaceApp {
    private static final String CONFIG_FILE = "config.properties";
    private static final String ORDERS_FILE = "orders.json";
    
    private String marketplaceId;
    private Properties config;
    private final Gson gson = new Gson();
    private final Map<String, String> sellerAddresses;
    private ZContext context;
    
    public MarketplaceApp() {
        this.marketplaceId = System.getenv().getOrDefault("MARKETPLACE_ID", "MP1");
        this.config = loadConfig();
        this.sellerAddresses = initSellerAddresses();
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Marketplace...");
        new MarketplaceApp().run();
    }
    
    public void run() {
        System.out.println("Marketplace " + marketplaceId + " starting...");
        
        context = new ZContext();
        List<Order> orders = loadOrders();
        OrderProcessor processor = new OrderProcessor(context, sellerAddresses, config);
        
        // Statistiken
        int successCount = 0;
        int failureCount = 0;
        
        // Verarbeite Orders mit konfigurierbarem Delay
        int orderDelay = Integer.parseInt(config.getProperty("order.delay.ms", "1000"));
        
        for (Order order : orders) {
            System.out.println("\n=== Processing Order: " + order.getOrderId() + " ===");
            
            try {
                boolean success = processor.processOrder(order);
                
                if (success) {
                    successCount++;
                    System.out.println("✓ Order " + order.getOrderId() + " completed successfully!");
                } else {
                    failureCount++;
                    System.out.println("✗ Order " + order.getOrderId() + " failed!");
                }
                
                Thread.sleep(orderDelay);
                
            } catch (Exception e) {
                System.err.println("Error processing order " + order.getOrderId() + ": " + e.getMessage());
                failureCount++;
            }
        }
        
        System.out.println("\n=== Processing Summary ===");
        System.out.println("Total orders: " + orders.size());
        System.out.println("Successful: " + successCount);
        System.out.println("Failed: " + failureCount);
        System.out.println("Success rate: " + (100.0 * successCount / orders.size()) + "%");
        
        // Cleanup
        context.destroy();
    }
    
    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(CONFIG_FILE)) {
            props.load(is);
        } catch (IOException e) {
            System.err.println("Could not load config, using defaults: " + e.getMessage());
            // Defaults
            props.setProperty("order.delay.ms", "1000");
            props.setProperty("request.timeout.ms", "5000");
        }
        return props;
    }
    
    private List<Order> loadOrders() {
        try (Reader reader = new FileReader(ORDERS_FILE)) {
            return gson.fromJson(reader, new TypeToken<List<Order>>(){}.getType());
        } catch (IOException e) {
            System.err.println("Could not load orders, generating test orders: " + e.getMessage());
            return generateTestOrders();
        }
    }
    
    private List<Order> generateTestOrders() {
        List<Order> orders = new ArrayList<>();
        
        Order order1 = new Order();
        order1.setOrderId("TEST-001");
        order1.setItems(List.of(
            new Order.OrderItem("P1", "seller1", 5),
            new Order.OrderItem("P2", "seller2", 3)
        ));
        orders.add(order1);
        
        Order order2 = new Order();
        order2.setOrderId("TEST-002");
        order2.setItems(List.of(
            new Order.OrderItem("P1", "seller1", 10),
            new Order.OrderItem("P3", "seller3", 2),
            new Order.OrderItem("P2", "seller4", 7)
        ));
        orders.add(order2);
        
        return orders;
    }
    
    private Map<String, String> initSellerAddresses() {
        Map<String, String> addresses = new HashMap<>();
        // Docker-Compose Service-Name ist Hostname
        addresses.put("seller1", "tcp://seller1:6001");
        addresses.put("seller2", "tcp://seller2:6002");
        addresses.put("seller3", "tcp://seller3:6003");
        addresses.put("seller4", "tcp://seller4:6004");
        addresses.put("seller5", "tcp://seller5:6005");
        return addresses;
    }
}
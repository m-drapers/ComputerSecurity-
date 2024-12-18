import java.io.*;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;



public class Server {
    private SSLServerSocket serverSocket;
    // Solve thread vulnerability
    private static ThreadPoolExecutor threadPool;
    private static final int maxThreads = 10;


    private Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    private static final DateTimeFormatter Formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Method to load environment variables from .env file
        public static void loadEnv() {
            try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2 && parts[0].startsWith("SERVER_")) {
                        // Set the property in the system
                        System.setProperty(parts[0], parts[1]);

                    }
                }
            } catch (IOException e) {
                 System.err.println("Error loading environment variables");
            }
        }

    public void start(int port) {
        try {
            // Load the server keystore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream keyStoreStream = new FileInputStream("server.keystore")) {
                char[] keyStorePassword = System.getProperty("SERVER_KEYSTORE_PASSWORD").toCharArray();
                keyStore.load(keyStoreStream, keyStorePassword);
            }

            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            char[] trustStorePassword = System.getProperty("SERVER_TRUSTSTORE_PASSWORD").toCharArray();
            keyManagerFactory.init(keyStore, trustStorePassword);

            // Initialize SSL context
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

            // Create SSL server socket
            SSLServerSocketFactory ServerSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) ServerSocketFactory.createServerSocket(port);

            // Solved thread vulnerability: Initialize the thread pool with a fixed size
            threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
    
            System.out.println("Server started on port " + port);
            
            while (true){
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket));

                if (threadPool.getActiveCount() >= maxThreads) {
                    System.out.println("Max threads reached. Rejecting connection.");
                    try (OutputStream os = clientSocket.getOutputStream();
                        PrintWriter writer = new PrintWriter(os, true)) {
                        writer.println("Server is busy. Please try again later.");
                    }
                    clientSocket.close(); 
                }
            }
        } catch (IOException e) {
            System.err.println("Unable to connect to the server."); // Please check IP address
        } catch (Exception e) {
            System.err.println("Unable to connect to the server.");
        }
    }

    public void stop() {
        try {
            serverSocket.close();
            if (threadPool != null) {
                threadPool.shutdown();
            }
        } catch (IOException e) {
            System.err.println("An error occurred while stopping the server.");
        } finally {
            System.clearProperty("SERVER_TRUSTORE_PASSWORD");
            System.clearProperty("SERVER_KEYSTORE_PASSWORD");
        }
    }
    private class ClientInfo {
        String id;
        String password;
        int counter;
        int instancesCount;

        ClientInfo(String id, String password, int counter, int instancesCount) {
            this.id = id;
            this.password = password;
            this.counter = counter;
            this.instancesCount = 1;
        }
    }

    private class ClientHandler extends Thread {
        private SSLSocket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;

        public ClientHandler(SSLSocket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            AtomicReference<ScheduledExecutorService> inactivityScheduler = new AtomicReference<>(Executors.newSingleThreadScheduledExecutor());
            ScheduledExecutorService sessionTimeoutScheduler = Executors.newSingleThreadScheduledExecutor();
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Handle client inactivity
                Runnable inactivityTask = () -> {
                    out.println("Session closed due to inactivity for the client: " + clientId);
                    handleLogout(out);
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client socket due to inactivity.");
                    }
                    inactivityScheduler.get().shutdown();
                };

                String message;
                while ((message = in.readLine()) != "LOGOUT") {

                    inactivityScheduler.get().shutdownNow();
                    inactivityScheduler.set(Executors.newSingleThreadScheduledExecutor());
                    inactivityScheduler.get().schedule(inactivityTask, 5, TimeUnit.MINUTES);

                    String[] parts = message.split(" ");
                    String command = parts[0];

                    switch (command) {
                        case "REGISTER":
                            handleRegister(out, parts);
                            break;
                        case "INCREASE":
                        case "DECREASE":
                            handleCounterOperation(out, command, parts);
                            break;
                        case "LOGOUT":
                            handleLogout(out);
                            inactivityScheduler.get().shutdown();
                            sessionTimeoutScheduler.shutdown();
                            return;
                        default:
                            out.println("ERROR: Unknown command.");
                            break;
                    }
                }
            } catch (SocketException e) {
                System.err.println("Client disconnected abruptly: " + clientId);
                handleLogout(out); // Treat as a "LOGOUT"
            } catch (IOException e) {
                System.err.println("Error occurred while handling client.");
            } finally {
                try {
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket");
                }
                inactivityScheduler.get().shutdown();
                sessionTimeoutScheduler.shutdown();
            }
        }

        private void handleRegister(PrintWriter out, String[] parts) {
            if (parts.length < 3) {
                out.println("ERROR: Invalid registration format.");
                return;
            }

            clientId = parts[1];
            String password = parts[2];

            // Check if the client ID is already registered
            if (clients.containsKey(clientId)) {
                ClientInfo existingClient = clients.get(clientId);

                // Verify if the password matches
                if (existingClient.password.equals(password)) {
                    existingClient.instancesCount += 1;

                    if (existingClient.instancesCount >= 4) {
                        System.out.println("ERROR: Maximum concurrent logins reached for client " + clientId);
                        out.println("Use one of your open sessions. ");
                        clientId = null;
                        existingClient.instancesCount -= 1; // Decrement instance count
                        try {
                            in.close();
                            clientSocket.close();
                        } catch (IOException e) {
                            System.err.println("Error closing client socket after rejecting login.");
                        }
                    } else {
                        out.println("ACK: Login successful.");
                    }

                } else {
                    System.out.println("ERROR: ID already in use with a different password.");
                }
            } else {
                // Register new client if ID is not in use
                clients.put(clientId, new ClientInfo(clientId, password, 0, 1));
                out.println("ACK: Registration successful.");
            }
        }

        private void handleCounterOperation(PrintWriter out, String command, String[] parts) {
            if (clientId == null) {
                out.println("ERROR: Client not registered.");
                return;
            }
    
            int amount;
            // Check for strings
            try{
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                out.println("ERROR: Invalid format.");
                return;
            }
            // Check for overflow + no negative numbeers
            if (amount < 0 || amount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid Amount");
            }
   
            ClientInfo clientInfo = clients.get(clientId);
            if (command.equals("INCREASE")) {
                clientInfo.counter += amount;
            } else { // DECREASE
                clientInfo.counter -= amount;
            }
            out.println("Counter " + command.toLowerCase()+"d to " + clientInfo.counter);

            generatelogfile(clientId, command, amount);
        }

        private void handleLogout(PrintWriter out) {
            try{
                if (clientId != null) {
                    ClientInfo clientInfo = clients.get(clientId);

                    if (clientInfo != null) {
                        clientInfo.instancesCount -= 1; // Decrement instance count

                        if (clientInfo.instancesCount <= 0) {
                            // If no more instances, remove from clients map and delete JSON file
                            clients.remove(clientId);
                            System.out.println("Client information deleted");
                            out.println("Session successfully terminated.");
                        } else {
                            out.println("ACK: Logout successful, remaining instances: " + clientInfo.instancesCount);
                        }
                    }
                    clientId = null;
                    try {
                        in.close();
                        clientSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client socket during logout.");
                    }
                }
            }
            catch(NullPointerException e)
            {
                System.err.println("Error: NullPointerException occurred during logout.");
            }
        }

    }


    private static void generatelogfile(String clientId, String action, int amount){
        final String LOG_FILE = "logfile.JSON"; 

        // Create JSON object
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("timestamp",LocalDateTime.now().format(Formatter));
        logEntry.put("id", clientId);
        logEntry.put("action", action);
        logEntry.put("amount", amount);

        // Convert Map to JSON String
        String jsonBuilder = mapToJsonString(logEntry);

        try (FileWriter fileWriter = new FileWriter(LOG_FILE, true)) {
            fileWriter.write(jsonBuilder + "\n");    

        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }

    }

    // Method to convert a Map to JSON-like string
    public static String mapToJsonString(Map<String, Object> map) {
        StringBuilder jsonBuilder = new StringBuilder("{");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            jsonBuilder.append("\"").append(entry.getKey()).append("\": ")
                       .append("\"").append(entry.getValue()).append("\", ");
        }
        jsonBuilder.delete(jsonBuilder.length() - 2, jsonBuilder.length()); // remove trailing comma
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    public static void main(String args[]) {
        // Load environment variables from .env file
        loadEnv();
        Server server = new Server();
        server.start(5001);
    }
}
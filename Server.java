import java.net.*;
import java.io.*;
import netscape.javascript.JSObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {
    private ServerSocket serverSocket;
    private Map<String, ClientInfo> clients = new HashMap<>();
    private static final DateTimeFormatter Formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            while (true)
                new ClientHandler(serverSocket.accept()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientInfo {
        String id;
        String password;
        int counter;

        ClientInfo(String id, String password, int counter) {
            this.id = id;
            this.password = password;
            this.counter = counter;
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(" ");
                    String command = parts[0];

                    switch (command) {
                        case "REGISTER":
                            handleRegister(out, parts);
                            break;
                        case "INCREASE":
                            handleIncrease(out, Integer.parseInt(parts[1]));
                            generatelogfile(clientId, command, Integer.parseInt(parts[1]));
                            break;
                        case "DECREASE":
                            handleDecrease(out, Integer.parseInt(parts[1]));
                            generatelogfile(clientId, command, Integer.parseInt(parts[1]));
                            break;
                        case "LOGOUT":
                            handleLogout(out);
                            break;
                        default:
                            out.println("ERROR: Unknown command.");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleLogout(out);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRegister(PrintWriter out, String[] parts) {
            if (parts.length < 3) {
                out.println("ERROR: Invalid registration format.");
                return;
            }

            clientId = parts[1];
            String password = parts[2];

            if (clients.containsKey(clientId)) {
                out.println("ERROR: ID already registered.");
            } else {
                clients.put(clientId, new ClientInfo(clientId, password, 0));
                out.println("ACK: Registration successful.");
            }
        }

        private void handleIncrease(PrintWriter out, int amount) {
            ClientInfo clientInfo = clients.get(clientId);
            if (clientInfo != null) {
                clientInfo.counter += amount;
                out.println("Counter increased to " + clientInfo.counter);
            } else {
                out.println("ERROR: Client not registered.");
            }
        }

        private void handleDecrease(PrintWriter out, int amount) {
            ClientInfo clientInfo = clients.get(clientId);
            if (clientInfo != null) {
                clientInfo.counter -= amount;
                out.println("Counter decreased to " + clientInfo.counter);
            } else {
                out.println("ERROR: Client not registered.");
            }
        }

        private void handleLogout(PrintWriter out) {
            if (clientId != null) {
                clients.remove(clientId);
                if (out != null) {
                    out.println("ACK: Logout successful.");
                }
            }
        }
    }

    private static void generatelogfile(String clientId, String action, int amount){
        final String LOG_FILE = "logfile.JSON"; 

        //Create JSON object
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("timestamp",LocalDateTime.now().format(Formatter));
        logEntry.put("id", clientId);
        logEntry.put("action", action);
        logEntry.put("amount", amount);

        // Convert Map to JSON String
        String jsonBuilder = mapToJsonString(logEntry);
        //System.out.println(jsonString);

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
        //generatelogfile("1", "increase", 600);
        Server server = new Server();
        server.start(5000);
    }
}


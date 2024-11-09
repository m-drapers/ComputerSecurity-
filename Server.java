import java.net.*;
import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
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
        int instancesCount;
        int counter;

        ClientInfo(String id, String password) {
            this.id = id;
            this.password = password;
            this.instancesCount = 1;
            this.counter = 0;
        }
    }

    private class ClientHandler extends Thread { // Each client connection is handled in a separate thread
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;
        private String password;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            String filePath = clientId + ".json";
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(" ");
                    String command = parts[0];

                    switch (command) {
                        case "REGISTER":
                            handleRegister(out, parts, filePath);
                            break;
                        case "INCREASE":
                        case "DECREASE":
                            handleCounterOperation(out, command, parts, filePath);
                            break;
                        case "LOGOUT":
                            handleLogout(out, filePath);
                            break;
                        default:
                            out.println("ERROR: Unknown command.");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleLogout(out, filePath);
                try {
                    clientSocket.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleRegister(PrintWriter out, String[] parts, String filePath) {
            if (parts.length < 3) {
                out.println("ERROR: Invalid registration format.");
                return;
            }

            String clientId = parts[1];
            String password = parts[2];

            if (clients.containsKey(clientId)) {
                // Check password against the stored password in the JSON file
                try {
                    String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
                    JSONObject clientJson = new JSONObject(jsonContent);
                    String storedPassword = clientJson.getString("password");

                    if (!storedPassword.equals(password)) {
                        out.println("ERROR: ID already registered with a different password.");
                        return;
                    } else{
                        clients.get(clientId).instancesCount++;
                        return;
                    }
                } catch (IOException e) {
                    out.println("ERROR: Failed to access client data.");
                    return;
                }
            } else {
                // Register the new client
                clients.put(clientId, new ClientInfo(clientId, password));
                out.println("ACK: Registration successful.");
            }
        }

        private void handleCounterOperation(PrintWriter out, String command, String[] parts, String filePath) throws IOException {
            if (clientId == null) {
                out.println("ERROR: Client not registered.");
                return;
            }
    
            int amount = Integer.parseInt(parts[1]);
            if (amount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
   
            ClientInfo clientInfo = clients.get(clientId);
            if (command.equals("INCREASE")) {
                clientInfo.counter += amount;
                addStep(filePath, command + " " + amount);
            } else { // DECREASE
                clientInfo.counter -= amount;
                addStep(filePath, command + " " + amount);
            }
            generatelogfile(clientId, command, amount);
        }

        private void handleLogout(PrintWriter out, String filePath) {
            if (clientId != null) {
                ClientInfo clientInfo = clients.get(clientId);

                if (clientInfo != null) {
                    clientInfo.instancesCount--; // Decrement instance count

                    if (clientInfo.instancesCount <= 0) {
                        // If no more instances, remove from clients map and delete JSON file
                        clients.remove(clientId);
                        try {
                            // Delete the client's JSON file
                            Path path = Paths.get(filePath);
                            Files.delete(path);
                            out.println("ACK: Logout successful.");
                        } catch (IOException e) {
                            System.err.println("Failed to delete the file " + filePath + ": " + e.getMessage());
                        }
                    } else {
                        out.println("ACK: Logout successful, remaining instances: " + clientInfo.instancesCount);
                    }
                }
                clientId = null;
            }
        }

        private static void addStep(String filePath, String command) {
            try {
                // Read the content of the JSON file
                String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));

                JSONObject clientJson = new JSONObject(jsonContent);
                JSONObject actionsJson = clientJson.getJSONObject("actions");

                // Add a new step to the "steps" array
                JSONArray stepsArray = actionsJson.getJSONArray("steps");
                stepsArray.put(command); 

                try (FileWriter fileWriter = new FileWriter(filePath)) {
                    fileWriter.write(clientJson.toString(4)); 
                }

            } catch (IOException e) {
                System.err.println("Error reading or writing to client file: " + e.getMessage());
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


import java.net.*;
import java.io.*;
import netscape.javascript.JSObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.nio.*;
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
                            handleRegister(out, parts);
                            break;
                        case "INCREASE":
                            int increaseAmount = Integer.parseInt(parts[1]);
                            if (increaseAmount < 0) {
                                throw new IllegalArgumentException("Amount cannot be negative");
                            } else {
                                handleIncrease(out, increaseAmount);
                                addStep(filePath, command);
                                generatelogfile(clientId, command, increaseAmount);
                            }
                            break;
                        case "DECREASE":
                            int decreaseAmount = Integer.parseInt(parts[1]);
                            if (decreaseAmount < 0) {
                                throw new IllegalArgumentException("Amount cannot be negative");
                            } else {
                                handleDecrease(out, decreaseAmount);
                                addStep(filePath, command);
                                generatelogfile(clientId, command, decreaseAmount);
                            }
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

        private void handleLogout(PrintWriter out, String filePath) {
            if (clientId != null) {
                clients.remove(clientId);
                try {
                    // Create a Path object for the file
                    Path path = Paths.get(filePath);

                    // Delete the file
                    Files.delete(path);

                } catch (IOException e) {
                    System.err.println("Failed to delete the file " + filePath + ": " + e.getMessage());
                }

                if (out != null) {
                    out.println("ACK: Logout successful.");
                }
            }
        }

        private static void addStep(String filePath, String command) {
            try {
                // Read the content of the JSON file
                String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));

                JSONObject clientJson = new JSONObject(jsonContent);
                JSONObject actionsJson = clientJson.getJSONObject("actions");

                // Get the "steps" array inside the "actions" object
                JSONArray stepsArray = actionsJson.getJSONArray("steps");

                // Add a new step to the "steps" array
                stepsArray.put(command); 

                // Write the modified JSON object back to the file
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

    //method to convert a Map to JSON-like string
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
        Server server = new Server();
        server.start(5001);
    }
}


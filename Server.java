import java.net.*;
import java.io.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;


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
                while ((message = in.readLine()) != "LOGOUT") {
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
        
            // Check if the client ID is already registered
            if (clients.containsKey(clientId)) {
                ClientInfo existingClient = clients.get(clientId);
        
                // Verify if the password matches
                if (existingClient.password.equals(password)) {
                    out.println("ACK: Login successful.");
                } else {
                    // Error if the ID is in use with a different password
                    out.println("ERROR: ID already in use with a different password.");
                    return;
                }
            } else {
                // Register new client if ID is not in use
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

                // Locate steps section
                int actionsIndex = jsonContent.indexOf("\"actions\"");
                int stepsIndex = jsonContent.indexOf("\"steps\": [", actionsIndex);

                // Find the position where the "steps" array ends
                int stepsEndIndex = jsonContent.indexOf("]", stepsIndex);

                // Create the new command entry
                String newStepEntry = "\"" + command + "\"";

                // Check if the steps array is empty or not
                boolean isArrayEmpty = jsonContent.substring(stepsIndex + 9, stepsEndIndex).trim().isEmpty();
                
                // Insert the new step command
                String updatedStepsArray;
                if (isArrayEmpty) {
                    // If the array is empty, just insert the new step
                    updatedStepsArray = jsonContent.substring(0, stepsEndIndex) + newStepEntry + jsonContent.substring(stepsEndIndex);
                } else {
                    // If the array has existing elements, add a comma before the new step
                    updatedStepsArray = jsonContent.substring(0, stepsEndIndex) + ", " + newStepEntry + jsonContent.substring(stepsEndIndex);
                } 

                // Write the modified JSON object back to the file
                try (FileWriter fileWriter = new FileWriter(filePath)) {
                    fileWriter.write(updatedStepsArray); 
                    fileWriter.flush();
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


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
            System.err.println("Could not start the server");
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error while stopping the server");;
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
                while ((message = in.readLine()) != "LOGOUT") {
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
                            break;
                        default:
                            out.println("ERROR: Unknown command.");
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client communication");
            } finally {
                handleLogout(out);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket");;
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
                    existingClient.instancesCount += 1;
                    out.println("ACK: Login successful.");
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
    
            int amount = Integer.parseInt(parts[1]);
            if (amount < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
   
            ClientInfo clientInfo = clients.get(clientId);
            String filePath = clientInfo.id + ".json";
            if (command.equals("INCREASE")) {
                clientInfo.counter += amount;
                //addStep(filePath, command, amount); DELETE
            } else { // DECREASE
                clientInfo.counter -= amount;
                //addStep(filePath, command, amount); DELETE
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
                            //DELETE
                            /*try {
                                // Delete the client's JSON file
                                Path path = Paths.get(filePath);
                                Files.delete(path);
                                if (out != null){
                                    out.println("ACK: Logout successful.");
                                }
                            } catch (IOException e) {
                                System.err.println("Failed to delete the file " + filePath);
                            } */
                        } else {
                            out.println("ACK: Logout successful, remaining instances: " + clientInfo.instancesCount);
                        }
                    }
                    clientId = null;
                }   
            }
            catch(NullPointerException e){
                System.err.println("Error: NullPointerException occurred during logout.");
            }
        }

        /* 
        private void addStep(String filePath, String command, int amount) {
            // String filePath = clients.get(clientId).id + ".json";

            try {
                // Read the content of the JSON file
                String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));

                // Locate "steps" section
                int actionsIndex = jsonContent.indexOf("\"actions\"");
                int stepsIndex = jsonContent.indexOf("\"steps\": [", actionsIndex);
                int stepsEndIndex = jsonContent.indexOf("]", stepsIndex);

                // Create the new command entry
                String newStepEntry = "\"" + command + " " + amount + "\"";
                
                // Insert the new step command
                boolean isArrayEmpty = jsonContent.substring(stepsIndex + 10, stepsEndIndex).trim().isEmpty();
                String updatedStepsArray;
                if (isArrayEmpty) {
                    updatedStepsArray = jsonContent.substring(0, stepsEndIndex) + newStepEntry + jsonContent.substring(stepsEndIndex);
                } else {
                    updatedStepsArray = jsonContent.substring(0, stepsEndIndex) + ", " + newStepEntry + jsonContent.substring(stepsEndIndex);
                } 

                // Update JSON file
                try (FileWriter fileWriter = new FileWriter(filePath)) {
                    fileWriter.write(updatedStepsArray); 
                    fileWriter.flush();
                }

            } catch (IOException e) {
                System.err.println("Error updating client file ");
            }
        } */
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
            System.err.println("Error writing to log file ");
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
        Server server = new Server();
        server.start(5001);
    }
}


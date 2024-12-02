import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) {
        try {          
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String sendMessage(String msg) {
        String resp = null;
        try {
            out.println(msg); // Send message to the server
            resp = in.readLine(); // Read response from the server
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resp;
    }

    public void stopConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
    private static void clientCreation(String clientId, String password, String ip,  int port, Scanner inputScanner, Client client){
        String clientFile = clientId + ".json" ;

        // Check if the file already exists
        File file = new File(clientFile);
        if (file.exists()) {
            System.out.println("Client file already exists: " + clientFile);

            // Check the password
            try (BufferedReader fileReader = new BufferedReader(new FileReader(clientFile))) {
                String line;
                String storedPassword = null;

                while ((line = fileReader.readLine()) != null) {
                    line = line.trim();  // Remove leading/trailing spaces
                    if (line.startsWith("\"password\":")) {
                        storedPassword = line.split(":")[1].trim();
                        storedPassword = storedPassword.replace("\"", "").replace(",", "");
                        break;
                    }
                }

                // Compare the stored password with the given one
                if (storedPassword == null || !storedPassword.equals(password)) {
                    System.out.println("ERROR: Password does not match the one in the file. Terminating connection.");
                    inputScanner.close();
                    client.stopConnection();
                    System.exit(0);
                    return; 
                } else {
                    System.out.println("Password matches. Proceeding with further steps.");
                }

            } catch (IOException e) {
                System.err.println("Error reading client file: " + e.getMessage());
                return;
            }
            return; // Exit the method if file exists and password mismatch occurs
        }

        Map<String, Object> newclient = new LinkedHashMap<>();
        
        // Client information
        newclient.put("id", clientId);
        newclient.put("password", password);

        // Server information
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("ip", ip);
        server.put("port", port);
        newclient.put("server", server);

        // Action information
        int delay = (int) (Math.random() * 121); // Generates a random integer from 0 to 120

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("delay", delay );
        action.put("steps", new ArrayList<>()); // Empty array
        newclient.put("actions", action);

        // Convert LinkedHashMap to JSON
        String jsonBuilder = mapToJsonString(newclient, 0);

        // write JSON string to individual file
        try (FileWriter fileWriter = new FileWriter(clientFile)) {
            fileWriter.write(jsonBuilder + "\n"); 
            fileWriter.flush();
            System.out.println("Client JSON file created: " + clientFile);
        } catch (IOException e) {
            System.err.println("Error writing to client file: " + e.getMessage());
        }
    }

    // Generate JSON format
    public static String mapToJsonString(Map<String, Object> map, int indentLevel) {
        StringBuilder jsonBuilder = new StringBuilder("{\n");
        String indent = "    ".repeat(indentLevel);
        String nestedIndent = "    ".repeat(indentLevel + 1);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            jsonBuilder.append(nestedIndent).append("\"").append(entry.getKey()).append("\": ");

            // Check if the value is a Map
            if (entry.getValue() instanceof Map) {
                jsonBuilder.append(mapToJsonString((Map<String, Object>) entry.getValue(), indentLevel + 1));
            }
            // Check if it is steps section
            else if (entry.getKey().equals("steps")) {
                jsonBuilder.append("[]");
            }
            // Add " " for strings
            else if (entry.getValue() instanceof String) {
                jsonBuilder.append("\"").append(entry.getValue()).append("\"");
            } 
            else {
                jsonBuilder.append(entry.getValue());
            }
            jsonBuilder.append(",\n");
        }

        jsonBuilder.delete(jsonBuilder.length() - 2, jsonBuilder.length());
        jsonBuilder.append("\n").append(indent).append("}");
        return jsonBuilder.toString();
    }

    public void sendIncrease(int amount) {
        System.out.println("Sending INCREASE command with amount: " + amount);
        String response = sendMessage("INCREASE " + amount);
        System.out.println(response);
    }

    public void sendDecrease(int amount) {
        System.out.println("Sending DECREASE command with amount: " + amount);
        String response = sendMessage("DECREASE " + amount);
        System.out.println(response);
    }

    public void sendLogout() {
        System.out.println("Sending LOGOUT command.");
        String response = sendMessage("LOGOUT");
        System.out.println(response);
    }
    
    private static int getValidatedInput(Scanner inputScanner, String action) {
        int amount = -1;
        boolean valid = false;
    
        while (!valid) {
            System.out.print("Enter amount to " + action + ": ");
            if (inputScanner.hasNextInt()) {
                amount = inputScanner.nextInt();
                if (amount > 0 && amount <= Integer.MAX_VALUE) {
                    valid = true; 
                } else {
                    System.out.println("ERROR: Invalid input.");
                }
            } else { //this happen if input in not an interger
                System.out.println("ERROR: Invalid input.");
                inputScanner.next(); // Clear invalid input
            }
        }
        inputScanner.nextLine(); 
        return amount;
    }

    private static boolean isValidClientId(String clientId) {
        return clientId != null && clientId.matches("^[a-zA-Z0-9_-]{3,20}$"); // Allow letters, numbers, _ and -, 3-20 characters
    }
    
    private static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 30) {
            return false; // Length requirement
        }
        // Regular expression to check at least one special character and two digits
        String specialCharAndDigitRegex = "^(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])(?=.*\\d.*\\d).+$";
        return password.matches(specialCharAndDigitRegex);
    }

    private static String getValidatedClientId(Scanner scanner) {
        while (true) {
            System.out.print("Enter client ID: ");
            String clientId = scanner.nextLine();
            //IDs must be 3-20 characters, alphanumeric, '_', or '-'
            if (clientId != null && clientId.matches("^[a-zA-Z0-9_-]{3,20}$")) {
                return clientId;
            } else {
                System.out.println("ERROR: Invalid client ID");
            }
        }
    }
    
    private static String getValidatedPassword() {
        Console console = System.console();
        while (true) {
            System.out.print("Enter password: ");
            char[] passwordArray = console.readPassword();
            String password = new String(passwordArray);
            //Passwords must be 8-30 characters, at least one special character, at least two digits
            if (password != null && password.matches("^(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])(?=.*\\d.*\\d).+$")) {
                return password;
            } else {
                System.out.println("ERROR: Invalid password");
            }
        }
    }
    
    
    

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);

        // Get ip from user
        String ip = null;
        try {
            InetAddress localIpAddress = InetAddress.getLocalHost();
            ip = localIpAddress.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace(); 
        }

        // Get port from user
        System.out.print("Enter port number: ");
        int port = inputScanner.nextInt();

        String clientId = getValidatedClientId(inputScanner);
        String password = getValidatedPassword();

        // Create client with user-provided IP and port
        Client client = new Client();

        client.startConnection(ip,port);
        clientCreation(clientId, password, ip, port, inputScanner, client);
        

        // Register the client
        String response = client.sendMessage("REGISTER " + clientId + " " + password);
        System.out.println(response);

        String command;
        do {
            System.out.println("Enter command (INCREASE, DECREASE, LOGOUT): ");
            command = inputScanner.nextLine().toUpperCase();

            switch (command) {
                case "INCREASE" -> {
                    int increaseAmount = getValidatedInput(inputScanner, "increase");
                    client.sendIncrease(increaseAmount);
                }
                case "DECREASE" -> {
                    int decreaseAmount = getValidatedInput(inputScanner, "decrease");
                    client.sendDecrease(decreaseAmount);
                }
                case "LOGOUT" -> client.sendLogout();
                default -> System.out.println("Unknown command. Try again.");
            }
        } while (!command.equals("LOGOUT"));

        inputScanner.close();
        client.stopConnection();
    }
}
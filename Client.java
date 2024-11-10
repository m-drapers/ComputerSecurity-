import java.net.*;
import java.io.*;
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
            out.println(msg);
            resp = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resp;
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void clientCreation(String clientId, String password, String ip,  int port ){
        String clientFile = clientId + ".JSON" ;

        Map<String, Object> client = new LinkedHashMap<>();
        
        //Client information
        client.put("id", clientId);
        client.put("password", password);

        //Server informatiom
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("ip", ip);
        server.put("port", port);
        client.put("server", server);

        //Action information
        int delay = (int) (Math.random() * 121); // Generates a random integer from 0 to 120

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("delay", delay );
        action.put("steps", new ArrayList<>()); // Empty array
        client.put("actions", action);

        //Convert LinkedHashMap to JSON
        String jsonBuilder = mapToJsonString(client,0);

        //write JSON string to individual file
        try (FileWriter fileWriter = new FileWriter(clientFile)) {
            fileWriter.write(jsonBuilder + "\n"); 
            fileWriter.flush();
            System.out.println("Client JSON file created: " + clientFile);
        } catch (IOException e) {
            System.err.println("Error writing to client file: " + e.getMessage());
        }
        

    }

    //Generate JSON format
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
    

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);

        //get ip from user
        String ip = null;
        try {
            InetAddress localIpAddress = InetAddress.getLocalHost();
            ip = localIpAddress.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace(); 
        }

        //get port from user
        System.out.print("Enter port number: ");
        int port = inputScanner.nextInt();

        //get client ID from user
        inputScanner.nextLine(); // Consume newline
        System.out.print("Enter client ID: ");
        String clientId = inputScanner.nextLine();

        //get password from user
        System.out.print("Enter password: ");
        String password = inputScanner.nextLine();

        //create client with user-provided IP and port
        Client client = new Client();

        clientCreation(clientId, password, ip, port);
        client.startConnection(ip,port);

        //register the client
        String response = client.sendMessage("REGISTER " + clientId + " " + password);
        System.out.println(response);

        String command;
        do {
            System.out.println("Enter command (INCREASE, DECREASE, LOGOUT): ");
            command = inputScanner.nextLine().toUpperCase();

            switch (command) {
                case "INCREASE":
                    System.out.print("Enter amount to increase: ");
                    int increaseAmount = inputScanner.nextInt();
                    inputScanner.nextLine(); // Consume newline
                    client.sendIncrease(increaseAmount);
                    break;
                case "DECREASE":
                    System.out.print("Enter amount to decrease: ");
                    int decreaseAmount = inputScanner.nextInt();
                    inputScanner.nextLine(); // Consume newline
                    client.sendDecrease(decreaseAmount);
                    break;
                case "LOGOUT":
                    client.sendLogout();
                    break;
                default:
                    System.out.println("Unknown command. Try again.");
            }
        } while (!command.equals("LOGOUT"));

        inputScanner.close();
        client.stopConnection();
    }
}


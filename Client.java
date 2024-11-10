import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONObject;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;



    public void startConnection(int port) {
        try {
            // Get the local IP address of the device
            InetAddress localIpAddress = InetAddress.getLocalHost();
            String ip = localIpAddress.getHostAddress();

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

    private static void clientCreation(String clientId, String password, String ip, int port ){
        String clientFile = clientId + ".JSON" ;

        Map<String, Object> client = new LinkedHashMap<>();
        
        // Client Information
        client.put("id", clientId);
        client.put("password", password); 

        // Server Informatiom
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("ip", ip);
        server.put("port", port);
        client.put("server", server);

        // Action Information
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("delay", "" );
        action.put("steps", new ArrayList<>()); // Empty array
        client.put("actions", action); 

        // Convert LinkedHashMap to JSONObject

        String jsonBuilder = mapToJsonString(client,0);

        JSONObject clientJson = new JSONObject();
        clientJson.put("id", client.get("id"));
        clientJson.put("password", client.get("password")); 

        // Write JSON string to individual file
        try (FileWriter fileWriter = new FileWriter(clientFile)) {
            fileWriter.write(jsonBuilder + "\n");
            //fileWriter.write(clientJson.toString(4)); // Pretty print with 4-space indentation
            fileWriter.flush();
            System.out.println("Client JSON file created: " + clientFile);
        } catch (IOException e) {
            System.err.println("Error writing to client file: " + e.getMessage());
        }
        

    }

    public static String mapToJsonString(Map<String, Object> map, int indentLevel) {
        StringBuilder jsonBuilder = new StringBuilder("{\n");
        String indent = "    ".repeat(indentLevel);
        String nestedIndent = "    ".repeat(indentLevel + 1);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            jsonBuilder.append(nestedIndent).append("\"").append(entry.getKey()).append("\": ");

            // Check if the value is a Map (nested JSON object)
            if (entry.getValue() instanceof Map) {
                jsonBuilder.append(mapToJsonString((Map<String, Object>) entry.getValue(), indentLevel + 1));
            }
            // If the value is the "steps" list, add it as an empty array
            else if (entry.getKey().equals("steps")) {
                jsonBuilder.append("[]");
            }
            // For regular strings, include them with quotes
            else if (entry.getValue() instanceof String) {
                jsonBuilder.append("\"").append(entry.getValue()).append("\"");
            } 
            // For other types like numbers
            else {
                jsonBuilder.append(entry.getValue());
            }
            jsonBuilder.append(",\n");
        }

        // Remove the last comma and newline, then close the JSON object
        jsonBuilder.delete(jsonBuilder.length() - 2, jsonBuilder.length());
        jsonBuilder.append("\n").append(indent).append("}");
        //jsonBuilder.append("\n}");
        return jsonBuilder.toString();
    }
    

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);

        // Get port from user
        System.out.print("Enter port number: ");
        int port = inputScanner.nextInt();

        // Get client ID from user
        inputScanner.nextLine(); // Consume newline
        System.out.print("Enter client ID: ");
        String clientId = inputScanner.nextLine();

        // Get password from user
        System.out.print("Enter password: ");
        String password = inputScanner.nextLine();

        // Create client with user-provided IP and port
        Client client = new Client();

        clientCreation(clientId, password, "127.0.0.1", port);
        client.startConnection( port);

        // Register the client
        String response = client.sendMessage("REGISTER " + clientId + " " + password);
        System.out.println(response);

        
        inputScanner.close();
        client.stopConnection();
    }
}

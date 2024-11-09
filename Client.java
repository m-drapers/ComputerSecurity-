import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
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

            clientSocket = new Socket("127.0.0.1", port);
            System.out.println("Connected");
            
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }

    public String sendMessage(String msg) {
        try {
            out.println(msg); // Send message to the server
            return in.readLine(); // Read response from the server
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            return null;
        }
    }

    public void stopConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    private static void clientCreation(String clientId, String password, String ip, int port ){
        String clientFile = clientId + ".json" ;

        Map<String, Object> client = new LinkedHashMap<>();
        
        // Client Information
        client.put("id", clientId);
        client.put("password", password);

        // Server Information
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("ip", ip);
        server.put("port", port);
        client.put("server", server);

        // Action Information
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("delay", "" );
        action.put("steps", new JSONArray()); // Empty array
        client.put("actions", action);

        // Convert LinkedHashMap to JSONObject
        JSONObject clientJson = new JSONObject(client);

        // Write JSON string to individual file
        try (FileWriter fileWriter = new FileWriter(clientFile)) {
            fileWriter.write(clientJson.toString(4));
            fileWriter.flush();
            System.out.println("Client JSON file created: " + clientFile);
        } catch (IOException e) {
            System.err.println("Error writing to client file: " + e.getMessage());
        }
        

    }
    

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);

        // Get port from user
        // System.out.print("Enter port number: ");
        // int port = inputScanner.nextInt();

        // Get client ID from user
        System.out.print("Enter client ID: ");
        String clientId = inputScanner.nextLine();

        // Get password from user
        System.out.print("Enter password: ");
        String password = inputScanner.nextLine();

        // Create client with user-provided IP and port
        Client client = new Client();

        clientCreation(clientId, password, "127.0.0.1", 5000);
        client.startConnection(5000);

        // Register the client
        String response = client.sendMessage("REGISTER " + clientId + " " + password);
        System.out.println(response);

        
        inputScanner.close();
        client.stopConnection();
    }
}

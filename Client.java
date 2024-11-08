import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import KeyEncrypt.RSA;

import org.json.JSONArray;
import org.json.JSONObject;

public class Client {
    static RSA rsa = new RSA();
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

    private static void clientCreation(String clientId, int clientNumber, String password, String ip, int port ) throws Exception {
        String clientFile = "client"+ String.valueOf(clientNumber) + ".JSON" ;

        Map<String, Object> client = new LinkedHashMap<>();
        
        // Client Information
        client.put("id", clientId);
        client.put("password",rsa.encrypt(password));

        // Server Informatiom
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
            fileWriter.write(clientJson.toString(4)); // Pretty print with 4-space indentation
            fileWriter.flush();
            System.out.println("Client JSON file created: " + clientFile);
        } catch (IOException e) {
            System.err.println("Error writing to client file: " + e.getMessage());
        }
        

    }
    

    public static void main(String[] args) throws Exception {
        Scanner inputScanner = new Scanner(System.in);

        // Get port from user
        System.out.print("Enter port number: ");
        int port = inputScanner.nextInt();

        // Get client ID from user
        inputScanner.nextLine(); // Consume newline
        System.out.print("Enter client ID: ");
        String clientId = inputScanner.nextLine();

        //TODO check the password

        // Get password from user
        System.out.print("Enter password: ");
        String password = inputScanner.nextLine();

        //TODO parallel running for multiple clients

        // Create client with user-provided IP and port
        Client client = new Client();
        int clientNumber =0;
        clientCreation(clientId, clientNumber, password, "127.0.0.1", port);
        client.startConnection( port);

        clientNumber++;
        // Register the client
        String response = client.sendMessage("REGISTER " + clientId + " " + password);
        System.out.println(response);

        
        inputScanner.close();
        client.stopConnection();
    }
}

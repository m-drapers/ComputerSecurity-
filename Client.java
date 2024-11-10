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
            //get the local IP address of the device
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
        
        //client information
        client.put("id", clientId);
        client.put("password", password);

        //server informatiom
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("ip", ip);
        server.put("port", port);
        client.put("server", server);

        //action information
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("delay", "" );
        action.put("steps", new JSONArray()); // Empty array
        client.put("actions", action);

        //convert LinkedHashMap to JSONObject
        JSONObject clientJson = new JSONObject(client);

        //write JSON string to individual file
        try (FileWriter fileWriter = new FileWriter(clientFile)) {
            fileWriter.write(clientJson.toString(4)); // Pretty print with 4-space indentation
            fileWriter.flush();
            System.out.println("Client JSON file created: " + clientFile);
        } catch (IOException e) {
            System.err.println("Error writing to client file: " + e.getMessage());
        }
        

    }
    

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);

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

        clientCreation(clientId, password, "127.0.0.1", port);
        client.startConnection(port);

        //register the client
        String response = client.sendMessage("REGISTER " + clientId + " " + password);
        System.out.println(response);

        
        inputScanner.close();
        client.stopConnection();
    }
}

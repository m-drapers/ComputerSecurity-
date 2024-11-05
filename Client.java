import java.net.*;
import java.io.*;
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
        String resp = null; // Declare and initialize 'resp' outside the try block
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);
        
        // Get IP address from user
        System.out.print("Enter IP address: ");
        String ipAddress = inputScanner.nextLine();
        
        // Get port from user
        System.out.print("Enter port number: ");
        int port = inputScanner.nextInt();
        
        // Create client with user-provided IP and port
        Client client = new Client();
        client.startConnection(ipAddress, port);

        inputScanner.close();
    }
}
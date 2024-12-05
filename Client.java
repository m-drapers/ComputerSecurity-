import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Scanner;
import javax.net.ssl.*;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;


    public void startConnection(String ip, int port, String truststore_password) {
        try {       
            // Load the client truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream trustStoreStream = new FileInputStream("client.truststore")) {
                char[] trustStorePassword = truststore_password.toCharArray();
                trustStore.load(trustStoreStream, trustStorePassword);
            }

            // Create trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            // Initialize SSL context
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            // Create SSL client socket   
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            SSLSocket clientSocket = (SSLSocket) socketFactory.createSocket(ip, port);
            
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
        } catch (UnknownHostException e) {
            System.err.println("Unable to connect to the server."); //Please check IP address
        } catch (IOException e) {
            System.err.println("Unable to connect to the server");
        } catch (Exception e) {
            System.err.println("Unable to connect to the server");
            stopConnection(); // Close resources if setup fails
        }
    }

    public String sendMessage(String msg) {
        String resp = null;
        try {
            out.println(msg); // Send message to the server
            resp = in.readLine(); // Read response from the server
        } catch (IOException e) {
            System.err.println("Unable to connect to the server");;
        }
        return resp;
    }

    public void stopConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client connection");
            } finally {
                System.clearProperty("CLIENT_TRUSTORE_PASSWORD");
            }
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
        try{
            System.out.println("Sending LOGOUT command.");
            String response = sendMessage("LOGOUT");
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Unable to connect to logout");
        }        
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
    
    private static String getValidatedClientId(Scanner scanner) {
        while (true) {
            System.out.print("Enter client ID: ");
            String clientId = scanner.next();
            // IDs must be 3-20 characters, alphanumeric, '_', or '-'
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

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
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
            System.err.println("Error IP");
        }

        // Get port from user
        System.out.print("Enter port number: ");
        int port = inputScanner.nextInt();

        // Get client ID an dpassword from user
        String clientId = getValidatedClientId(inputScanner);
        String password = getValidatedPassword();
        password = hashPassword(password);

        // Create client with user-provided IP and port
        Client client = new Client();

        System.out.print("Enter your truststore password: ");
        Console console = System.console();
        char[] truststore_passwordArray = console.readPassword();
        String truststore_password = new String(truststore_passwordArray);

        client.startConnection(ip, port, truststore_password);
        
        // Register the client
        String response = client.sendMessage("REGISTER " + clientId + " " + password);
        System.out.println(response);
        if (!response.startsWith("ACK")) {
            System.out.println("Server error during registration: " + response);
            inputScanner.close();
            client.stopConnection();
            System.exit(1);
        }

        String command;
        do {
            System.out.println("Enter command (INCREASE, DECREASE, LOGOUT): ");

            command = inputScanner.hasNextLine() ? inputScanner.next().toUpperCase() : "LOGOUT";

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
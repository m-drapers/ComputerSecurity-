import java.net.*;
import java.io.*;


public class Server {
    private ServerSocket serverSocket;

    public void start(int port) {
        try{
        serverSocket = new ServerSocket(port);
        while (true)
            new ClientHandler(serverSocket.accept()).start();
        }
        catch (IOException e) {
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

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true)) {

                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(" ");
                    String command = parts[0];

                    switch (command) {
                        case "REGISTER":
                            handleRegister(out, parts);
                            break;
                        case "INCREASE":
                            handleIncrease(out, Integer.parseInt(parts[1]));
                            break;
                        case "DECREASE":
                            handleDecrease(out, Integer.parseInt(parts[1]));
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
                e.printStackTrace();
            } finally {
                handleLogout(null);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    
        private void handleRegister(PrintWriter out, String[] parts) {
            clientId = parts[1];
            String password = parts[2];

            if (clients.containsKey(clientId) && !clients.get(clientId).password.equals(password)) {
                out.println("ERROR: ID already registered with a different password.");
            } else {
                clients.put(clientId, new ClientInfo(clientId, password, 0));
                out.println("ACK: Registration successful.");
            }
        }

        private void handleIncrease(PrintWriter out, int amount) {
            ClientInfo clientInfo = clients.get(clientId);
            if (clientInfo != null) {
                clientInfo.counter += amount;
                logAction("INCREASE", clientId, clientInfo.counter);
                out.println("Counter increased to " + clientInfo.counter);
            } else {
                out.println("ERROR: Client not registered.");
            }
        }

        private void handleDecrease(PrintWriter out, int amount) {
            ClientInfo clientInfo = clients.get(clientId);
            if (clientInfo != null) {
                clientInfo.counter -= amount;
                logAction("DECREASE", clientId, clientInfo.counter);
                out.println("Counter decreased to " + clientInfo.counter);
            } else {
                out.println("ERROR: Client not registered.");
            }
        }

        private void handleLogout(PrintWriter out) {
            if (clientId != null) {
                clients.remove(clientId);
                logAction("LOGOUT", clientId, 0);
                if (out != null) {
                    out.println("ACK: Logout successful.");
                }
            }
        }

        private void logAction(String action, String clientId, int counter) {
            try (PrintWriter logWriter = new PrintWriter(new FileWriter("server.log", true))) {
                logWriter.printf("%s: Client %s, Counter = %d%n", action, clientId, counter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    }
public static void main(String args[])
    {
        Server server = new Server();
        server.start(5000);
    }
}


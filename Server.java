import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private ServerSocket serverSocket;
    private Map<String, ClientInfo> clients = new HashMap<>();

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
            while (true)
                new ClientHandler(serverSocket.accept()).start();
        } catch (IOException e) {
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

    private class ClientInfo {
        String id;
        String password;
        int counter;

        ClientInfo(String id, String password, int counter) {
            this.id = id;
            this.password = password;
            this.counter = counter;
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
                handleLogout(out);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
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

            if (clients.containsKey(clientId)) {
                out.println("ERROR: ID already registered.");
            } else {
                clients.put(clientId, new ClientInfo(clientId, password, 0));
                out.println("ACK: Registration successful.");
            }
        }

        private void handleIncrease(PrintWriter out, int amount) {
            ClientInfo clientInfo = clients.get(clientId);
            if (clientInfo != null) {
                clientInfo.counter += amount;
                out.println("Counter increased to " + clientInfo.counter);
            } else {
                out.println("ERROR: Client not registered.");
            }
        }

        private void handleDecrease(PrintWriter out, int amount) {
            ClientInfo clientInfo = clients.get(clientId);
            if (clientInfo != null) {
                clientInfo.counter -= amount;
                out.println("Counter decreased to " + clientInfo.counter);
            } else {
                out.println("ERROR: Client not registered.");
            }
        }

        private void handleLogout(PrintWriter out) {
            if (clientId != null) {
                clients.remove(clientId);
                if (out != null) {
                    out.println("ACK: Logout successful.");
                }
            }
        }
    }

    public static void main(String args[]) {
        Server server = new Server();
        server.start(5000);
    }
}


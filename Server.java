import java.net.*;
import java.io.*;


public class Server {
    //initialize socket and input stream
    private ServerSocket serverSocket   = null;

    public void start(int port) {
        serverSocket = new ServerSocket(port);
        while (true)
            new ClientHandler(serverSocket.accept()).start();
    }

    public void stop() {
        serverSocket.close();
    }
    

    private static class ClientHandler extends Thread{
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;


    }

}


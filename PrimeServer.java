import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeServer {
    public static void main(String[] args) throws IOException {
        startServer(12345, "Master Server");
        startServer(12346, "Slave Server");
    }
    
    private static void startServer(int port, String serverName) {
        new Thread(() -> {
            try {
                @SuppressWarnings("resource")
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println(serverName + " started on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
                int[] range = (int[]) in.readObject(); // Receive range from client
                List<Integer> primesInRange = PrimeChecker.get_primes(range[0], range[1]); // Generate primes
                out.writeObject(primesInRange); // Send primes back to client
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}


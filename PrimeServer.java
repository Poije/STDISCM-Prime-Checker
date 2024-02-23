import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeServer {
    public static void main(String[] args) throws IOException {
        @SuppressWarnings("resource")
        ServerSocket serverSocket = new ServerSocket(12345); // Server socket listening on port 12345

        while (true) {
            System.out.println("Server is running...");
            Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
            Thread clientThread = new Thread(new ClientHandler(clientSocket));
            clientThread.start();
        }
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


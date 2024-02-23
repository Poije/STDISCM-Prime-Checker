import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeClient {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the port number: ");
        int port = scanner.nextInt();
        Socket socket = new Socket("localhost", port); // Connect to server on localhost, port 12345
        
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        System.out.print("Enter the lower bound: ");
        int start = scanner.nextInt(); // Define your start and end ranges here
        System.out.print("Enter the upper bound: ");
        int end = scanner.nextInt();

        out.writeObject(new int[]{start, end}); // Send range to server

        try {
            @SuppressWarnings("unchecked")
            List<Integer> primes = (List<Integer>) in.readObject(); // Receive primes from server
            System.out.println("Number of Primes: " + primes.size());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
        scanner.close();
    }
}

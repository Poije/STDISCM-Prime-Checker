import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeClient {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter the port number: ");
        int port = scanner.nextInt(); // User inputs the port number
        
        Socket socket = new Socket("localhost", port); // Connect to server on localhost with the user-specified port
        
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        System.out.print("Enter the lower bound: ");
        int start = scanner.nextInt(); // Define your start and end ranges here
        System.out.print("Enter the upper bound: ");
        int end = scanner.nextInt();
        System.out.print("Enter the number of threads: ");
        int numThreads = scanner.nextInt();

        out.writeObject(new Object[]{new int[]{start, end}, numThreads}); // Send range and numThreads to server

        try {
            @SuppressWarnings("unchecked")
            Object[] receivedData = (Object[]) in.readObject();
            List<Integer> primes = (List<Integer>) receivedData[0]; // Receive primes from server
            long elapsedTimeMillis = (long) receivedData[1]; // Receive elapsed time
            System.out.println("Number of Primes: " + primes.size());
            System.out.println("Runtime: " + elapsedTimeMillis + " milliseconds");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            socket.close();
            scanner.close();
        }
    }
}
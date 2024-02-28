import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeSlave {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 12345); // Connect to server on localhost, port 12345
        System.out.println("Slave Connected to Server...");

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        try {
            List<Integer> primes = new ArrayList<>();
            //int[] range = (int[]) in.readObject(); // Receive primes from server
            Object[] receivedData = (Object[]) in.readObject();
            int[] range = (int[]) receivedData[0]; // Extract range
            int numThreads = (Integer) receivedData[1]; // Extract number of threads
            primes = PrimeChecker.get_primes(range[0], range[1], numThreads);
            out.writeObject(primes);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}

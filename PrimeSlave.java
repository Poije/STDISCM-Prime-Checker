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
            int[] range = (int[]) in.readObject(); // Receive primes from server
            primes = PrimeChecker.get_primes(range[0], range[1]);
            out.writeObject(primes);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}

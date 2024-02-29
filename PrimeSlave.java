import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeSlave {
    public static void main(String[] args) throws IOException {
        try(ServerSocket serverSocket = new ServerSocket(12346)) {
            System.out.println("Slave Server started on port 12346");
            while (true) {
                Socket MasterSocket = serverSocket.accept();
                System.out.println("Connected to Master Server");
                new Thread(new SlaveHandler(MasterSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class SlaveHandler implements Runnable {
        private final Socket clientSocket;

        public SlaveHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
                Object[] data = (Object[]) in.readObject();
                int[] bounds = (int[]) data[0];
                int numThreads = (int) data[1];

                List<Integer> primes = new ArrayList<>();
                long startTime = System.nanoTime();

                System.out.println("Calculating primes from " + bounds[0] + " to " + bounds[1]);
                primes = PrimeChecker.get_primes(bounds[0], bounds[1], numThreads);

                long endTime = System.nanoTime();
                long elapsedTimeMillis = (endTime - startTime) / 1000000;
                System.out.println("Elapsed Time: " + elapsedTimeMillis);
                out.writeObject(primes);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeServer {
    private static final int MASTER_SERVER_PORT = 12345;
    private static final int SLAVE_SERVER_PORT_1 = 12346;
    private static final int SLAVE_SERVER_PORT_2 = 12347;

    public static void main(String[] args) {
        startSlaveServer(SLAVE_SERVER_PORT_1, "Slave Server 1");
        startSlaveServer(SLAVE_SERVER_PORT_2, "Slave Server 2");

        try (ServerSocket serverSocket = new ServerSocket(MASTER_SERVER_PORT)) {
            System.out.println("Master Server started on port " + MASTER_SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                Socket slaveSocket = serverSocket.accept(); // Accept incoming slave connections
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startSlaveServer(int port, String serverName) {
        new Thread(() -> {
            try {
                @SuppressWarnings("resource")
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println(serverName + " started on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                    Thread clientThread = new Thread(new SlaveHandler(clientSocket));
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
                List<Integer> primes = new ArrayList<>();

                // Distribute tasks to slave servers and collect results
                primes.addAll(distributeTask(range));

                out.writeObject(primes); // Send primes back to client
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private List<Integer> distributeTask(int[] range) {
            List<int[]> partitionedList = PrimeChecker.split_range(range[1], 2);
            List<Integer> allPrimes = new ArrayList<>();

            for (int[] subrange : partitionedList) {
                allPrimes.addAll(distributeSubRangeToSlave(subrange));
            }

            return allPrimes;
        }

        // use PrimeChecker.split_range(limit, numthreads) instead to split the range into subranges
    /* 
        private List<int[]> splitRange(int limit, int numParts) {
            List<int[]> partitionedList = new ArrayList<>();
            int range = limit / numParts;
            int start = 0;
            int end = range;

            for (int i = 0; i < numParts; i++) {
                int[] temp = {start, end};
                partitionedList.add(temp);
                start = end + 1;
                end += range;
            }

            // Adjust the end of the last subrange to ensure it covers the entire range
            partitionedList.get(numParts - 1)[1] = limit;

            return partitionedList;
        }
    */

        @SuppressWarnings("unchecked")
        private List<Integer> distributeSubRangeToSlave(int[] subrange) {
            try (Socket slaveSocket = new Socket("localhost", SLAVE_SERVER_PORT_1);
                 ObjectOutputStream out = new ObjectOutputStream(slaveSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(slaveSocket.getInputStream())) {

                out.writeObject(subrange); // Send subrange to slave server
                return (List<Integer>) in.readObject(); // Receive primes from slave server
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return new ArrayList<>(); // Handle error gracefully
            }
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

                int[] range = (int[]) in.readObject(); // Receive range from master server
                System.out.println("Port: " + clientSocket.getPort() + " Received range: " + Arrays.toString(range));
                List<Integer> primes = PrimeChecker.get_primes(range[0], range[1]); // Generate primes
                out.writeObject(primes); // Send primes back to master server
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

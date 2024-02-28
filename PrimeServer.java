import java.io.*;
import java.net.*;
import java.util.*;

public class PrimeServer {
    private static final int MASTER_SERVER_PORT = 12345;
    private static final int SLAVE_SERVER_PORT_1 = 12346;
    private static final int SLAVE_SERVER_PORT_2 = 12347;
    private static final List<Integer> slavePorts = Arrays.asList(SLAVE_SERVER_PORT_1, SLAVE_SERVER_PORT_2);

    public static void main(String[] args) {
        startSlaveServer(SLAVE_SERVER_PORT_1, "Slave Server 1");
        startSlaveServer(SLAVE_SERVER_PORT_2, "Slave Server 2");

        try (ServerSocket serverSocket = new ServerSocket(MASTER_SERVER_PORT)) {
            System.out.println("Master Server started on port " + MASTER_SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming client connections
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Probably have to fix this method to accomodate the seperate slave servers
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
                    Object[] receivedData = (Object[]) in.readObject();
                    int[] range = (int[]) receivedData[0]; // Extract range
                    int numThreads = (Integer) receivedData[1]; // Extract number of threads
        
                    List<Integer> primes = distributeTask(range);
                    out.writeObject(primes);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private List<Integer> distributeTask(int[] range) {
            int totalRange = range[1] - range[0] + 1;
            int totalServers = slavePorts.size() + 1; // +1 for the master server
            List<Integer> allPrimes = new ArrayList<>();
        
            // Calculate the size of each subrange considering the master server
            int subrangeSize = totalRange / totalServers;
            int remainder = totalRange % totalServers;
            int currentStart = range[0];
        
            // Master server processes its subrange
            int masterEnd = currentStart + subrangeSize - 1;
            if (remainder > 0) {
                masterEnd += 1;
                remainder--;
            }
            System.out.println("Master Server at port: "  + clientSocket.getLocalPort() + " received range: [" + currentStart + ", " + masterEnd + "]");
            allPrimes.addAll(PrimeChecker.get_primes(currentStart, masterEnd));
        
            // Update the start for the slave servers
            currentStart = masterEnd + 1;
        
            // Distribute the rest of the range to the slave servers
            for (int slavePort : slavePorts) {
                int slaveEnd = currentStart + subrangeSize - 1;
                if (remainder > 0) {
                    slaveEnd += 1;
                    remainder--;
                }
                int[] slaveSubrange = new int[]{currentStart, slaveEnd};
                allPrimes.addAll(distributeSubRangeToSlave(slaveSubrange, slavePort));
                currentStart = slaveEnd + 1; // Update the start for the next slave server
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

    // Probably have to fix this method to accomodate the seperate slave servers
        @SuppressWarnings("unchecked")
        private List<Integer> distributeSubRangeToSlave(int[] subrange, int slavePort) {
            try (Socket slaveSocket = new Socket("localhost", slavePort);
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

    //Proabably have to be Similar to the ClientHandler
    private static class SlaveHandler implements Runnable {
        private final Socket clientSocket;
    
        public SlaveHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
    
        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
    
                // Receive range from master server
                int[] range = (int[]) in.readObject(); 
                System.out.println("Slave Server at port: " + clientSocket.getLocalPort() + " received range: " + Arrays.toString(range));
    
                // Generate primes within the received range
                List<Integer> primes = PrimeChecker.get_primes(range[0], range[1]);
    
                // Send primes back to master server
                out.writeObject(primes); 
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    
}

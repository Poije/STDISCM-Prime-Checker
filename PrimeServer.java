import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

public class PrimeServer {
    private static final int MASTER_SERVER_PORT = 12345;
    //private static final List<Integer> slavePorts = Collections.synchronizedList(new ArrayList<>());
    private static ExecutorService clientExecutor;
    private static ExecutorService slaveExecutor;
    public static int numSlaves = 0;
    
    public static void main(String[] args) {
        clientExecutor = Executors.newCachedThreadPool(); 
        slaveExecutor = Executors.newFixedThreadPool(2); 

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Number of Slaves: ");
        numSlaves = scanner.nextInt();
        try (ServerSocket serverSocket = new ServerSocket(MASTER_SERVER_PORT)) {
            System.out.println("Master Server started on port " + MASTER_SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientExecutor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientExecutor.shutdown();
            slaveExecutor.shutdown();
        }
        scanner.close();
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
                    long startTime = System.nanoTime(); 

                    int[] range = (int[]) receivedData[0]; 
                    int numThreads = (Integer) receivedData[1]; 
        
                    List<Integer> primes = distributeTask(range, numThreads);
                    long endTime = System.nanoTime();
                    long elapsedTimeMillis = (endTime - startTime) / 1000000;
                    System.out.println("Elapsed Time: " + elapsedTimeMillis); //check
                    
                    sendCompressedData(new Object[]{primes, startTime}, clientSocket);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private List<Integer> distributeTask(int[] range, int numThreads) {
            int totalRange = range[1] - range[0] + 1;
            int totalServers = numSlaves + 1; 
            List<Integer> allPrimes = new ArrayList<>();
        
            int subrangeSize = totalRange / totalServers;
            int remainder = totalRange % totalServers;
            int currentStart = range[0];
        
            int masterEnd = currentStart + subrangeSize - 1;
            if (remainder > 0) {
                masterEnd += 1;
                remainder--;
            }
            System.out.println("Master Server at port: "  + clientSocket.getLocalPort() + " received range: [" + currentStart + ", " + masterEnd + "]");
            allPrimes.addAll(PrimeChecker.get_primes(currentStart, masterEnd, numThreads));
            // System.out.println("Master calculated primes: " + allPrimes);
        
            currentStart = masterEnd + 1;
        
            for (int i = 0; i < numSlaves; i++) {
                int slaveEnd = currentStart + subrangeSize - 1;
                if (remainder > 0) {
                    slaveEnd += 1;
                    remainder--;
                }
                int[] slaveSubrange = new int[]{currentStart, slaveEnd};
                allPrimes.addAll(distributeSubRangeToSlave(slaveSubrange, 12346, numThreads));
                currentStart = slaveEnd + 1; 
            }
        
            return allPrimes;
        }

    // Probably have to fix this method to accomodate the seperate slave servers
        @SuppressWarnings("unchecked")
        private List<Integer> distributeSubRangeToSlave(int[] subrange, int slavePort, int numThreads) {
            List<Integer> primes = new ArrayList<>();
            try (ServerSocket masterSocket = new ServerSocket(12346)){
                System.out.println("Slave Server started on port 12346");
                while (true) {
                    Socket slaveSocket = masterSocket.accept();
                    System.out.println("Connected to Slave Server");
                    new Thread(new SlaveHandler(slaveSocket, subrange, numThreads)).start();
                    return primes;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return primes;
            }
        }

        private void sendCompressedData(Object data, Socket socket) {
            try {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteStream);
                    ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut)) {
                    objectOut.writeObject(data);
                } 

                byte[] compressedData = byteStream.toByteArray();
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeInt(compressedData.length);
                dos.write(compressedData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Proabably have to be Similar to the ClientHandler
    private static class SlaveHandler implements Runnable {
        private final Socket slaveSocket;
        private final int[] range;
        private final int numThreads;
    
        public SlaveHandler(Socket slaveSocket, int[] range, int numThreads) {
            this.slaveSocket = slaveSocket;
            this.range = range;
            this.numThreads = numThreads;
        }
    
        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(slaveSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(slaveSocket.getOutputStream())) {
                    
                out.writeObject(new Object[] {range, numThreads});
                // System.out.println("Slave calculated primes: " + primes);
                @SuppressWarnings("unchecked")
                List<Integer> primes = (List<Integer>) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public List<Integer> getPrimes() {
            return PrimeChecker.get_primes(range[0], range[1], numThreads);
        }
    }
}

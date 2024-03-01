import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PrimeServer {
    private static final int MASTER_SERVER_PORT = 12345;
    //private static final List<Integer> slavePorts = Collections.synchronizedList(new ArrayList<>());
    private static ExecutorService clientExecutor;
    private static ExecutorService slaveExecutor;
    public static int numSlaves = 0;
    public static  List<Integer> allPrimes = new ArrayList<>();
    public static int DoneCounter = 0;
    
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
            List<Integer> allPrimes_temp = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();
            int subrangeSize = totalRange / totalServers;
            int remainder = totalRange % totalServers;
            int currentStart = range[0];
        
            final int masterEnd = (remainder > 0) ? currentStart + subrangeSize - 1 : currentStart + subrangeSize;
            if (remainder > 0) {
                remainder--;
            }
            final int masterStart = currentStart;
            System.out.println("Master Server at port: "  + clientSocket.getLocalPort() + " received range: [" + currentStart + ", " + masterEnd + "]");
            threads.add(new Thread(() -> {allPrimes.addAll(PrimeChecker.get_primes(masterStart, masterEnd, numThreads));
            System.err.println("Master calulated primes: " + allPrimes.size());}));
            // System.out.println("Master calculated primes: " + allPrimes);
        
            currentStart = masterEnd + 1;
        
            for (int i = 0; i < numSlaves; i++) {
                int slaveEnd = currentStart + subrangeSize - 1;
                if (remainder > 0) {
                    slaveEnd += 1;
                    remainder--;
                }
                int[] slaveSubrange = new int[]{currentStart, slaveEnd};
                threads.add(distributeSubRangeToSlave(slaveSubrange, 12346, numThreads));
                //threads.get(i + 1).start();
                currentStart = slaveEnd + 1; 
            }
            for (Thread thread : threads){
                try {
                    thread.start();
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            while (DoneCounter < numSlaves) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            allPrimes_temp.addAll(allPrimes);
            System.out.println("All Primes: " + allPrimes_temp.size());
            return allPrimes_temp;
        }

    // Probably have to fix this method to accomodate the seperate slave servers
        
        private Thread distributeSubRangeToSlave(int[] subrange, int slavePort, int numThreads) {
            try (ServerSocket masterSocket = new ServerSocket(12346)){
                System.out.println("Slave Server started on port 12346");
                Socket slaveSocket = masterSocket.accept();
                System.out.println("Connected to Slave Server");
                Thread slaveThread = new Thread(new SlaveHandler(slaveSocket, subrange, numThreads));
                return slaveThread;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
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
                @SuppressWarnings("unchecked")
                Object[] receivedData = (Object[]) receiveAndDecompressData(slaveSocket);
                List<Integer> primes = Arrays.stream(receivedData)
                                            .filter(obj -> obj instanceof Integer)
                                            .map(obj -> (Integer) obj)
                                            .collect(Collectors.toList());

                System.out.println("Slave calculated primes: " + primes.size());
                allPrimes.addAll(primes);
                DoneCounter++;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private static Object receiveAndDecompressData(Socket socket) throws ClassNotFoundException {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            int length = dis.readInt();
            if (length > 0) {
                byte[] compressedData = new byte[length];
                dis.readFully(compressedData, 0, compressedData.length);
    
                try (ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
                     GZIPInputStream gzipIn = new GZIPInputStream(byteStream);
                     ObjectInputStream objectIn = new ObjectInputStream(gzipIn)) {
                    return objectIn.readObject();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    }
}

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

    public static void main(String[] args) {
        clientExecutor = Executors.newCachedThreadPool(); 
        slaveExecutor = Executors.newFixedThreadPool(2); 

        //startSlaveServer("Slave Server 1");
        //startSlaveServer("Slave Server 2");

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
    }
/*
    private static void startSlaveServer(String serverName) {
        slaveExecutor.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                int port = serverSocket.getLocalPort();
                synchronized (slavePorts) {
                    slavePorts.add(port);
                }
                System.out.println(serverName + " started on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new SlaveHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    } */

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
            int totalServers = 1 + 1; 
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
        
            for (int i = 0; i < 1; i++) {
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
            try (Socket slaveSocket = new Socket("localhost", slavePort);
                 ObjectOutputStream out = new ObjectOutputStream(slaveSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(slaveSocket.getInputStream())) {
        
                out.writeObject(new Object[] {subrange, numThreads}); 

                return (List<Integer>) in.readObject(); 
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return new ArrayList<>(); 
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
        private final Socket clientSocket;
    
        public SlaveHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
    
        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
    
                Object[] receivedData = (Object[]) in.readObject();
                int[] range = (int[]) receivedData[0]; 
                int numThreads = (Integer) receivedData[1]; 
                System.out.println("Slave Server at port: " + clientSocket.getLocalPort() + " received range: " + Arrays.toString(range));
    
                List<Integer> primes = PrimeChecker.get_primes(range[0], range[1], numThreads);
                // System.out.println("Slave calculated primes: " + primes);

                out.writeObject(primes); 
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

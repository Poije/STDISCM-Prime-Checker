import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class PrimeClient {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter the port number: ");
        int port = scanner.nextInt();
        
        Socket socket = new Socket("localhost", port);
        
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        System.out.print("Enter the lower bound: ");
        int start = scanner.nextInt();
        System.out.print("Enter the upper bound: ");
        int end = scanner.nextInt();
        System.out.print("Enter the number of threads: ");
        int numThreads = scanner.nextInt();

        out.writeObject(new Object[]{new int[]{start, end}, numThreads});

        try {
            Object[] receivedData = (Object[]) receiveAndDecompressData(socket);
            List<Integer> primes = (List<Integer>) receivedData[0];
            long startTime = (long) receivedData[1];

            long endTime = System.nanoTime();
            long elapsedTimeMillis = (endTime - startTime) / 1000000;

            System.out.println("Number of Primes: " + primes.size());
            System.out.println("Runtime: " + elapsedTimeMillis + " milliseconds");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            socket.close();
            scanner.close();
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class PrimeSlave {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Slave Started...");
        boolean connected = false;
        while (!connected) {
            try {
                Socket socket = new Socket("localhost", 12346); // Connect to server on localhost, port 12346
                System.out.println("Slave Connected to Server...");
                connected = true;

                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                try {
                    List<Integer> primes = new ArrayList<>();
                    Object[] data = (Object[]) in.readObject();
                    int[] range = (int[]) data[0];
                    int numThreads = (int) data[1];
                    primes = PrimeChecker.get_primes(range[0], range[1], numThreads);
                    System.out.println("Slave calculated primes: " + primes.size());
                    sendCompressedData(new Object[]{primes}, socket);
                    // out.writeObject(primes);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    socket.close();
                }
            } catch (ConnectException e) {
                Thread.sleep(1000); // Wait for 1 second before retrying
            }
        }
    }

    private static void sendCompressedData(Object data, Socket socket) {
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


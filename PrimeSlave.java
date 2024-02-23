import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class PrimeSlave {
    public static void main(String[] args) {
        System.out.print ("Wating for Master");
        Socket socket = new Socket("localhost", 12345); // Connect to server on localhost, port 12345
        
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());



        out.writeObject(new int[]{start, end});
    }
}

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServerSocketExample {
    private static final int PORT = 80;

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            // Event loop starts here
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                threadPool.execute(() -> handleClient(socket));
            }
            // Event loop ends here
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void handleClient(Socket socket) {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead = input.read(buffer);
            String request = new String(buffer, 0, bytesRead);
            System.out.println("Received: " + request);

            String httpResponse = "HTTP/1.1 200 OK\r\n\r\nHello, World!";
            output.write(httpResponse.getBytes("UTF-8"));
        } catch (IOException ex) {
            System.out.println("Client handler exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                System.out.println("Failed to close socket: " + ex.getMessage());
            }
        }
    }
}
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class WebServer {
    private static final int PORT = 8080; // we can also use 80, in that case we have to run the server as root
    private static final String RESPONSE = "HTTP/1.1 200 OK\r\n" +
                                           "Content-Length: 13\r\n" +
                                           "Content-Type: text/plain\r\n" +
                                           "\r\n" +
                                           "Hello, World!";

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    accept(key, selector);
                } else if (key.isReadable()) {
                    read(key);
                }
            }
        }
    }

    private static void accept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private static void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            socketChannel.close();
            return;
        }

        buffer.flip();
        socketChannel.write(ByteBuffer.wrap(RESPONSE.getBytes()));
        socketChannel.close();
    }
}
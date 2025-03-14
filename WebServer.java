import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebServer {
    private final int port;
    private final ExecutorService threadPool;
    private final Selector selector;
    private volatile boolean isRunning = true;
    private final Queue <Runnable> selectorTasks = new ConcurrentLinkedQueue<>();

    public WebServer(int port, int threadPoolSize) throws IOException {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.selector = Selector.open();
    }

    public void start() throws IOException{
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println( "Server is listening to port: " + port);

        while(isRunning){
            try{
                processPendingTasks();
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while(keys.hasNext()){
                    SelectionKey key = keys.next();
                    keys.remove();

                    try{
                        if(!key.isValid()){
                            continue;
                        }

                        if(key.isAcceptable()){
                            handleAccept(serverChannel);
                        }else if(key.isReadable()){
                            handleRead(key);
                        }
                    } catch(Exception e){
                        System.err.println("Error in handling key: " + e.getMessage());
                        safelyCloseConnection(key);
                    }
                }
            }catch(Exception e){
                System.err.println("Error in main loop: " + e.getMessage());
            }
        }
    }

    private void handleAccept(ServerSocketChannel serverChannel) throws IOException{
        SocketChannel clientChannel = serverChannel.accept();
        if(clientChannel != null){
            clientChannel.configureBlocking(false);
            SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ);
            RequestContext context = new RequestContext();
            key.attach(context);
            System.out.println("New client connected: " + clientChannel.getRemoteAddress());
        }
    }

    private void handleRead(SelectionKey key){
        threadPool.submit(() -> {
            try{
                processRequest(key);
            }catch(Exception e){
                System.err.println("Error in reading from client: " + e.getMessage());
                safelyCloseConnection(key);
            }
        });
    }

    private void processRequest(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        RequestContext context = (RequestContext) key.attachment();
        ByteBuffer buffer = context.buffer;

        try{
            int bytesRead = clientChannel.read(buffer);
            if(bytesRead == -1){
                safelyCloseConnection(key);
                return;
            }

            buffer.flip();
            byte [] data = new byte[buffer.remaining()];
            buffer.get(data);
            buffer.clear();

            context.requestBuilder.append(new String(data));
            String requestContent = context.requestBuilder.toString();

            if(requestContent.contains("\r\n\r\n")){
                String responce = createHttpResponse("Welcome to the server!");
                writeResponse(clientChannel, responce);
                context.requestBuilder.setLength(0);
            }

            selectorTasks.offer(() -> {
                try{
                    if(key.isValid()){
                        key.interestOps(SelectionKey.OP_READ);
                        selector.wakeup();
                    }
                }catch(Exception e){
                    safelyCloseConnection(key);
                }
            });

        } catch (IOException e){
            safelyCloseConnection(key);
        }
    }

    private void writeResponse(SocketChannel channel, String response) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
        while(buffer.hasRemaining()){
            channel.write(buffer);
        }
    }

    private static class RequestContext{
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        StringBuilder requestBuilder = new StringBuilder();
        // boolean headerComplere = false;
        // int contentLength = 0;
    }

    private String createHttpResponse(String content) {
        String html = "<html><body><h1>" + content + "</h1></body></html>";
        return "HTTP/1.1 200 OK\r\n" +
               "Content-Type: text/html\r\n" +
               "Content-Length: " + html.length() + "\r\n" +
               "Connection: keep-alive\r\n" +
               "Cache-Control: no-cache\r\n" +
               "\r\n" +
               html;
    }

    private void safelyCloseConnection(SelectionKey key){
        selectorTasks.offer(() -> {
            try{
                if(key.isValid()){
                    key.cancel();
                }
                key.channel().close();
            }catch (IOException ignored) {}

            selector.wakeup();
        });
    }

    private void processPendingTasks(){
        Runnable task;
        while((task = selectorTasks.poll()) != null){
            task.run();
        }
    }

    //  public static void main(String[] args) throws IOException {
    //     int port = 8080;
    //     int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    //     HybridServer server = new HybridServer(port, threadPoolSize);
    //
    //     // Start the server in a separate thread
    //     Thread serverThread = new Thread(() -> {
    //         try {
    //             server.start();
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }
    //     });
    //     serverThread.start();
    //
    //     // Add a shutdown hook to stop the server gracefully
    //     Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    //         System.out.println("Shutting down server...");
    //         server.stop();
    //     }));
    //
    //     // Simulate running for a while and then stopping the server
    //     try {
    //         Thread.sleep(60000); // Run for 60 seconds
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    //
    //     // Stop the server programmatically
    //     server.stop();
    // }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        WebServer server = new WebServer(port, threadPoolSize);
        server.start();
    }
}
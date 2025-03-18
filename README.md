# High-Performance Java NIO Web Server

This is a lightweight and efficient web server built from scratch using Java NIO (Non-blocking I/O). The server is designed to handle multiple concurrent client connections using a selector-based event-driven architecture and a thread pool for request processing.


## Benchmarking with `wrk`

To measure the server's performance, we used `wrk`, a high-performance HTTP benchmarking tool. Below are the results of a **30-second** test with **12 threads and 400 connections**:

### Benchmarking Command:
```bash
wrk -t12 -c400 -d30s http://127.0.0.1:8080/index.html
```

### Results:
```
Running 30s test @ http://127.0.0.1:8080/index.html
  12 threads and 400 connections
  Thread Stats   Avg      Stdev     Max  +/- Stdev
    Latency      74.34ms  161.29ms  1.94s  85.71%
    Req/Sec      17.06k   11.59k    60.31k   66.99%
  352,359 requests in 30.10s, 571.22MB read
  Requests/sec:  117,054.81
  Transfer/sec:  18.98MB
  Socket errors: connect 0, read 1, write 0, timeout 286
```

### Key Metrics:

- **Average Requests per Second:** **117,054.81**
- **Peak Requests per Second:** **60.31k per thread**
- **Latency:** **74.34ms (avg)**
- **Total Requests Processed:** **352,359 in 30s**
- **Data Transferred:** **571.22MB**

These results demonstrate the efficiency of this Java-based web server in handling high-concurrency workloads.

### Benchmarking Screenshot:
![Benchmarking Results](Screenshot_20250318_211349.png)

## How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/Sabbir0074/WebServerUsingJava.git
   cd WebServerUsingJava
   ```
2. Compile and run the server:
   ```bash
   javac WebServer.java
   java WebServer
   ```
3. The server will start listening on **port 8080**.

## Future Enhancements

- Support for **HTTPS (TLS)**
- Improved request parsing and support for **RESTful APIs**
- Logging and monitoring tools integration

---

🚀 **Happy Coding!**


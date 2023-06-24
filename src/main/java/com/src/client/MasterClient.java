package com.src.client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import com.src.utils.Command;
import com.src.utils.SocketHandler;

public class MasterClient {
    Hashtable<String, SocketHandler> handlers = new Hashtable<String, SocketHandler>();
    ReentrantLock lock = new ReentrantLock();
    String resourcePath = "";

    static Logger logger = Logger.getLogger("Client");

    public MasterClient(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void addConnection(String address, int port) throws UnknownHostException, IOException {
        lock.lock();
        try {
            handlers.put(address, new SocketHandler(address, 10325, (arg) -> {return this.handleResponse(arg); }));
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }

    public boolean running() {
        for (SocketHandler handler : handlers.values())
            if (handler.running())
                return true;

        return false;
    };

    public void start() throws Exception {
        List<String> addresses = Collections.list(handlers.keys());
        ArrayList<SocketHandler> values = new ArrayList<>(handlers.values());
        
        for (SocketHandler handler : values)
            handler.start();

        // Initialize
        {
            sendToAll(Command.INITIALIZE.label());
            sendToAll(String.join(";", addresses));
            sendToAll(Command.END.label());
        }

        // Splitting & Mapping
        {
            sendToAll(Command.MAPPING.label());

            BufferedReader reader = new BufferedReader(new FileReader(resourcePath));
            String line = reader.readLine();    
            int index = 0;

            while (line != null) {
                values.get(index).send(line);
                index = (index + 1) % values.size();
                line = reader.readLine();
            }

            reader.close();

            sendToAll(Command.END.label());
        }

        // Shuffling & Reducing
        {
            sendToAll(Command.SHUFFLING.label());
            sendToAll(Command.QUIT.label());
        }
    }

    public void stop() {
        for (SocketHandler handler : handlers.values())
            handler.stopRunning();
    }

    public String handleResponse(String content) {
        lock.lock();

        try {
            // ...
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }

        return "";
    }

    private void sendToAll(String arg) {
        for (SocketHandler handler : handlers.values())
            handler.send(arg);
    }
}
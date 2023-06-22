package com.src;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

public class MasterClient {
    Hashtable<String, ClientSocketHandler> handlers = new Hashtable<String, ClientSocketHandler>();
    ReentrantLock lock = new ReentrantLock();
    String resourcePath = "";

    public MasterClient(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void addConnection(String address, int port) throws UnknownHostException, IOException {
        lock.lock();
        try {
            handlers.put(address, new ClientSocketHandler(this, address, 10325));
        } finally {
            lock.unlock();
        }
    }

    public boolean running() {
        for (ClientSocketHandler handler : handlers.values())
            if (handler.running())
                return true;

        return false;
    };

    public void start() throws Exception {
        String[] addresses = (String[]) Collections.list(handlers.keys()).toArray();
        for (ClientSocketHandler handler : handlers.values()) {
            handler.initialize(addresses);
            handler.start();
        }

        // Read file
        {
            BufferedReader reader = new BufferedReader(new FileReader(resourcePath));
            String line = reader.readLine();

            ArrayList<ClientSocketHandler> values = new ArrayList<>(handlers.values());
            int index = 0;

            while (line != null) {
                values.get(index).send(line);
                index = (index + 1) % values.size();
                line = reader.readLine();
            }

            reader.close();
        }

        //...
    }

    public void stop() {
        for (ClientSocketHandler handler : handlers.values())
            handler.stopRunning();
    }

    public String handleResponse(String content) {
        lock.lock();

        try {
            // ...
        } finally {
            lock.unlock();
        }

        return new String("");
    }
}
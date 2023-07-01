package com.src.client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import com.src.utils.Command;
import com.src.utils.SocketHandler;

public class MasterClient {
    Hashtable<String, SocketHandler> handlerTable = new Hashtable<String, SocketHandler>();
    String resourcePath = "";

    static Logger logger = Logger.getLogger("Client");

    public MasterClient(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public synchronized void addConnection(String address, int port) throws UnknownHostException, IOException {
        try {
            handlerTable.put(address + ":" + port, new SocketHandler(new Socket(address, port), (arg) -> {
                this.handleResponse(arg);
            }));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean running() {
        for (SocketHandler handler : handlerTable.values())
            if (handler.running())
                return true;

        return false;
    };

    public void start() throws Exception {
        List<String> addresses = Collections.list(handlerTable.keys());

        handlerTable.forEach((address, handler) -> {
            handler.start();
        });

        // Initialize
        {
            sendToAll(Command.INITIALIZE.label());
            handlerTable.forEach((address, handler) -> {
                handler.send("-a=" + address + " -s=" + String.join(";", addresses));
            });
            // for (SocketHandler handler : handlers.values())
            // handler.send(arg);
            // sendToAll(new String[]{"asd", "asd"});
            sendToAll(Command.END.label());
        }

        // Splitting & Mapping
        {
            sendToAll(Command.MAPPING.label());
            splitData();
            sendToAll(Command.END.label());
        }

        // Shuffling & Reducing
        {
            sendToAll(Command.SHUFFLING.label());
            sendToAll(Command.QUIT.label());
        }

        handlerTable.forEach((address, handler) -> {
            String line = handler.read();
            while (line != null && !line.equals(Command.END.label()))
                ;
        });
    }

    public synchronized void stop() {
        handlerTable.forEach((address, handler) -> {
            handler.stopRunning();
        });
    }

    public synchronized String handleResponse(String content) {
        //lock.lock();

        try {
            // ...
        } finally {
            //if (lock.isHeldByCurrentThread())
            //    lock.unlock();
        }

        return "";
    }

    private synchronized void sendToAll(String arg) {
        handlerTable.forEach((address, handler) -> {
            handler.send(arg);
        });
    }

    private void splitData() {
        try {
            ArrayList<SocketHandler> handlers = new ArrayList<>(handlerTable.values());
            File file = new File(resourcePath);
            long splitSize = file.length() / handlers.size() + 1;

            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            char[] buffer = new char[(int) splitSize];

            for (int i = 0; i < handlers.size(); i++) {
                int bytesRead = fileReader.read(buffer);

                if (bytesRead == -1)
                    break;

                String data = null;

                if (bytesRead < splitSize) {
                    data = new String(buffer);
                } else {
                    StringBuilder sb = new StringBuilder(new String(buffer));

                    char ch = '0';
                    do {
                        ch = (char) fileReader.read();

                        if (ch == -1)
                            break;

                        sb.append(ch);
                    } while (ch != ' ' && ch != '\n');

                    data = sb.toString();
                }

                SocketHandler handler = handlers.get(i);
                handler.send("-sp=" + Long.toString(data.length()));
                handler.send(data);
            }

            fileReader.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
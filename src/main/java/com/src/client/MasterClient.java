package com.src.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.src.utils.Command;
import com.src.utils.SocketHandler;

public class MasterClient {
    private Hashtable<String, SocketHandler> handlerTable = new Hashtable<String, SocketHandler>();
    private String resourcePath = "";
    private final AtomicReference<Command> nextCommand = new AtomicReference<>(Command.INITIALIZE);
    private final AtomicInteger finishedCount = new AtomicInteger(0);
    private TreeSet<Integer> counts = new TreeSet<>();

    static Logger logger = Logger.getLogger("Master");

    public MasterClient(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public synchronized void addConnection(String address, int port) throws UnknownHostException, IOException {
        try {
            handlerTable.put(address + ":" + port, new SocketHandler(new Socket(address, port), (arg) -> {
                this.handleResponse(arg);
            }));
        } catch (Exception e) {
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
            sendToAll(Command.END.label());

            nextCommand.set(Command.MAPPING);
        }

        // Splitting & Mapping
        {
            sendToAll(Command.MAPPING.label());
            splitData();
            sendToAll(Command.END.label());

            nextCommand.set(Command.SHUFFLING);
        }

        // Shuffling & Reducing
        {
            sendToAll(Command.SHUFFLING.label());

            nextCommand.set(Command.SORT_MAPPING);
        }

        // Sorting (Mapping)
        {
            sendToAll(Command.SORT_MAPPING.label());
            while (nextCommand.get() == Command.SORT_MAPPING)
                ;
            sendToAll(Command.END.label());

            nextCommand.set(Command.SORT_SHUFFLING);
        }

        // Sorting (Shuffling & Reducing)
        {
            sendToAll(Command.SORT_SHUFFLING.label());

            Integer splitSize = counts.size() / handlerTable.size();
            ArrayList<Integer> thresholds = new ArrayList<>(counts);

            ArrayList<String> ranges = new ArrayList<>();
            for (int i = 0; i < handlerTable.size(); ++i) {
                Integer index = (i + 1) * splitSize - 1;
                String threshold = Integer
                        .toString((i == (handlerTable.size() - 1)) ? thresholds.get(thresholds.size() - 1)
                                : thresholds.get(index));
                String address = addresses.get(i);
                ranges.add(address + "=" + threshold);
            }

            logger.log(Level.INFO, "master : The following ranging (server=frequency threshold) was calculated:");
            ranges.forEach((range) -> {
                logger.log(Level.INFO, "master: " + range);
            });

            sendToAll(String.join(" ", ranges));

            nextCommand.set(Command.QUIT);
        }

        // Quit
        {
            sendToAll(Command.QUIT.label());
        }

        handlerTable.forEach((address, handler) -> {
            String line = handler.read();

            while (line != null && !line.equals(Command.END.label()));
        });
    }

    public synchronized void stop() {
        handlerTable.forEach((address, handler) -> {
            handler.stopRunning();
        });
    }

    public synchronized String handleResponse(String data) {

        if (nextCommand.get() == Command.SORT_MAPPING) {
            String[] range = data.split(";");

            for (int i = 0; i < range.length; ++i)
                counts.add(Integer.parseInt(range[i]));

            finishedCount.incrementAndGet();
            if (finishedCount.get() == handlerTable.size()) {
                finishedCount.set(0);
                nextCommand.set(Command.SORT_SHUFFLING);
            }
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

                    // Get the whole word
                    char ch = (char) fileReader.read();
                    while (ch != ' ' && ch != '\n') {
                        sb.append(ch);
                        ch = (char) fileReader.read();
                    }

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
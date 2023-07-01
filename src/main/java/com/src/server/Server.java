package com.src.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.src.utils.Command;
import com.src.utils.LogFormatter;
import com.src.utils.SocketHandler;
import com.src.utils.SocketIO;

public class Server {
    private HashMap<String, SocketHandler> handlerTable = new HashMap<String, SocketHandler>();
    private HashMap<String, Integer> shufflingWordCounter = new HashMap<String, Integer>();
    private HashMap<String, Integer> wordCounter = new HashMap<String, Integer>();
    private final AtomicInteger endCount = new AtomicInteger(0);
    private final AtomicInteger okCount = new AtomicInteger(0);
    private final AtomicReference<Command> nextCommand = new AtomicReference<>(Command.INITIALIZE);
    private Logger logger = Logger.getLogger("Server");
    private String localAddress = null;
    private String[] args = null;

    public Server(String[] args) {
        this.args = args.clone();
    }

    public void start() {
        LogManager.getLogManager().reset();
        logger.setLevel(Level.INFO);

        logger.addHandler(new ConsoleHandler());

        try {
            Handler fileHandler = new FileHandler("./client.log", 2000, 5);
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Handler handler : logger.getHandlers())
            handler.setFormatter(new LogFormatter());

        ServerSocket server = null;

        Options options = new Options();
        Option portOpt = new Option("p", "port", true, "");
        options.addOption(portOpt);
        Option serversOpt = new Option("s", "servers", true, "");
        options.addOption(serversOpt);
        Option addressOpt = new Option("a", "address", true, "");
        options.addOption(addressOpt);
        Option splitSizeOpt = new Option("sp", "splitsize", true, "");
        options.addOption(splitSizeOpt);

        Option wordOpt = new Option("w", "word", true, "");
        options.addOption(wordOpt);
        Option countOpt = new Option("c", "count", true, "");
        options.addOption(countOpt);

        logger.log(Level.INFO, String.join(" ", args));

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args, false);
            Integer localPort = Integer.parseInt(cmd.getOptionValue("port", "10325"));

            logger.log(Level.INFO, "Starting server program on port " + Integer.toString(localPort) + " ...");

            server = new ServerSocket(localPort);
            SocketIO msio = new SocketIO(server.accept());

            while (true) {
                logger.log(Level.INFO, (localAddress == null ? "" : localAddress) + " : Waiting for the master");
                String line = msio.is.readLine();
                logger.log(Level.INFO, (localAddress == null ? "" : localAddress) + " : From master: " + line);

                if (nextCommand.get() == Command.INITIALIZE && line.equals(Command.INITIALIZE.label())) {

                    // List of other servers
                    line = msio.is.readLine();
                    logger.log(Level.INFO, (localAddress == null ? "" : localAddress) + "From master: " + line);

                    cmd = parser.parse(options, line.split(" "), false);
                    localAddress = cmd.getOptionValue("address");
                    String[] addresses = cmd.getOptionValue("servers").split(";");

                    for (int i = 0; i < addresses.length; ++i) {
                        if (addresses[i].equals(localAddress)) {
                            handlerTable.put(addresses[i], null);

                            for (int j = i + 1; j < addresses.length; ++j) {
                                String[] pair = addresses[j].split(":");
                                String address = pair[0];
                                Integer port = Integer.parseInt(pair[1]);

                                createHandler(addresses[j], new Socket(address, port));
                            }

                            for (int j = addresses.length - 1; j > i; --j)
                                handlerTable.get(addresses[j]).send(Command.END.label());

                            break;
                        } else {
                            SocketHandler sh = createHandler(addresses[i], server.accept());

                            do {
                                line = sh.read();
                            } while (line == null || !line.equals(Command.END.label()));
                        }
                    }

                    while (!msio.is.readLine().equals(Command.END.label()))
                        ;

                    nextCommand.set(Command.MAPPING);

                } else if (nextCommand.get() == Command.MAPPING && line.equals(Command.MAPPING.label())) {

                    while (!(line = msio.is.readLine()).equals(Command.END.label())) {
                        cmd = parser.parse(options, line.split(" "), false);
                        int bytesLeft = Integer.parseInt(cmd.getOptionValue("sp")) + 1;

                        logger.log(Level.INFO, localAddress + " : Bytes to get : " + Integer.toString(bytesLeft));

                        char[] buffer = new char[bytesLeft];
                        String data = "";

                        while (bytesLeft > 0) {
                            int receivedBytes = msio.is.read(buffer, 0, bytesLeft);
                            bytesLeft -= receivedBytes;
                            data = data.concat(new String(buffer, 0, receivedBytes));

                            String[] words = data.split("[\\s+|\\r?\\n|\\x00]");
                            if (words.length > 0) {
                                for (int i = 0; i < words.length - 1; ++i) {
                                    if (words[i].length() > 0)
                                        shufflingWordCounter.compute(words[i], (key, val) -> (val == null) ? 1
                                                : val + 1);
                                }

                                data = words[words.length - 1];
                            } else {
                                data = "";
                            }
                        }

                        if (!data.isEmpty()) {
                            String[] words = data.split("[\\s+|\\r?\\n|\\x00]");
                            if (words.length > 0) {
                                for (int i = 0; i < words.length; ++i) {
                                    if (words[i].length() > 0)
                                        shufflingWordCounter.compute(words[i], (key, val) -> (val == null) ? 1
                                                : val + 1);
                                }
                            }
                        }
                    }

                    nextCommand.set(Command.SHUFFLING);

                } else if (nextCommand.get() == Command.SHUFFLING && line.equals(Command.SHUFFLING.label())) {

                    shufflingWordCounter.forEach((word, count) -> {
                        Integer index = Math.abs(word.hashCode()) % handlerTable.size();

                        try {
                            SocketHandler handler = new ArrayList<>(handlerTable.values()).get(index);
                            String data = word + "<=!=>" + count;

                            if (handler == null) {
                                processData(data);
                            } else {
                                handler.send(data);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    shufflingWordCounter.clear();

                    handlerTable.forEach((address, handler) -> {
                        if (handler == null)
                            return;

                        handler.send(Command.END.label());
                    });

                    while (nextCommand.get() == Command.SHUFFLING)
                        ;

                } else if (line == null || (nextCommand.get() == Command.QUIT && line.equals(Command.QUIT.label()))) {
                    BufferedWriter os = new BufferedWriter(new FileWriter("res.txt"));

                    wordCounter.forEach((word, count) -> {
                        try {
                            os.write(word + " " + Integer.toString(count));
                            os.newLine();
                            os.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    os.close();

                    msio.os.write(Command.END.label());
                    msio.os.newLine();
                    msio.os.flush();
                    break;
                }
            }

            server.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            logger.log(Level.INFO, localAddress + " : Stopping server program...");

            handlerTable.forEach((key, handler) -> {
                if (handler == null)
                    return;

                handler.stopRunning();
            });
        }

        logger.log(Level.INFO, localAddress + " : Server programm stopped.");
    }

    private SocketHandler createHandler(String address, Socket socket) throws UnknownHostException, IOException {

        SocketHandler sh = new SocketHandler(socket, (data) -> {
            String reply = processData(data);

            if (!reply.isEmpty()) {
                logger.log(Level.INFO, localAddress + " : Reply : " + reply + " to " + address);
                handlerTable.get(address).send(reply);
            }
        });

        handlerTable.put(address, sh);
        sh.start();

        return sh;
    }

    private synchronized String processData(String data) {
        if (nextCommand.get() == Command.SHUFFLING) {

            if (data.equals(Command.END.label())) {

                endCount.incrementAndGet();
                if (endCount.get() == (handlerTable.size() - 1) && okCount.get() == endCount.get()) {
                    endCount.set(0);
                    okCount.set(0);
                    nextCommand.set(Command.QUIT);
                }

                return Command.OK.label();
            } else if (data.equals(Command.OK.label())) {

                okCount.incrementAndGet();
                if (okCount.get() == (handlerTable.size() - 1) && okCount.get() == endCount.get()) {
                    endCount.set(0);
                    okCount.set(0);
                    nextCommand.set(Command.QUIT);
                }

            } else {
                String[] pair = data.split("<=!=>");

                if (pair.length == 2) {
                    String word = pair[0];
                    Integer count = Integer.parseInt(pair[1]);
                    wordCounter.compute(word, (key, val) -> (val == null ? count : val + count));
                }
            }
        }

        return "";
    }
}

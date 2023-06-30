package com.src.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    private HashMap<String, SocketIO> sios = new HashMap<String, SocketIO>();
    private HashMap<String, Integer> shufflingWordCounter = new HashMap<String, Integer>();
    private HashMap<String, Integer> wordCounter = new HashMap<String, Integer>();
    private final AtomicInteger finishedCount = new AtomicInteger(0);
    private String localAddress = null;
    private final AtomicReference<Command> nextCommand = new AtomicReference<>(Command.INITIALIZE);
    private Logger logger = Logger.getLogger("Server");
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

                    for (String adr : addresses) {
                        String[] pair = adr.split(":");
                        String address = pair[0];
                        Integer port = Integer.parseInt(pair[1]);

                        if (adr.equals(localAddress)) {
                            handlerTable.put(adr, null);
                            continue;
                        }

                        SocketHandler sh = new SocketHandler(address, port, (data) -> {
                            String reply = processData(data);

                            if (nextCommand.get() == Command.SHUFFLING && reply.equals(Command.OK.label())) {
                                logger.log(Level.INFO, localAddress + " : Ok : " + adr);
                                SocketIO sio = sios.get(adr);
                                
                                try {
                                    sio.os.write(reply);
                                    sio.os.newLine();
                                    sio.os.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        handlerTable.put(adr, sh);
                        sh.start();
                    }

                    for (int i = 0; i < addresses.length; ++i) {

                        if (addresses[i].equals(localAddress)) {
                            sios.put(addresses[i], null);
                            continue;
                        }

                        sios.put(addresses[i], new SocketIO(server.accept()));
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
                    }

                    nextCommand.set(Command.SHUFFLING);

                } else if (nextCommand.get() == Command.SHUFFLING && line.equals(Command.SHUFFLING.label())) {

                    shufflingWordCounter.forEach((word, count) -> {
                        Integer index = Math.abs(word.hashCode()) % sios.size();

                        try {
                            SocketIO ios = new ArrayList<>(sios.values()).get(index);
                            String data = word + "<=!=>" + count;

                            if (ios == null) {
                                processData(data);
                            } else {
                                ios.os.write(data);
                                ios.os.newLine();
                                ios.os.flush();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    shufflingWordCounter.clear();

                    sios.forEach((address, sio) -> {
                        try {
                            if (sio == null) {
                                processData(Command.OK.label());
                                return;
                            }

                            sio.os.write(Command.END.label());
                            sio.os.newLine();
                            sio.os.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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

            // server.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            logger.log(Level.INFO, localAddress + " : Stopping server program...");

            handlerTable.forEach((key, handler) -> {
                if (handler == null)
                    return;

                handler.stopRunning();
            });

            sios.forEach((key, sio) -> {
                try {
                    if (sio == null)
                        return;

                    sio.is.close();
                    sio.os.close();
                    sio.socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }

        logger.log(Level.INFO, localAddress + " : Server programm stopped.");
    }

    private synchronized String processData(String data) {
        if (nextCommand.get() == Command.SHUFFLING) {

            if(data.equals(Command.OK.label()))
                logger.log(Level.INFO, localAddress + " : kek2 : " + data);

            if (data.equals(Command.END.label())) {

                logger.log(Level.INFO, localAddress + " : kek1 : " + data);

                return Command.OK.label();
            } else if (data.equals(Command.OK.label())) {
                finishedCount.incrementAndGet();

                if (finishedCount.compareAndSet(handlerTable.size(), 0)) {
                    logger.log(Level.INFO, localAddress + " : Next state: " +
                            Command.QUIT.label());
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

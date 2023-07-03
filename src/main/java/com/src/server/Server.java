package com.src.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.StringJoiner;
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
import org.apache.commons.cli.ParseException;

import com.src.utils.Command;
import com.src.utils.LogFormatter;
import com.src.utils.SocketHandler;
import com.src.utils.SocketIO;

public class Server {
    private HashMap<String, SocketHandler> handlerTable = new HashMap<String, SocketHandler>();
    private HashMap<String, Integer> shufflingWordCounter = new HashMap<String, Integer>();
    private HashMap<String, Integer> mappingWordCounter = new HashMap<String, Integer>();

    private TreeMap<Integer, ArrayList<String>> sortMappingWordCounter = new TreeMap<Integer, ArrayList<String>>();
    private TreeMap<Integer, ArrayList<String>> sortShufflingWordCounter = new TreeMap<Integer, ArrayList<String>>();

    private final AtomicInteger endCount = new AtomicInteger(0);
    private final AtomicInteger okCount = new AtomicInteger(0);
    private final AtomicReference<Command> nextCommand = new AtomicReference<>(Command.INITIALIZE);

    private Logger logger = Logger.getLogger("Server");
    private String localAddress = "";
    private String[] args = null;

    private Options options = new Options();
    private CommandLineParser parser = new DefaultParser();

    public Server(String[] args) {
        this.args = args.clone();
    }

    public void start() {
        LogManager.getLogManager().reset();
        logger.setLevel(Level.INFO);

        logger.addHandler(new ConsoleHandler());

        try {
            Handler fileHandler = new FileHandler("./server.log", 2000, 5);
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Handler handler : logger.getHandlers())
            handler.setFormatter(new LogFormatter());

        ServerSocket server = null;

        Option portOpt = new Option("p", "port", true, "");
        options.addOption(portOpt);

        Option serversOpt = new Option("s", "servers", true, "");
        options.addOption(serversOpt);

        Option addressOpt = new Option("a", "address", true, "");
        options.addOption(addressOpt);

        Option splitSizeOpt = new Option("sp", "splitsize", true, "");
        options.addOption(splitSizeOpt);

        Option wordsOpt = new Option("w", "words", true, "");
        options.addOption(wordsOpt);

        Option countOpt = new Option("c", "count", true, "");
        options.addOption(countOpt);

        try {
            CommandLine cmd = parser.parse(options, args, false);
            Integer localPort = Integer.parseInt(cmd.getOptionValue("port", "10325"));

            logger.log(Level.INFO, "Starting server program on port " + Integer.toString(localPort) + " ...");

            server = new ServerSocket(localPort);
            SocketIO msio = new SocketIO(server.accept());

            while (true) {
                logger.log(Level.INFO, localAddress + " : Waiting for the master");
                String line = msio.is.readLine();
                logger.log(Level.INFO, localAddress + " : From master: " + line);

                if (line == null || (nextCommand.get() == Command.QUIT && line.equals(Command.QUIT.label()))) {
                    msio.os.write(Command.END.label());
                    msio.os.newLine();
                    msio.os.flush();
                    break;
                } else if (nextCommand.get() == Command.INITIALIZE && line.equals(Command.INITIALIZE.label())) {

                    // List of other servers
                    line = msio.is.readLine();
                    logger.log(Level.INFO, localAddress + " : From master: " + line);

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
                                        mappingWordCounter.compute(words[i], (key, val) -> (val == null) ? 1
                                                : val + 1);
                                }

                                data = words[words.length - 1];
                            } else {
                                data = "";
                            }
                        }

                        // If some data left
                        if (!data.isEmpty()) {
                            String[] words = data.split("[\\s+|\\r?\\n|\\x00]");
                            if (words.length > 0) {
                                for (int i = 0; i < words.length; ++i) {
                                    if (words[i].length() > 0)
                                        mappingWordCounter.compute(words[i], (key, val) -> (val == null) ? 1
                                                : val + 1);
                                }
                            }
                        }
                    }

                    nextCommand.set(Command.SHUFFLING);

                } else if (nextCommand.get() == Command.SHUFFLING && line.equals(Command.SHUFFLING.label())) {

                    mappingWordCounter.forEach((word, count) -> {
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

                    mappingWordCounter.clear();

                    handlerTable.forEach((address, handler) -> {
                        if (handler == null)
                            return;

                        handler.send(Command.END.label());
                    });

                    while (nextCommand.get() == Command.SHUFFLING)
                        ;

                } else if (nextCommand.get() == Command.SORT_MAPPING && line.equals(Command.SORT_MAPPING.label())) {
                    shufflingWordCounter.forEach((word, count) -> {
                        if (sortMappingWordCounter.containsKey(count)) {
                            sortMappingWordCounter.get(count).add(word);
                        } else {
                            sortMappingWordCounter.put(count, new ArrayList<String>() {
                                {
                                    add(word);
                                }
                            });
                        }
                    });

                    shufflingWordCounter.clear();

                    StringJoiner joiner = new StringJoiner(";");
                    sortMappingWordCounter.keySet().forEach((val) -> {
                        joiner.add(Integer.toString(val));
                    });

                    msio.os.write(joiner.toString());
                    msio.os.newLine();
                    msio.os.flush();

                    while (!msio.is.readLine().equals(Command.END.label()))
                        ;

                    nextCommand.set(Command.SORT_SHUFFLING);

                } else if (nextCommand.get() == Command.SORT_SHUFFLING && line.equals(Command.SORT_SHUFFLING.label())) {

                    TreeMap<Integer, String> thresholds = new TreeMap<>();

                    // The data we expect: server1=10 server2=19 server3=43
                    // It means that
                    // server1 is responsible for the [1, 10] subrange
                    // server2 is responsible for the [11, 19] subrange
                    // server3 is responsible for the [20, 43] subrange
                    line = msio.is.readLine();

                    String[] pairs = line.split(" ");
                    for (String pair : pairs) {
                        String[] data = pair.split("=");
                        String address = data[0];
                        Integer threshold = Integer.parseInt(data[1]);

                        thresholds.put(threshold, address);
                    }

                    Iterator<Entry<Integer, String>> thIt = thresholds.entrySet().iterator();
                    Entry<Integer, String> thEntry = thIt.next();

                    Iterator<Entry<Integer, ArrayList<String>>> wIt = sortMappingWordCounter.entrySet().iterator();
                    while (wIt.hasNext()) {
                        Entry<Integer, ArrayList<String>> wEntry = wIt.next();
                        Integer count = wEntry.getKey();
                        ArrayList<String> words = wEntry.getValue();

                        while (thEntry.getKey() < count)
                            thEntry = thIt.next();

                        String data = "-w=" + String.join("<=!=>", words);
                        data = "-c=" + Integer.toString(count) + " " + data;

                        SocketHandler handler = handlerTable.get(thEntry.getValue());

                        if (handler == null) {
                            processData(data);
                        } else {
                            handler.send(data);
                        }
                    }

                    sortMappingWordCounter.clear();

                    handlerTable.forEach((address, handler) -> {
                        if (handler == null)
                            return;

                        handler.send(Command.END.label());
                    });

                    while (nextCommand.get() == Command.SORT_SHUFFLING)
                        ;

                } else {
                    logger.log(Level.INFO, localAddress + " : Unexpected data line : " + line);
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
                logger.log(Level.INFO, localAddress + " : Reply " + reply + " to " + address);
                handlerTable.get(address).send(reply);
            }
        });

        handlerTable.put(address, sh);
        sh.start();

        return sh;
    }

    private synchronized void processEndCounter(Command nextCommand) {
        endCount.incrementAndGet();
        if (endCount.get() == (handlerTable.size() - 1) && okCount.get() == endCount.get()) {
            endCount.set(0);
            okCount.set(0);
            this.nextCommand.set(nextCommand);
        }
    }

    private synchronized void processOkCounter(Command nextCommand) {
        okCount.incrementAndGet();
        if (okCount.get() == (handlerTable.size() - 1) && okCount.get() == endCount.get()) {
            endCount.set(0);
            okCount.set(0);
            this.nextCommand.set(nextCommand);
        }
    }

    private synchronized String processData(String data) {
        if (nextCommand.get() == Command.SHUFFLING) {

            if (data.equals(Command.END.label())) {

                processEndCounter(Command.SORT_MAPPING);
                return Command.OK.label();
                
            } else if (data.equals(Command.OK.label())) {

                processOkCounter(Command.SORT_MAPPING);

            } else {
                String[] pair = data.split("<=!=>");

                if (pair.length == 2) {
                    String word = pair[0];
                    Integer count = Integer.parseInt(pair[1]);
                    shufflingWordCounter.compute(word, (key, val) -> (val == null ? count : val + count));
                }
            }
        } else if (nextCommand.get() == Command.SORT_SHUFFLING) {

            if (data.equals(Command.END.label())) {

                processEndCounter(Command.QUIT);
                return Command.OK.label();

            } else if (data.equals(Command.OK.label())) {

                processOkCounter(Command.QUIT);

            } else {
                try {
                    CommandLine cmd = parser.parse(options, data.split(" "), false);

                    Integer count = Integer.parseInt(cmd.getOptionValue("c"));
                    String[] words = cmd.getOptionValue("w").split("<=!=>");

                    for (int i = 0; i < words.length; ++i) {
                        String word = words[i];

                        if (sortShufflingWordCounter.containsKey(count)) {
                            sortShufflingWordCounter.get(count).add(word);
                        } else {
                            sortShufflingWordCounter.put(count, new ArrayList<String>() {
                                {
                                    add(word);
                                }
                            });
                        }
                    }

                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return "";
    }
}

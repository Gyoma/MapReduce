package com.src.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.src.utils.Command;
import com.src.utils.SocketIO;
import com.src.utils.SocketHandler;

public class ServerProgram {
    private static Hashtable<String, SocketHandler> handlers = new Hashtable<String, SocketHandler>();
    private static List<SocketIO> sios = new ArrayList<SocketIO>();
    private static Hashtable<String, Integer> shufflingWordCounter = new Hashtable<String, Integer>();
    private static Hashtable<String, Integer> wordCounter = new Hashtable<String, Integer>();
    private static ReentrantLock lock = new ReentrantLock();
    private static Integer finishedCount = 0;
    private static Command nextCommand = Command.INITIALIZE;

    public static void main(String args[]) {

        ServerSocket server = null;
        try {
            server = new ServerSocket(10325);
            SocketIO msio = new SocketIO(server.accept());

            while (true) {
                String line = msio.is.readLine();

                if (nextCommand == Command.INITIALIZE && line.equals(Command.INITIALIZE.label())) {

                    // List of other servers
                    line = msio.is.readLine();
                    String[] addresses = line.split(";");

                    for (String address : addresses) {

                        SocketHandler st = new SocketHandler(address, 10325, (data) -> {
                            lock.lock();

                            if (data.equals(Command.END.label())) {
                                ++finishedCount;
                                if (finishedCount == handlers.size()) {
                                    finishedCount = 0;
                                    nextCommand = Command.QUIT;
                                }
                            } else {
                                String[] pair = data.split(":");
                                String word = pair[0];
                                Integer count = Integer.parseInt(pair[1]);
                                wordCounter.compute(word, (key, val) -> (val == null ? count : val + count));
                            }

                            if (lock.isHeldByCurrentThread())
                                lock.unlock();

                            return "";
                        });

                        handlers.put(address, st);
                        st.start();
                    }

                    for (int i = 0; i < addresses.length; ++i)
                        sios.add(new SocketIO(server.accept()));

                    while (!msio.is.readLine().equals(Command.END.label()))
                        ;
                    nextCommand = Command.MAPPING;

                } else if (nextCommand == Command.MAPPING && line.equals(Command.MAPPING.label())) {

                    while (!(line = msio.is.readLine()).equals(Command.END.label()))
                        for (String word : line.split(" "))
                            shufflingWordCounter.compute(word, (key, val) -> (val == null) ? 1 : val + 1);

                    nextCommand = Command.SHUFFLING;

                } else if (nextCommand == Command.SHUFFLING && line.equals(Command.SHUFFLING.label())) {

                    shufflingWordCounter.forEach((word, count) -> {
                        Integer index = word.hashCode() % sios.size();

                        try {
                            BufferedWriter os = sios.get(index).os;
                            os.write(word + ":" + count);
                            os.newLine();
                            os.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    shufflingWordCounter.clear();

                    sios.forEach((sio) -> {
                        BufferedWriter os = sio.os;

                        try {
                            os.write(Command.END.label());
                            os.newLine();
                            os.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    while (nextCommand == Command.SHUFFLING);

                    // while(!msio.is.readLine().equals(Command.END.label()));
                } // else if (line == Command.REDUCING.label()) {
                  // }
                else if (line == null || (nextCommand == Command.QUIT && line.equals(Command.QUIT.label()))) {
                    BufferedWriter os = new BufferedWriter(new FileWriter("res.txt"));

                    wordCounter.forEach((word, count) -> {
                        try {
                            os.write(word + " " + Integer.toString(count));
                            os.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    os.close();
                    break;
                }
            }

            // server.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            handlers.forEach((key, handler) -> {
                handler.stopRunning();
            });
        }
    }
}
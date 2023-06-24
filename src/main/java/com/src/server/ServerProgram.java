package com.src.server;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

import com.src.utils.Command;
import com.src.utils.SocketIO;
import com.src.utils.SocketHandler;

public class ServerProgram {
    public static Hashtable<String, SocketHandler> handlers = new Hashtable<String, SocketHandler>();
    public static ArrayList<SocketIO> sios = new ArrayList<SocketIO>();
    public static Hashtable<String, Integer> shufflingWordCounter = new Hashtable<String, Integer>();
    public static Hashtable<String, Integer> wordCounter = new Hashtable<String, Integer>();
    public static ReentrantLock lock = new ReentrantLock();

    public static void main(String args[]) {

        ServerSocket server = null;
        try {
            server = new ServerSocket(10325);
            SocketIO msio = new SocketIO(server.accept());

            while (true) {
                String line = msio.is.readLine();

                if (line == Command.INITIALIZE.label()) {

                    // List of other servers
                    line = msio.is.readLine();
                    String[] addresses = line.split(";");

                    for (String address : addresses) {

                        SocketHandler st = new SocketHandler(address, 10325, (data) -> {
                            lock.lock();
                            
                            String[] pair = data.split(":");
                            String word = pair[0];
                            Integer count = Integer.parseInt(pair[1], 0);
                            wordCounter.compute(word, (key, val) -> (val == null ? count : val + count));
                            
                            lock.unlock();
                            return "";
                        });

                        handlers.put(address, st);
                        st.start();
                    }

                    for (int i = 0; i < addresses.length; ++i)
                        sios.add(new SocketIO(server.accept()));

                    while (!msio.is.readLine().equals(Command.END.label()));

                } else if (line == Command.MAPPING.label()) {

                    while (!(line = msio.is.readLine()).equals(Command.END.label()))
                        for (String word : line.split(" "))
                            shufflingWordCounter.compute(word, (key, val) -> (val == null) ? 1 : val + 1);

                } else if (line == Command.SHUFFLING.label()) {

                    shufflingWordCounter.forEach((word, count) -> {
                        Integer index = word.hashCode() % handlers.size();
                        try {
                            sios.get(index).os.write(word + ":" + count);
                        } catch (Exception e) {
                            System.out.println(e.toString());
                        }
                    });

                    shufflingWordCounter.clear();

                    // while(!msio.is.readLine().equals(Command.END.label()));
                } // else if (line == Command.REDUCING.label()) {
                  // }
                else if (line == Command.QUIT.label()) {
                    break;
                }
            }

            // server.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        } finally {
        }
    }
}
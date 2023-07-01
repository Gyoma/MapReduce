package com.src.utils;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

public class SocketHandler extends Thread {
    private SocketIO sio = null;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Consumer<String> callback = null;

    public SocketHandler(Socket socket, Consumer<String> callback) throws UnknownHostException, IOException {
        this.callback = callback;
        this.sio = new SocketIO(socket);
    }

    public boolean running() {
        return this.running.get();
    }

    public void stopRunning() {

        try {
            sio.socket.close();
            sio.is.close();
            sio.os.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.running.set(false);
    }

    public synchronized String read() {
        String line = null;

        try {
            line = this.sio.is.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return line;
    }

    public synchronized void send(String data) {
        try {
            this.sio.os.write(data);
            this.sio.os.newLine();
            this.sio.os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (this.running.get()) {
            try {
                String line = this.sio.is.readLine();

                if (line == null) {
                    this.running.set(false);
                    break;
                }

                if (!line.isEmpty())
                    callback.accept(line);
                    
            } catch (Exception e) {
                if (!this.sio.socket.isClosed())
                    e.printStackTrace();

                this.running.set(false);
            }
        }
    }
}

package com.src.utils;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class SocketHandler extends Thread {
    SocketIO sio = null;
    boolean running = false;
    Function<String, String> callback = null;
    ReentrantLock lock = new ReentrantLock();

    public SocketHandler(String address, int port, Function<String, String> callback) throws UnknownHostException, IOException {
        this.callback = callback;
        this.sio = new SocketIO(new Socket(address, port));
    }

    public SocketHandler(Object address, String address2, int i) {
    }

    public boolean running() {
        return this.running;
    }

    public void stopRunning() {
        this.running = false;
    }

    public void send(String data) {
        this.lock.lock();
        
        try {
            this.sio.os.write(data);
            this.sio.os.newLine();
            this.sio.os.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            if (this.lock.isHeldByCurrentThread())
                this.lock.unlock();
        }
    }

    @Override
    public void run() {

        this.running = true;
        while (this.running) {
            try {
                if (this.sio.socket.getInputStream().available() == 0)
                    continue;

                this.lock.lock();
                String data = callback.apply(this.sio.is.readLine());

                if (!data.isEmpty())
                    send(data);

            } catch (Exception e) {
                e.printStackTrace();
                this.running = false;
            }
            finally {
                if (this.lock.isHeldByCurrentThread())
                    this.lock.unlock();
            }
        }
    }
}

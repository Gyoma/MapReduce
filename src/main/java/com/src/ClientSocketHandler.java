package com.src;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.locks.ReentrantLock;

public class ClientSocketHandler extends Thread {

    Socket socket = null;
    BufferedWriter os = null;
    BufferedReader is = null;
    boolean running = false;
    MasterClient master;
    ReentrantLock lock = new ReentrantLock();

    public ClientSocketHandler(MasterClient master, String address, int port) throws UnknownHostException, IOException {
        this.master = master;

        this.socket = new Socket(address, port);

        // Create output stream at the client (to send data to the server)
        this.os = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));

        // Input stream at Client (Receive data from the server).
        this.is = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    public void initialize(String[] args) throws IOException {
        this.os.write(String.join(";", args));
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
            this.os.write(data);
        } catch(Exception e) {
            System.out.println(e.toString());
        }
        finally {
            this.lock.unlock();
        }
    }

    @Override
    public void run() {

        this.running = true;
        while (this.running) {
            try {
                if (this.socket.getInputStream().available() == 0)
                    continue;

                this.lock.lock();
                String data = this.master.handleResponse(this.is.readLine());

                if (!data.isEmpty())
                    send(data);

            } catch (Exception e) {
                e.printStackTrace();
                this.running = false;
            }
            finally {
                this.lock.unlock();
            }
        }
    }
}

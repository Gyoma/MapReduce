package com.src;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ClientProgram {

    private static MasterClient master = null;
    
    public static void main(String[] args) {

        // Server Host
        // final String serverHost = "tp-1a201-26.enst.fr";

        Options options = new Options();
        Option so = new Option("s", "servers", true, "");
        options.addOption(so);
        Option po = new Option("p", "path", true, "");
        options.addOption(po);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args, false);

            if (cmd.hasOption('p'))
                master = new MasterClient(cmd.getOptionValue('p', ""));

            if (cmd.hasOption('s')) {
                String[] addresses = cmd.getOptionValue('s', "").split(";");

                for (String address : addresses)
                    master.addConnection(address, 10325);
            }

            master.start();

        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(1);
        }
        
        // try {
        // cmd = parser.parse(options, args, false);
        // } catch (ParseException e) {
        // System.out.println(e.getMessage());
        // formatter.printHelp("utility-name", options);

        // System.exit(1);
        // }

        // try {
        // ClientSocketHandler sh = new ClientSocketHandler(serverHost, 0);
        // } catch (UnknownHostException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        // Socket socketOfClient = null;
        // BufferedWriter os = null;
        // BufferedReader is = null;

        // try {

        // // Send a request to connect to the server is listening
        // // on machine 'localhost' port 9999.
        // socketOfClient = new Socket(serverHost, 9999);

        // // Create output stream at the client (to send data to the server)
        // os = new BufferedWriter(new
        // OutputStreamWriter(socketOfClient.getOutputStream()));

        // // Input stream at Client (Receive data from the server).
        // is = new BufferedReader(new
        // InputStreamReader(socketOfClient.getInputStream()));

        // } catch (UnknownHostException e) {
        // System.err.println("Don't know about host " + serverHost);
        // return;
        // } catch (IOException e) {
        // System.err.println("Couldn't get I/O for the connection to " + serverHost);
        // return;
        // }

        // try {

        // // Write data to the output stream of the Client Socket.
        // os.write("HELO");

        // // End of line
        // os.newLine();

        // // Flush data.
        // os.flush();
        // os.write("I am Tom Cat");
        // os.newLine();
        // os.flush();
        // os.write("QUIT");
        // os.newLine();
        // os.flush();

        // // Read data sent from the server.
        // // By reading the input stream of the Client Socket.
        // String responseLine;
        // while ((responseLine = is.readLine()) != null) {
        // System.out.println("Server: " + responseLine);
        // if (responseLine.indexOf("OK") != -1) {
        // break;
        // }
        // }

        // os.close();
        // is.close();
        // socketOfClient.close();
        // } catch (UnknownHostException e) {
        // System.err.println("Trying to connect to unknown host: " + e);
        // } catch (IOException e) {
        // System.err.println("IOException: " + e);
        // }
    }

}
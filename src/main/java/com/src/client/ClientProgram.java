package com.src.client;

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
    }

}
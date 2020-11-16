/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Gianluca Cerotto
 */
public class Cli {
    private String[] args;

    public Cli(String[] args) {
        this.args = args;
    }
    
    public void parse() {
        
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption( "r", "registry", true, "registry port (default 7432)" );
        options.addOption( "m", "rmi", true, "RMI port (default 1099)" );
        options.addOption( "l", "listen", true, "server listen port" );
        options.addOption( "h", "help", false, "show help" );
            
        HelpFormatter formatter = new HelpFormatter();
        
        try {
            CommandLine line = parser.parse( options, args );
            if(line.hasOption("r")) {
                Config.setRegistryport(Integer.parseInt(line.getOptionValue("r")));
            }
            if(line.hasOption("m")) {
                Config.setRmiport(Integer.parseInt(line.getOptionValue("m")));
            }
            if(line.hasOption("l")) {
                Config.setListenport(Integer.parseInt(line.getOptionValue("l")));
            }
            if(line.hasOption("h")) {
                formatter.printHelp("TuringServer -l <port> [OPTIONS]", options);
                System.exit(0);
            }
            if (args.length==0) {
                formatter.printHelp("TuringServer -l <port> [OPTIONS]", options);
                System.exit(0);
            }
        } catch (ParseException ex) {
            formatter.printHelp("TuringServer -l <port> [OPTIONS]", options);
            System.exit(1);
        }
    }
    
}

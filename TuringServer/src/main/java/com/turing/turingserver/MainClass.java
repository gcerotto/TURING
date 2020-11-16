/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import com.turing.common.TRMI;


/**
 *
 * @author Gianluca Cerotto
 */
public class MainClass {

    public static void main(String[] args) {
        
        Cli cli=new Cli(args);
        cli.parse();
        
        Users users=new Users();
        Documents documents=new Documents();
        AddressFactory factory = new AddressFactory();
        ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
        int port=Config.getListenport();
        
        try {
            Path dir = Files.createTempDirectory("turing");
            Config.setPath(dir.toString());
            
//            mi assicuro di non lasciare file temporanei
            Runtime.getRuntime().addShutdownHook( new Thread(  () -> {
                System.out.println("Exiting...");
                try {
                    Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException ex) {
                    System.err.println("errore fatale in fase di cleanup");
                }
            }));
            
//            setup rmi
            LocateRegistry.createRegistry(Config.getRegistryport());
            Registry r=LocateRegistry.getRegistry(Config.getRegistryport());
            TRMImpl c = new TRMImpl(users);
            TRMI stub =(TRMI) UnicastRemoteObject.exportObject(c, Config.getRmiport());
            r.rebind("Turing", stub);
            
            System.out.printf("Starting Turing on port %d\n", port);
            
//            accetto le connessioni e le smisto al threadpool
            ServerSocket serverSocket = new ServerSocket(port);
            while(true){
                Socket socket = serverSocket.accept();
//                System.err.println("connessione");
                ThreadWorker t = new ThreadWorker(users, documents, socket, factory);
                executor.execute(t);
            }
        }
        catch (RemoteException e) {
            System.out.println("RMI Server Error: " + e);
            return;
        } catch (IOException ex) {
            System.out.println("I/O error");
            ex.printStackTrace();
        }

    }
}

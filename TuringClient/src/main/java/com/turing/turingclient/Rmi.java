/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingclient;

import com.turing.common.TRMI;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author Gianluca Cerotto
 */
public class Rmi {
    
    public static boolean register(String address, Integer port, String user, String pass) {
        TRMI serverObject;
        Remote RemoteObject;
        
        try {
            Registry r = LocateRegistry.getRegistry(address, port);
            RemoteObject = r.lookup("Turing");
            serverObject = (TRMI) RemoteObject;
            return serverObject.register(user, pass);
        } catch (Exception e) {
            System.out.println("Error in invoking object method " +
            e.toString());
            e.printStackTrace();
        }
        return false;
    }
    
}

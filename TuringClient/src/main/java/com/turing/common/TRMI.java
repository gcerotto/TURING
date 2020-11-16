/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author Gianluca Cerotto
 */
public interface TRMI extends Remote {
    
    public boolean register(String Username, String Password) throws RemoteException;;
    
}

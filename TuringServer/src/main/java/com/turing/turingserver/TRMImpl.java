/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import java.rmi.server.RemoteObject;
import com.turing.common.TRMI;

/**
 *
 * @author Gianluca Cerotto
 */
public class TRMImpl extends RemoteObject implements TRMI {
    private Users users;

    public TRMImpl(Users users) {
        this.users = users;
    }

    public boolean register(String Username, String Password) {
//        System.out.println("Registrazione utente");
        return users.add(Username, Password);
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Gianluca Cerotto
 */
public class Users {
    private final ConcurrentHashMap<String,UserEl> userlist;

    public Users() {
        this.userlist = new ConcurrentHashMap<>();
    }

    
//    aggiunge l'utente se non esiste. thread safe
    boolean add(String Username, String Password) {
        if (Username == null || Password==null)
            return false;
        String user=Username.trim();
        UserEl el= new UserEl(user, Password);
        return userlist.putIfAbsent(user, el) == null;
    }
    

    boolean verify(String Username, String Password) {
        if (Username == null || Password==null)
            return false;
        UserEl el = userlist.get(Username);
        if(el==null) return false;
        return el.verifyPassword(Password);
    }
    
    ObjectOutputStream getUserSocket (String Username) {
        return userlist.get(Username).getSocket();
    }
    
    boolean setUserSocket (String Username, ObjectOutputStream socket) {
        return userlist.get(Username).setSocket(socket);
    }
    
//    true se e solo se il documento è stato inserito con successo
//    atomicità garantita dalla sottostante ConcurrentHashMap
    public boolean addDocumentAccess (String username, String document) {
        if (document==null) return false;
        return userlist.get(username).addDocumentAccess(document);
    }
    
    public boolean canAccessDocument (String username, String document) {
        if (document==null)
            return false;
        return userlist.get(username).canAccessDocument(document);
    }
    
//    restituisce l'elenco dei documenti cui l'utente ha accesso
    public ArrayList<String> getDocuments (String username) {
        return userlist.get(username).getDocuments();
    }
    
    public boolean userExists (String username) {
        return userlist.containsKey(username);
    }
    
    public boolean isOnline (String username) {
        return (this.getUserSocket(username) != null);
    }
    
    public void aggiunginotifica (String username, String document) {
        userlist.get(username).aggiunginotifica(document);
    }
    
    public ArrayList<String> legginotifiche (String username) {
        return userlist.get(username).legginotifiche();
    }
    
    public void cleansocket(String username) {
        userlist.get(username).cleansocket();
    }
    
}

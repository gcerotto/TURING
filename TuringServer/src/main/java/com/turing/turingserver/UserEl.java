/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Gianluca Cerotto
 */
public class UserEl {
    private String username, password;
    private ObjectOutputStream socket;
    private ConcurrentHashMap<String,Integer> documentlist;
    private ConcurrentLinkedQueue<String> listanotifiche;

    
    public UserEl(String username, String password) {
        this.username = username;
        this.password = password;
        this.socket = null;
        this.documentlist=new ConcurrentHashMap<>();
        this.listanotifiche=new ConcurrentLinkedQueue<>();
    }

    public String getUsername() {
        return username;
    }

    public boolean verifyPassword(String pass) {
        return this.password.equals(pass);
    }
    
    public synchronized ObjectOutputStream getSocket() {
        return socket;
    }

//    garantisce che un utente non faccia due volte login
    public synchronized boolean setSocket(ObjectOutputStream socket) {
        if (this.socket!=null) return false;
        else {
            this.socket = socket;
            return true;
        }
    }
    
    public void cleansocket() {
        this.socket = null;
    }
    
    //    ritorna true se i privilegi d'accesso sono stati inseriti con successo
    public boolean addDocumentAccess (String document) {
        return (documentlist.putIfAbsent(document, Integer.MIN_VALUE) == null);
    }
    
    public boolean canAccessDocument (String document) {
        return documentlist.containsKey(document);
    }
    
    public ArrayList<String> getDocuments () {
        ArrayList<String> res;
            res=new ArrayList<>(documentlist.keySet());
        return res;
    }

    
        @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.username);
        return hash;
    }

//    verifichiamo solo il nome utente. permette la putifabsent
//    implementata in users
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserEl other = (UserEl) obj;
        if (!this.username.equals(other.getUsername())) {
            return false;
        }

        return true;
    }

    public void aggiunginotifica(String document) {
        listanotifiche.add(document);
    }

    public ArrayList<String> legginotifiche() {
        ArrayList<String> ret=new ArrayList<>();
        String el;
//        queta volta non posso essere weekly consistent
//        non posso rischiare di rimuovere più elementi del dovuto
        while ((el=listanotifiche.poll())!=null) {
            ret.add(el);
        }
        return ret;
    }
}

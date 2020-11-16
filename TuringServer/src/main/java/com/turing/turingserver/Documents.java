/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Gianluca Cerotto
 */
public class Documents {
    private final ConcurrentHashMap<String, DocumentEl> documents;

    public Documents() {
        this.documents = new ConcurrentHashMap<>();
    }
    
//    putifabsent ritorna null se il documento è stato inserito con successo
    public boolean putDocument(String name, DocumentEl el) {
        return (documents.putIfAbsent(name, el) == null);
    }
    
//    ritorna null se il documento non esiste
    public DocumentEl getDocument(String name) {
        return documents.get(name);
    }
    
}

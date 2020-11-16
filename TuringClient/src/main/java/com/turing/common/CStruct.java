/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.common;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Gianluca Cerotto
 */

public class CStruct implements Serializable {

    private static final long serialVersionUID = 1L;
    private OP codop;
    private String docname;
    private int nsez;
    private String username;
    private String password;
    private ArrayList<String> elencodoc;
    private String testo;
    private STATUS esito;
    private String Address;
    private boolean in_modifica;
    private ArrayList<Integer> sezmod;
    private ArrayList<String> notifiche;


    public OP getCodop() {
        return codop;
    }

    public void setCodop(OP codop) {
        this.codop = codop;
    }

    public String getDocname() {
        return docname;
    }

    public void setDocname(String docname) {
        this.docname = docname;
    }

    public int getNsez() {
        return nsez;
    }

    public void setNsez(int nsez) {
        this.nsez = nsez;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ArrayList<String> getElencodoc() {
        return elencodoc;
    }

    public void setElencodoc(ArrayList<String> elencodoc) {
        this.elencodoc = elencodoc;
    }

    public String getTesto() {
        return testo;
    }

    public void setTesto(String testo) {
        this.testo = testo;
    }

    public STATUS getEsito() {
        return esito;
    }

    public void setEsito(STATUS esito) {
        this.esito = esito;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String Address) {
        this.Address = Address;
    }    

    public boolean isIn_modifica() {
        return in_modifica;
    }

    public void setIn_modifica(boolean in_modifica) {
        this.in_modifica = in_modifica;
    }

    public ArrayList<Integer> getSezmod() {
        return sezmod;
    }

    public void setSezmod(ArrayList<Integer> sezmod) {
        this.sezmod = sezmod;
    }

    public ArrayList<String> getNotifiche() {
        return notifiche;
    }

    public void setNotifiche(ArrayList<String> notifiche) {
        this.notifiche = notifiche;
    }

    @Override
    public String toString() {
        return "CStruct{" + "codop=" + codop + ", docname=" + docname + ", nsez=" + nsez + ", username=" + username + ", password=" + password + ", elencodoc=" + elencodoc + ", testo=" + testo + ", esito=" + esito + ", Address=" + Address + ", in_modifica=" + in_modifica + ", sezmod=" + sezmod + ", notifiche=" + notifiche + '}';
    }


    public enum OP {
      LOGIN,
      LOGOUT,
      CREA,
      INVITA,
      ELENCA,
      EDIT,
      ENDEDIT,
      SHOW,
      SHOWALL,
      NOTIFICA
    }

    public enum STATUS {
      SUCCESS,
      FAILURE
    }
    
}

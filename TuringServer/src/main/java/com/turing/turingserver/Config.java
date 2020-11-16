/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

/**
 *
 * @author Gianluca Cerotto
 */
public class Config {
    private static String path="/tmp/turing";
    private static int registryport=7432;
    private static int rmiport=1099;
    private static int listenport=7433;

    public Config() {
    }

    public static String getPath() {
        return path;
    }

    public static void setPath(String path) {
        Config.path = path;
    }

    public static int getRegistryport() {
        return registryport;
    }

    public static void setRegistryport(int registryport) {
        Config.registryport = registryport;
    }

    public static int getRmiport() {
        return rmiport;
    }

    public static void setRmiport(int rmiport) {
        Config.rmiport = rmiport;
    }

    public static int getListenport() {
        return listenport;
    }

    public static void setListenport(int listenport) {
        Config.listenport = listenport;
    }

    
    
    
    
}

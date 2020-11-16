/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Gianluca Cerotto
 */
public class AddressFactory {
    private LinkedBlockingQueue<String> indirizziliberi;

    public AddressFactory() {
        indirizziliberi=new LinkedBlockingQueue<>();
        for (int i=1; i<50; i++) {
            for (int j=1;j<254; j++) {
                for (int k=1; k<254; k++) {
                    indirizziliberi.add("224."+i+"."+j+"."+k);
                }
            }
        }
    }
    
    public String getAddress() throws NoSuchElementException {
        return indirizziliberi.remove();
    }
    
    public void releaseAddress(String address) {
        indirizziliberi.add(address);
    }
        
}

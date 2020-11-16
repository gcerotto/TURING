/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingclient;

import com.turing.common.CStruct;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Gianluca Cerotto
 */
public class Dispatcher implements Runnable {
    private ObjectInputStream cread;
    private LinkedBlockingQueue<CStruct> queue;
    private CStruct c;

    public Dispatcher(ObjectInputStream cread, LinkedBlockingQueue<CStruct> queue) {
        this.cread = cread;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                c= (CStruct) cread.readObject();
                if (c.getCodop()==CStruct.OP.NOTIFICA) {
                    System.out.printf("\r\nsei stato invitato a collaborare a questi documenti:");
                    for (String docname : c.getNotifiche()) {
                        System.out.printf(" %s", docname);
                    }
                    System.out.printf("\r\npremi invio per continuare");
                }
                else {
                    queue.add(c);
                }
            }
        } catch (EOFException eof ) {
//            System.out.println("Server closed the connection.");
        } catch (IOException ex) {
            System.err.println("I/O error");
            ex.printStackTrace();
            System.exit(1);
        } catch (ClassNotFoundException ex) {
            System.err.println("errore nella deserializzazione");
            ex.printStackTrace();
            System.exit(1);
        }
    }


}

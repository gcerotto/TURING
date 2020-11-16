/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Gianluca Cerotto
 */
public class Chat implements Runnable {
    private LinkedBlockingQueue<String> receivequeue;
    private String groupname;
    private MulticastSocket s=null;
    private InetAddress group=null;
    private int port=9003;

    public Chat(String groupname, LinkedBlockingQueue<String> receivequeue) {
        this.groupname = groupname;
        this.receivequeue=receivequeue;
    }

    @Override
    public void run() {
        DatagramPacket recv;
        try {
        this.group = InetAddress.getByName(groupname);
        
        s = new MulticastSocket(port);
        s.joinGroup(group);
        
        byte[] buf = new byte[1000];
        
        while (true) {
            recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            receivequeue.add(new String(recv.getData(), 0, recv.getLength()));
        }
        } catch (Exception ex) {
            System.err.println("impossibile collegarsi alla chat");
            ex.printStackTrace();
        }
        
    }
    
//    non eseguita nel thread, non ne abbiamo bisogno
    boolean send (String msg) {
        if (group==null) return false;
        DatagramPacket pkt = new DatagramPacket(msg.getBytes(), msg.length(),
                             group, port);
        try {
            s.send(pkt);
            return true;
        } catch (IOException ex) {
            System.err.println("impossibile inviare il messaggio");
            ex.printStackTrace();
            return false;
        }
    }
    
}

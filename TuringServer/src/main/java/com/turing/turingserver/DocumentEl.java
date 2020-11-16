/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author Gianluca Cerotto
 */
public class DocumentEl {
    private String Owner;
    private String name;
    private int nsez;
//    private CopyOnWriteArraySet<Integer> sezmodset;
    private final HashSet<Integer> sezmodset;
    private ConcurrentHashMap<Integer,ReentrantReadWriteLock> lockfile;
    private AddressFactory factory;
    private String address;
    

    public DocumentEl(String Owner, String name, int nsez, AddressFactory factory) throws IOException, Exception {
        if (nsez<1) throw new Exception();
        this.Owner = Owner;
        this.name = name;
        this.nsez = nsez;
        this.factory=factory;
        this.sezmodset=new HashSet<>();
        this.lockfile=new ConcurrentHashMap<>();
        for (int i=0; i<nsez; i++) {
            FileChannel.open(Paths.get(Config.getPath()+"/"+name+"."+String.valueOf(i)), 
                    StandardOpenOption.CREATE, StandardOpenOption.READ, 
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            lockfile.put(i, new ReentrantReadWriteLock());
        }
    }

    public String getOwner() {
        return Owner;
    }

    public String getName() {
        return name;
    }

    public int getNsez() {
        return nsez;
    }
    
    public String getFile(Integer nsez) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4*1024);
        StringBuilder string = new StringBuilder();
        FileChannel file = FileChannel.open(Paths.get(Config.getPath()+"/"+this.name+"."+nsez), 
                    StandardOpenOption.READ);
        
        if (nsez>=this.nsez || nsez<0) return null;
        
        Lock read = lockfile.get(nsez).readLock();
        read.lock();
        
        try {
            while(file.read(buffer) != -1) {
                buffer.flip();
                
//                while (buffer.hasRemaining()) {
//                    string.append((char)buffer.get());
//                }
                string.append(StandardCharsets.UTF_8.decode(buffer).toString());
                buffer.clear();
            }
        } finally {
            read.unlock();
            file.close();
        }
        return string.toString();
    }
    
    public String getallFile() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024*1024);
        StringBuilder string = new StringBuilder();
        
        for (int i=0; i<this.nsez; i++) {
            FileChannel file = FileChannel.open(Paths.get(Config.getPath()+"/"+this.name+"."+i), 
                    StandardOpenOption.READ);
            
            Lock read = lockfile.get(i).readLock();
            read.lock();
            try {
                while(file.read(buffer) != -1) {
                    buffer.flip();
                    string.append(StandardCharsets.UTF_8.decode(buffer).toString());
                    buffer.clear();
                }
            } finally {
                read.unlock();
                file.close();
            }
        }
        return string.toString();
    }
    
    void putFile(Integer nsez, String string) throws UnsupportedEncodingException, IOException {
        FileChannel file = FileChannel.open(Paths.get(Config.getPath()+"/"+this.name+"."+nsez), 
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        ByteBuffer buffer = ByteBuffer.wrap(string.getBytes("UTF-8"));
        
        Lock write = lockfile.get(nsez).writeLock();
        write.lock();
        try {
            while (buffer.hasRemaining()) {
                file.write(buffer);
            }
            buffer.clear();
        } finally {
            write.unlock();
            file.close();
            synchronized (sezmodset) {
                sezmodset.remove(nsez);
                if (sezmodset.isEmpty()) {
                    factory.releaseAddress(this.address);
                }
            }
        }
    }

    public ArrayList<Integer> getSezmodset() {
        ArrayList<Integer> res;
        synchronized (sezmodset) {
            res=new ArrayList<>(sezmodset);
        }
        return res;
    }
    
    public boolean is_in_edit(Integer nsez) {
        synchronized(sezmodset) {
            return sezmodset.contains(nsez);
        }
    }
    
//    ritorna null se la sezione è già in modifica
//    altrimenti, l'indirizzo dedicato a chi edita questo file
    public String request_edit(Integer nsez) {
        if (nsez>=this.nsez || nsez<0) return null;
        synchronized(sezmodset) {
            if (sezmodset.isEmpty()) {
                this.address=factory.getAddress();
            }
            if(!sezmodset.contains(nsez)) {
                sezmodset.add(nsez);
                return this.address;
            } else {
                return null;
            }
        }
    }
    
    public void emerg_release_lock(Integer nsez) {
        synchronized (sezmodset) {
            sezmodset.remove(nsez);
        }
    }
    
    
}

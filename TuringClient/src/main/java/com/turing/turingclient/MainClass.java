/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingclient;

import com.turing.common.CStruct;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Gianluca Cerotto
 */
public class MainClass {

    
    public static void main(String[] args) {
        String help=
            "usage : COMMAND [ ARGS ...]\r\n"+
            "commands :\r\n"+
            "register <username> <password>\tregistra l'utente\r\n"+
            "login <username> <password>\teffettua il login\r\n"+
            "logout\t\t\t\teffettua il logout\r\n"+
            "create <doc> <numsezioni>\tcrea un documento\r\n"+
            "share <doc> <username>\t\tcondivide il documento\r\n"+
            "show <doc> <sec>\t\tmostra una sezione del documento\r\n"+
            "show <doc>\t\t\tmostra l'intero documento\r\n"+
            "list\t\t\t\tmostra la lista dei documenti\r\n"+
            "edit <doc> <sec>\t\tmodifica una sezione del documento\r\n"+
            "end-edit <doc> <sec>\t\tfine modifica della sezione del doc\r\n"+
            "send <msg>\t\t\tinvia un msg sulla chat\r\n"+
            "receive\t\t\t\tvisualizza i msg ricevuti sulla chat\r\n"+
            "quit\t\t\t\ttermina questo programma";
        
        String cli="usage: TuringClient <hostname> <port> [optional <rmi-registry-port>]";
        
        if (args.length<2) {
            System.out.println(cli);
            return;
        }
        
        Scanner scanner=new Scanner(System.in);
        
        System.out.println(help);
        
        CStruct c, cw;
        boolean login=false, in_edit=false;
        Socket socket;
        ObjectInputStream cread=null;
        ObjectOutputStream cwrite=null;
        LinkedBlockingQueue<CStruct> queue=null;
        LinkedBlockingQueue<String> chatqueue=null;
        Thread thread=null;
        Chat chat=null;
        Thread threadchat=null;
        String username=null;
        final Path tmp;
        String prompt="Turing: $ ";
        
        try {
            tmp = Files.createTempDirectory("turing");
        } catch (IOException ex) {
            System.err.println("impossibile creare directory temporanea");
            ex.printStackTrace();
            return;
        }
        String tmpd=tmp.toString();
        
        Runtime.getRuntime().addShutdownHook( new Thread(  () -> {
                System.out.println("Exiting...");
                try {
                    Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException ex) {
                    System.err.println("errore fatale in fase di cleanup");
                }
            }));
        
        try{
        while (true) {
            System.out.printf(prompt);
            String line=scanner.nextLine();
            
            String[] split=line.split(" ");
            String command=split[0];
            
            OUTER:
            switch (command) {
                
                case "register" :
                {
                    if (login==true) {
                        System.out.println("per registrare un nuovo utente effettuare il logout");
                        break;
                    }
                    if (args.length!=3) {
                        System.out.println("please specify rmi-registry-port");
                        System.exit(1);
                    }
                    if (split.length!=3) {
                        System.out.println("parsing error");
                        break;
                    }
                    if( Rmi.register(args[0], Integer.parseInt(args[2]), split[1], split[2]) )
                        System.out.println("Registrazione avvenuta con successo");
                    else
                        System.out.println("Errore: username già presente o invalido");
                    break;
                }
                
                case "login" :
                {
                    if (login==true) {
                        System.out.println("mi sembra che tu sia già loggato...");
                        break;
                    }
                    if (split.length!=3) {
                        System.out.println("parsing error");
                        break;
                    }
                    username=split[1];
                    cw=new CStruct();
                    cw.setCodop(CStruct.OP.LOGIN);
                    cw.setUsername(username);
                    cw.setPassword(split[2]);
                    socket=new Socket(args[0], Integer.parseInt(args[1]));
                    cwrite = new ObjectOutputStream(socket.getOutputStream());
                    cread = new ObjectInputStream(socket.getInputStream());
                    cwrite.writeObject(cw);
                    c = (CStruct) cread.readObject();
                    if (c.getEsito()==CStruct.STATUS.FAILURE) {
                        System.out.println("login fallito");
                        break;
                    }
                    System.out.println("login avvenuto con successo");
                    prompt="Turing: @"+split[1]+" $ ";
                    queue=new LinkedBlockingQueue<>();
                    Dispatcher dispatcher = new Dispatcher(cread, queue);
                    thread = new Thread(dispatcher);
                    thread.start();
                    login=true;
                    break;
                }
                
                case "quit" :
                {
                    if (login==true) {
                        System.out.println("dovresti fare prima il logout");
                        break;
                    }
                    if (in_edit==true) {
                        System.out.println("errore: ci sono modifiche ancora in corso");
                        break;
                    }
                    System.exit(0);
                }
                
                case "logout" :
                {
                    if (login==false) {
                        System.out.println("dovresti fare login prima di fare logout");
                        break;
                    }
                    if (in_edit==true) {
                        System.out.println("errore: ci sono modifiche ancora in corso");
                        break;
                    }
                    cw=new CStruct();
                    cw.setCodop(CStruct.OP.LOGOUT);
                    cwrite.writeObject(cw);
                    c=queue.take();
                    if (c.getEsito()==CStruct.STATUS.SUCCESS) {
                        System.out.println("logout eseguito con successo");
                        prompt="Turing: $ ";
                        login=false;
                        thread.interrupt();
                    } else {
                        System.out.println("si è verificato un errore");
                    }
                    break;
                }
                
                case "create":
                {
                    if (login==false) {
                        System.out.println("dovresti fare prima il login");
                        break;
                    }
                    if (in_edit==true) {
                        System.out.println("errore: ci sono modifiche ancora in corso");
                        break;
                    }
                    if (split.length!=3) {
                        System.out.println("parsing error");
                        break;
                    }
                    cw=new CStruct();
                    cw.setCodop(CStruct.OP.CREA);
                    cw.setDocname(split[1]);
                    try {
                        cw.setNsez(Integer.parseInt(split[2]));
                    } catch (NumberFormatException ex) {
                        System.out.println("inserire un numero di sezione valido");
                        break;
                    }
                    cwrite.writeObject(cw);
                    c=queue.take();
                    if (c.getEsito()==CStruct.STATUS.SUCCESS) {
                        System.out.println("inserimento eseguito con successo");
                    } else {
                        System.out.println("si è verificato un errore");
                    }
                    break;
                }
                
                case "share":
                {
                    if (login==false) {
                        System.out.println("dovresti prima fare il login");
                        break;
                    }
                    if (in_edit==true) {
                        System.out.println("errore: ci sono modifiche ancora in corso");
                        break;
                    }
                    if (split.length!=3) {
                        System.out.println("parsing error");
                        break;
                    }
                    cw=new CStruct();
                    cw.setCodop(CStruct.OP.INVITA);
                    cw.setDocname(split[1]);
                    cw.setUsername(split[2]);
                    cwrite.writeObject(cw);
                    c=queue.take();
                    if (c.getEsito()==CStruct.STATUS.SUCCESS) {
                        System.out.println("documento condiviso con successo");
                    } else {
                        System.out.println("si è verificato un errore");
                    }
                    break;
                }
                
                case "show":
                {
                    if (login==false) {
                        System.out.println("dovresti prima fare il login");
                        break;
                    }   if (in_edit==true) {
                        System.out.println("errore: ci sono modifiche ancora in corso");
                        break;
                    }
                        
                    cw=new CStruct();
                    ByteBuffer buffer;
                    String docname=split[1];
                    String sez="";
                    boolean all=false;
                        
                    if (split.length == 2) {
                        cw.setCodop(CStruct.OP.SHOWALL);
                        all=true;
                    }
                    else if (split.length == 3) {
                        cw.setCodop(CStruct.OP.SHOW);
                        sez=split[2];
                        try {
                            cw.setNsez(Integer.parseInt(sez));
                        } catch (NumberFormatException ex) {
                            System.out.println("inserire un numero di sezione valido");
                            break;
                        }
                    }
                    else {
                        System.out.println("parsing error");
                        break;
                    }
                    cw.setDocname(docname);
                    cwrite.writeObject(cw);
//                    bloccante. rimaniamo in attesa di risposta
                    c=queue.take();
                    if (c.getEsito()==CStruct.STATUS.SUCCESS) {
                        String filepath;
                        if (all) filepath=tmpd+"/"+docname;
                        else filepath=tmpd+"/"+docname+"."+sez;
                        FileChannel file = FileChannel.open(Paths.get(filepath), 
                            StandardOpenOption.CREATE, StandardOpenOption.READ, 
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                        buffer = ByteBuffer.wrap(c.getTesto().getBytes("UTF-8"));
                        try {
                            while (buffer.hasRemaining()) {
                                file.write(buffer);
                            }
                            buffer.clear();
                        } catch (IOException ex) {
                            System.err.println("i/o error. impossibile scrivere il file");
                            ex.printStackTrace();
                        }
                        if (all) {
                            System.out.printf("file scaricato con successo in %s\r\n", filepath);
                            ArrayList<Integer> sezioni=c.getSezmod();
                            if (!sezioni.isEmpty()) {
                                System.out.printf("le seguenti sezioni sono in corso di modifica:");
                                for (Integer i : sezioni) {
                                    System.out.printf(" %d", i);
                                }
                                System.out.printf("\r\n");
                            }
                            
                        } else {
                            System.out.printf("sezione scaricata con successo in %s\r\n", filepath);
                            if (c.isIn_modifica()) {
                                System.out.println("la sezione è attualmente in modifica da parte di un altro utente");
                            }
                        }
                        file.close();
                    }
                    else {
                        System.out.println("si è verificato un errore");
                    }
                    break;
                }
                
                case "list" :
                {   if (login==false) {
                        System.out.println("dovresti prima fare il login");
                        break;
                    }
                    if (in_edit==true) {
                        System.out.println("errore: ci sono modifiche ancora in corso");
                        break;
                    }
                    cw=new CStruct();
                    cw.setCodop(CStruct.OP.ELENCA);
                    cwrite.writeObject(cw);
                    c=queue.take();
                    if (c.getEsito()==CStruct.STATUS.FAILURE) {
                        System.out.println("si è verificato un errore");
                        break;
                    }
                    ArrayList<String> elenco=c.getElencodoc();
                    if (elenco.isEmpty()) {
                        System.out.println("Non ci sono documenti cui puoi accedere");
                    } else {
                        System.out.printf("puoi accedere ai seguenti documenti:");
                        for (String s : elenco) {
                            System.out.printf(" %s", s);
                        }
                        System.out.printf("\r\n");
                    }
                    break;
                }
                
                case "edit" :
                {
                    if (login==false) {
                        System.out.println("dovresti prima fare il login");
                        break;
                    }
                    if (in_edit==true) {
                        System.out.println("errore: ci sono modifiche ancora in corso");
                        break;
                    }
                    if (split.length!=3) {
                        System.out.println("parsing error");
                        break;
                    }
                    String docname=split[1];
                    cw=new CStruct();
                    cw.setCodop(CStruct.OP.EDIT);
                    cw.setDocname(docname);
                    String sez=split[2];
                        try {
                        cw.setNsez(Integer.parseInt(sez));
                    } catch (NumberFormatException ex) {
                        System.out.println("inserire un numero di sezione valido");
                        break;
                    }
                    cwrite.writeObject(cw);
                    c=queue.take();
                    if (c.getEsito()==CStruct.STATUS.SUCCESS) {
                        ByteBuffer buffer;
                        String filepath=tmpd+"/"+docname+"."+sez;
                        FileChannel file = FileChannel.open(Paths.get(filepath), 
                            StandardOpenOption.CREATE, StandardOpenOption.READ, 
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                        buffer = ByteBuffer.wrap(c.getTesto().getBytes("UTF-8"));
                        try {
                            while (buffer.hasRemaining()) {
                                file.write(buffer);
                            }
                            buffer.clear();
                        } catch (IOException ex) {
                            System.err.println("i/o error. impossibile scrivere il file");
                            ex.printStackTrace();
                        }
                        System.out.printf("sezione scaricata con successo in %s\r\n", filepath);
                        System.out.println("È possibile modificare il file.");
                        in_edit=true;
//                        creo la chat dedicata al documento
                        chatqueue=new LinkedBlockingQueue<>();
                        chat = new Chat(c.getAddress(), chatqueue);
                        threadchat = new Thread(chat);
                        threadchat.start();
                    } else {
                        System.out.println("non è possibile eseguire questa operazione");
                        break;
                    }
                    break;
                }
                
                case "end-edit" :
                {
                    if (login==false) {
                        System.out.println("dovresti prima fare il login");
                        break;
                    }
                    if (in_edit==false) {
                        System.out.println("errore: non c'è un edit in corso");
                        break;
                    }
                    if (split.length!=3) {
                        System.out.println("parsing error");
                        break;
                    }
                    cw=new CStruct();
                    cw.setCodop(CStruct.OP.ENDEDIT);
                    cw.setDocname(split[1]);
                    try {
                        cw.setNsez(Integer.parseInt(split[2]));
                    } catch (NumberFormatException ex) {
                        System.out.println("inserire un numero di sezione valido");
                        break;
                    }                    
                    ByteBuffer buffer = ByteBuffer.allocateDirect(4*1024);
                    StringBuilder string = new StringBuilder();
                    String filepath=tmpd+"/"+split[1]+"."+split[2];
                    FileChannel file = FileChannel.open(Paths.get(filepath), 
                                                    StandardOpenOption.READ);
                    while(file.read(buffer) != -1) {
                        buffer.flip();
                        string.append(StandardCharsets.UTF_8.decode(buffer).toString());
                        buffer.clear();
                    }
                    cw.setTesto(string.toString());
                    cwrite.writeObject(cw);
                    c=queue.take();
                    if (c.getEsito()==CStruct.STATUS.SUCCESS) {
                        System.out.println("documento inviato con successo");
                        in_edit=false;
                        threadchat.interrupt();
                    } else {
                        System.out.println("si è verificato un errore");
                    }
                    break;
                }
                
                case "send" :
                {
                    if (login==false) {
                        System.out.println("dovresti prima fare il login");
                        break;
                    }
                    if (in_edit==false) {
                        System.out.println("errore: non c'è un edit in corso");
                        break;
                    }
                    StringBuilder string = new StringBuilder();
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");  
                    Date date = new Date();  
                    string.append(formatter.format(date));
                    string.append(String.format(" %s: ", username));
                    string.append(line.substring(command.length()+1));
                    if(chat.send(string.toString())) {
                        System.out.println("messaggio inviato sulla chat");
                    }
                    break;
                }
                
                case "receive" :
                {
                    if (login==false) {
                        System.out.println("dovresti prima fare il login");
                        break;
                    }
                    if (in_edit==false) {
                        System.out.println("errore: non c'è un edit in corso");
                        break;
                    }
                    String msg;
                    while ((msg=chatqueue.poll())!=null) {
                        System.out.println(msg);
                    }
                    break;
                }
                
                case "" :
                {
                    break;
                }
                
                default :
                {
                    System.out.println("parsing error");
                    break;
                }
                
            }
        }
        } catch(NoSuchElementException ex) {
            
        } catch (IOException ex) {
            System.err.println("I/O error");
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.err.println("errore nella deserializzazione");
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        System.exit(0);
    }
    
    
    
}

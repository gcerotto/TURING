/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.turing.turingserver;

import com.turing.common.CStruct;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Gianluca Cerotto
 */
public class ThreadWorker implements Runnable {
    private Users users;
    private Documents documents;
    private Socket socket;
    private AddressFactory factory;
//    private ExecutorService executor;

    public ThreadWorker(Users users, Documents documents, Socket socket, AddressFactory factory) {
        this.users = users;
        this.documents = documents;
        this.socket = socket;
        this.factory = factory;
//        this.executor = executor;
    }
    
    @Override
    public void run() {
        CStruct c, cw;
        String username=null;
        String docname=null;
        Integer edit_sez=null;
        boolean login=false;
        boolean in_edit=false;
        try {
//            va creato prima l'outputstream
            ObjectOutputStream cwrite = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream cread = new ObjectInputStream(socket.getInputStream());
            c = (CStruct) cread.readObject();
            
            username=c.getUsername();
            
//            l'utente si può loggare solo se non è già loggato
            if(!users.verify(username, c.getPassword()) ||
                    !users.setUserSocket(username, cwrite)) {
//                System.out.printf("login non riuscito. users.verify %b ", users.verify(username, c.getPassword()) );
                returnfailure(cwrite);
                socket.close();
                return;
            }
            login=true;
            cw=new CStruct();
            cw.setCodop(CStruct.OP.LOGIN);
            cw.setEsito(CStruct.STATUS.SUCCESS);
            synchronized (cwrite) {
                cwrite.writeObject(cw);
            }
//            invio eventuali inviti pendenti
            ArrayList<String> notifiche=users.legginotifiche(username);
            if (!notifiche.isEmpty()) {
                cw=new CStruct();
                cw.setCodop(CStruct.OP.NOTIFICA);
                cw.setNotifiche(notifiche);
                synchronized (cwrite) {
                    cwrite.writeObject(cw);
                }
            }

            while(true) {
                c = (CStruct) cread.readObject();
                
                switch (c.getCodop()) {
                    
                    case CREA :
                    {
                        if(in_edit==true) {
                            returnfailure(cwrite);
                            break;
                        }
                        try {
                            docname=c.getDocname();
                            Integer nsez=c.getNsez();
                            if (nsez<1) {
                                returnfailure(cwrite);
                                break;
                            }
                            DocumentEl doc=new DocumentEl(username, docname, nsez, factory);
                            
                            if(documents.putDocument(docname, doc) != true) {
                                returnfailure(cwrite);
                                break;
                            }
                            if(users.addDocumentAccess(username, docname) != true) {
                                returnfailure(cwrite);
                                break;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            returnfailure(cwrite);
                            socket.close();
                            return;
                        }
                        cw=new CStruct();
                        cw.setCodop(CStruct.OP.CREA);
                        cw.setEsito(CStruct.STATUS.SUCCESS);
                        synchronized (cwrite) {
                                cwrite.writeObject(cw);
                        }
                        break;
                    }
                    
                    case ELENCA :
                        if(in_edit==true) {
                            returnfailure(cwrite);
                            break;
                        }
                        cw=new CStruct();
                        cw.setElencodoc(users.getDocuments(username));
                        cw.setEsito(CStruct.STATUS.SUCCESS);
                        synchronized (cwrite) {
                                cwrite.writeObject(cw);
                        }
                        break;
                        
                    case SHOW :
                    {
                        if(in_edit==true) {
                            returnfailure(cwrite);
                            break;
                        }
                        docname=c.getDocname();
                        Integer sez=c.getNsez();
                        if (users.canAccessDocument(username, docname)) {
                            DocumentEl el = documents.getDocument(docname);
                            if (el==null) {
                                returnfailure(cwrite);
                                break;
                            }
                            String testo=el.getFile(sez);
                            if (testo==null) {
                                returnfailure(cwrite);
                                break;
                            }
                            cw=new CStruct();
                            cw.setCodop(CStruct.OP.SHOW);
                            cw.setTesto(testo);
                            cw.setIn_modifica(el.is_in_edit(sez));
                            cw.setEsito(CStruct.STATUS.SUCCESS);
                            synchronized (cwrite) {
                                cwrite.writeObject(cw);
                            }
                        } else {
                            returnfailure(cwrite);
                        }
                        break;
                    }
                        
                    case SHOWALL :
                    {
                        if(in_edit==true) {
                            returnfailure(cwrite);
                            break;
                        }
                        docname=c.getDocname();
                        if (users.canAccessDocument(username, docname)) {
                            DocumentEl el = documents.getDocument(docname);
                            cw=new CStruct();
                            cw.setCodop(CStruct.OP.SHOW);
                            cw.setTesto(el.getallFile());
                            cw.setSezmod(el.getSezmodset());
                            cw.setEsito(CStruct.STATUS.SUCCESS);
                            synchronized (cwrite) {
                                cwrite.writeObject(cw);
                            }
                        } else {
                            returnfailure(cwrite);
                        }
                        break;
                    }
                    
                    case LOGOUT :
                    {
                        if(in_edit==true) {
                            returnfailure(cwrite);
                            break;
                        }
                        users.setUserSocket(username, null);
                        cw=new CStruct();
                        cw.setCodop(CStruct.OP.LOGOUT);
                        cw.setEsito(CStruct.STATUS.SUCCESS);
                        synchronized (cwrite) {
                            cwrite.writeObject(cw);
                        }
                        cread.close();
                        cwrite.close();
                        socket.close();
                        return;
                    }
                    
                    case EDIT :
                    {
                        if(in_edit==true) {
                            returnfailure(cwrite);
                            break;
                        }
                        docname=c.getDocname();
                        edit_sez=c.getNsez();
                        if (!users.canAccessDocument(username, docname)) {
                            returnfailure(cwrite);
                            break;
                        }
                        DocumentEl el=documents.getDocument(docname);
//                        ritorna null se un altro utente sta effettuando edit,
//                        altrimenti l'indirizzo dedicato al multicasting
                        String address = el.request_edit(edit_sez);
                        if (address==null) {
                            returnfailure(cwrite);
                            break;
                        }
                        in_edit=true;
                        String testo=el.getFile(edit_sez);
                        if (testo==null) {
                            returnfailure(cwrite);
                            break;
                        }
                        cw=new CStruct();
                        cw.setCodop(CStruct.OP.EDIT);
                        cw.setTesto(testo);
                        cw.setAddress(address);
                        cw.setEsito(CStruct.STATUS.SUCCESS);
                        synchronized (cwrite) {
                            cwrite.writeObject(cw);
                        }
                        break;
                    }
                    
                    case ENDEDIT :
                    {
                        if(in_edit==false) {
                            returnfailure(cwrite);
                            break;
                        }
//                        non verifico i diritti d'accesso perchè sono già
//                        in modalità editing
//                        verifico che l'utente stia inviando il file giusto
                        if (!c.getDocname().equals(docname) || c.getNsez()!=edit_sez) {
                            returnfailure(cwrite);
                            break;
                        }
                        String testo=c.getTesto();
                        documents.getDocument(docname).putFile(edit_sez, testo);
                        cw=new CStruct();
                        cw.setCodop(CStruct.OP.ENDEDIT);
                        cw.setEsito(CStruct.STATUS.SUCCESS);
                        synchronized (cwrite) {
                            cwrite.writeObject(cw);
                        }
                        in_edit=false;
                        break;
                    }
                    
                    case INVITA :
                    {
                        if(in_edit==true) {
                            returnfailure(cwrite);
                            break;
                        }
                        String dainvitare=c.getUsername();
                        String qualedoc=c.getDocname();
                        DocumentEl el=documents.getDocument(qualedoc);
                        
                        
                        if ( el==null || !el.getOwner().equals(username)
                                || !users.userExists(dainvitare) ) {
                            returnfailure(cwrite);
                            break;
                        }
                        if( users.addDocumentAccess(dainvitare, qualedoc) != true) {
                            returnfailure(cwrite);
                            break;
                        }
                        ObjectOutputStream altroutente=users.getUserSocket(dainvitare);
                        if(altroutente==null) {
                            users.aggiunginotifica(dainvitare, qualedoc);
                        }
                        else {
//                        voglio evitare di bloccarmi ad aspettare client molto lenti
//                          executor.execute( () -> {
                            CStruct caltro=new CStruct();
                            caltro.setCodop(CStruct.OP.NOTIFICA);
                            caltro.setNotifiche(new ArrayList<>(Arrays.asList(qualedoc)));
                            try {
                                synchronized(altroutente) {
                                    altroutente.writeObject(caltro);
                                }
                            } catch (IOException ex) {
                                System.out.println("l'utente non è più online, rimetto l'invito nella coda");
                                users.aggiunginotifica(dainvitare, qualedoc);
                            }
//                          });
                        }
                        cw=new CStruct();
                        cw.setCodop(CStruct.OP.INVITA);
                        cw.setEsito(CStruct.STATUS.SUCCESS);
                        synchronized (cwrite) {
                            cwrite.writeObject(cw);
                        }
                        break;
                    }
                    
                    
//                    non utilizzata. vecchia versione polling
//                    case NOTIFICA :
//                    {
//                        cw=new CStruct();
//                        cw.setCodop(CStruct.OP.NOTIFICA);
//                        cw.setNotifiche(users.legginotifiche(username));
//                        synchronized (cwrite) {
//                            cwrite.writeObject(cw);
//                        }
//                        break;
//                    }
//                        
                            
                }
            }
        } catch( EOFException eof ) {
            System.out.println("Client closed the connection.");
        } catch (IOException ex) {
            System.out.println("error reading socket or file");
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.out.println("error in deserialization");
            ex.printStackTrace();
        } finally {
//            garantisce la consistenza della rappresentazione
            if (login==true) {
                System.err.println("cleaning out");
                users.cleansocket(username);
            }
            if (in_edit==true && docname !=null && edit_sez!=null) {
                System.err.println("release lock");
                documents.getDocument(docname).emerg_release_lock(edit_sez);
            }
        }
        
                
        
    }

    private void returnfailure(ObjectOutputStream cwrite) throws IOException {
        CStruct cw=new CStruct();
        cw.setEsito(CStruct.STATUS.FAILURE);
        synchronized (socket) {
            cwrite.writeObject(cw);
        }
    }
    
}

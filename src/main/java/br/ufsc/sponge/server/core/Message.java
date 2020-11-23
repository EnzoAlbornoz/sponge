package br.ufsc.sponge.server.core;
import java.io.Serializable;

import br.ufsc.sponge.server.repositories.SerVirtualFile;


public class Message implements Serializable{
    private static final long serialVersionUID = 1234202020L;

    public SerVirtualFile file;
    public MessageCommand command;

    public Message() {}
    public Message(SerVirtualFile file, MessageCommand command) {    
        this.file = file;
        this.command = command;
    }

}
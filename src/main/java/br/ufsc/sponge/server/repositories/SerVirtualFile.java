package br.ufsc.sponge.server.repositories;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class SerVirtualFile implements Serializable{
    private static final long serialVersionUID = 1234202021L;
    public String name;
    public UUID id;
    public Date date;
    public Long size; // size in bytes
    public byte[] content;
    public boolean physical = false; // read past tense

    public SerVirtualFile(){}

    public SerVirtualFile(VirtualFile vf) {
        this.name = vf.getName();
        this.id = UUID.fromString(vf.getId());
        this.date = vf.getDate();
        this.size = vf.getSize();
        this.content = vf.getContent().orElse(null);
        this.physical = vf.hasPhysicalCopy();
    }

}

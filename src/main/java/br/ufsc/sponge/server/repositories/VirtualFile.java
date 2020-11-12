package br.ufsc.sponge.server.repositories;

import java.util.UUID;

import java.util.Base64;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Date;

public class VirtualFile {
    private String name;
    private UUID id;
    private Date date;
    private Long size; // size in bytes
    private Byte[] content;
    private boolean empty;

    public VirtualFile(String name, Long date, Long size) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.date = new Date(date);
        this.size = size;
    }

    public VirtualFile(File file) throws UnsupportedEncodingException {
        // Get File Metadata
        final var fmeta = file.getName().split("_");
        // Extract Metadata
        final var id = UUID.fromString(fmeta[0]);
        final var ts = Long.parseLong(fmeta[1], 16);
        final var nm = new String(Base64.getDecoder().decode(fmeta[2]), "UTF-8");
        final var sz = file.length();
        System.out.println(fmeta[2]);
        System.out.println(nm);
        // Setup File Data
        this.name = nm;
        this.id = id;
        this.date = new Date(ts);
        this.size = sz;
    }

    public String toString() {
        return "Id: " + this.getId() + 
        "\nName: " + this.getName() + 
        "\nSize: " + this.getSize() + 
        " bytes\nLast modified: " + this.getDate();
    }

    public String getId() {
        return this.id.toString();
    }
    public String getName() {
        return this.name;
    }
    public Long getSize() {
        return this.size;
    }
    public Date getDate() {
        return this.date;
    }

}
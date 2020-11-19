package br.ufsc.sponge.server.repositories;

import java.util.UUID;

import java.util.Base64;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

public class VirtualFile {
    private String name;
    private UUID id;
    private Date date;
    private Long size; // size in bytes
    private Optional<byte[]> content = Optional.empty();
    private boolean physical = false; // read past tense

    private VirtualFile(VirtualFile vf) {
        this.name = vf.name;
        this.id = vf.id;
        this.date = vf.date;
        this.size = vf.size;
        this.content = vf.content;
        this.physical = vf.physical;
    }

    public VirtualFile(String name, Long date, Long size) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.date = new Date(date);
        this.size = size;
    }

    // Build a file from an upload
    public VirtualFile(String name, Long date, byte[] content) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.date = new Date(date);
        this.size = (long) content.length;
        this.content = Optional.of(content);
    }

    // Build a file from a storage entry
    public VirtualFile(File file) throws UnsupportedEncodingException {
        // Get File Metadata
        final var fmeta = file.getName().split("_");
        // Extract Metadata
        final var id = UUID.fromString(fmeta[0]);
        final var ts = Long.parseLong(fmeta[1], 16);
        final var nm = new String(Base64.getDecoder().decode(fmeta[2]), "UTF-8");
        final var sz = file.length();
        // Setup File Data
        this.name = nm;
        this.id = id;
        this.date = new Date(ts);
        this.size = sz;
        this.physical = true;
    }

    public String toString() {
        return "Id: " + this.getId() + 
        "\nName: " + this.getName() + 
        "\nSize: " + this.getSize() + 
        " bytes\nLast modified: " + this.getDate();
    }

    public Optional<byte[]> getContent() {
        return this.content;
    }

    public void setContent(byte[] content) {
        this.content = Optional.of(content);
    }

    public void setName(String newName) {
        this.name = newName;
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

    public boolean hasPhysicalCopy() {
        return this.physical;
    }

    public String toFileNameHash() throws UnsupportedEncodingException {
        String[] params = {
            this.getId(),
            Long.toHexString(this.date.getTime()),
            new String(Base64.getEncoder().encode(this.name.getBytes()), "UTF-8")
        };
        return String.join("_", params); 
    }

    public VirtualFile clone() {
        return new VirtualFile(this);
    }
}
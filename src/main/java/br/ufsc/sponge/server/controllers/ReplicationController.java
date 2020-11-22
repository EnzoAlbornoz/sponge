package br.ufsc.sponge.server.controllers;

import br.ufsc.sponge.server.repositories.VirtualFile;

public class ReplicationController{

    public ReplicationController(){
        super();
    }

    public boolean create(VirtualFile vFile) {
        // Create file here
        // try {
        //     return FileRepository.getInstance().createFileSlave(vFile);
        // } catch(Exception e) {
        //     return false;
        // }
        return true;
    }
    
}

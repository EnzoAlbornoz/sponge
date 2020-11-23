package br.ufsc.sponge.server.controllers;

import java.util.Optional;

import org.java_websocket.client.WebSocketClient;
import org.tinylog.Logger;

import br.ufsc.sponge.server.core.Message;
import br.ufsc.sponge.server.repositories.FileRepository;
import br.ufsc.sponge.server.repositories.VirtualFile;

public class ReplicationController{
    // Singleton Overhead
    private static Optional<ReplicationController> instance = Optional.empty();

    private ReplicationController() {
    }

    public static ReplicationController getInstance() {
        if (instance.isEmpty()) {
            instance = Optional.of(new ReplicationController());
        }
        return instance.get();
    }

    public void create(WebSocketClient client, Message message) {
        // Create file here
        var vFile = VirtualFile.fromSerializable(message.file);
        Logger.info("[create] Received command on {}", vFile.getId());
        var success = FileRepository.getInstance().createFileSlave(vFile);
        Logger.info("[create] Command result status", success);
        client.send(Boolean.toString(success));
    }

    public void update(WebSocketClient client, Message message) {
        var vFile = VirtualFile.fromSerializable(message.file);
        Logger.info("[update] Received command on {}", vFile.getId());
        var success = FileRepository.getInstance().updateFileSlave(vFile);
        Logger.info("[update] Command result status", success);
        client.send(Boolean.toString(success));
    }

    public void delete(WebSocketClient client, Message message) {
        var vFile = VirtualFile.fromSerializable(message.file);
        Logger.info("[delete] Received command on {}", vFile.getId());
        var success = FileRepository.getInstance().deleteFileSlave(vFile);
        Logger.info("[delete] Command result status", success);
        client.send(Boolean.toString(success));
    }
    
}

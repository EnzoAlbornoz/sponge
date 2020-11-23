package br.ufsc.sponge.server.repositories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import br.ufsc.sponge.server.core.MasterConnector;
import br.ufsc.sponge.server.core.Message;
import br.ufsc.sponge.server.core.MessageCommand;
import ch.jalu.configme.SettingsManager;

public class ReplicationRepository {
    private ReplicationRepository() {
    };

    private boolean initialized = false;
    private SettingsManager settings;
    private MasterConnector connector;

    private static ReplicationRepository single_instance = null;

    public static ReplicationRepository getInstance() {
        if (single_instance == null)
            single_instance = new ReplicationRepository();
        return single_instance;
    }

    // Methods
    public void initialize(SettingsManager settings) {
        Logger.info("Initializing");
        // Initialize Settings
        this.settings = settings;
        // Instantiate Registry
        // Mark as Initialized
        this.initialized = true;
        Logger.info("Initialized");
    }

    public void registerConnector(MasterConnector connector) {
        this.connector = connector;
    }

    public boolean replicateFileCreation(VirtualFile vFile) {
        var message = new Message(vFile.toSerializable(), MessageCommand.CREATE);
        try {
            Logger.info("[replicateFileCreation] Sending command to Slaves");
            // Send message
            var results = new ArrayList<>(this.connector.broadcast(message).get());
            Logger.info("[replicateFileCreation] Settled results");
            Logger.info("[replicateFileCreation] Checking for errors");
            if(results.parallelStream().anyMatch((entry) -> { return !entry.getValue(); })) {
                // Some entry has error -> Rollback!
                Logger.info("[replicateFileCreation] Error ocurred, making rollback");
                var willRollbackSockets = results.parallelStream()
                    .filter(entry -> entry.getValue())
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toList());
                // Define rollback action
                var rollbackMessage = new Message(vFile.toSerializable(), MessageCommand.DELETE);
                // Send rollback
                this.connector.broadcast(willRollbackSockets, rollbackMessage).get();
                return false;
            } else {
                // Sucess
                Logger.info("[replicateFileCreation] No errors detected");
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public boolean replicateFileUpdate(VirtualFile vFile) {
        var message = new Message(vFile.toSerializable(), MessageCommand.UPDATE);
        try {
            Logger.info("[replicateFileUpdate] Sending command to Slaves");
            // Send Message
            var results = new ArrayList<>(this.connector.broadcast(message).get());
            Logger.info("[replicateFileUpdate] Settled results");
            Logger.info("[replicateFileUpdate] Checking for errors");
            if(results.parallelStream().anyMatch((entry) -> { return !entry.getValue(); })) {
                // Some entry has error -> Rollback!
                Logger.info("[replicateFileUpdate] Error ocurred, making rollback");
                var willRollbackSockets = results.parallelStream()
                    .filter(entry -> entry.getValue())
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toList());
                // Define rollback action
                var rollbackMessage = new Message(vFile.toSerializable(), MessageCommand.UPDATE);
                // Send rollback
                this.connector.broadcast(willRollbackSockets, rollbackMessage).get();
                return false;
            } else {
                // Sucess
                Logger.info("[replicateFileUpdate] No errors detected");
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public boolean replicateFileDeletion(VirtualFile vFile) {
        var message = new Message(vFile.toSerializable(), MessageCommand.DELETE);
        try {
            Logger.info("[replicateFileDeletion] Sending command to Slaves");
            // Sending Message
            var results = new ArrayList<>(this.connector.broadcast(message).get());
            Logger.info("[replicateFileDeletion] Settled results");
            Logger.info("[replicateFileDeletion] Checking for errors");
            if(results.parallelStream().anyMatch((entry) -> { return !entry.getValue(); })) {
                // Some entry has error -> Rollback!
                Logger.info("[replicateFileDeletion] Error ocurred, making rollback");
                var willRollbackSockets = results.parallelStream()
                    .filter(entry -> entry.getValue())
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toList());
                // Define rollback action
                var rollbackMessage = new Message(vFile.toSerializable(), MessageCommand.CREATE);
                // Send rollback
                this.connector.broadcast(willRollbackSockets, rollbackMessage).get();
                return false;
            } else {
                // Sucess
                Logger.info("[replicateFileDeletion] No errors detected");
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

}

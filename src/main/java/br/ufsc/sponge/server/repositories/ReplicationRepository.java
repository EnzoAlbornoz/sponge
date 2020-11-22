package br.ufsc.sponge.server.repositories;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.tinylog.Logger;

import br.ufsc.sponge.server.config.ServerConfiguration;
import ch.jalu.configme.SettingsManager;

public class ReplicationRepository {
    private ReplicationRepository() {
    };

    private boolean initialized = false;
    private SettingsManager settings;
    private Registry registry;

    private static ReplicationRepository single_instance = null;

    public static ReplicationRepository getInstance() {
        if (single_instance == null)
            single_instance = new ReplicationRepository();
        return single_instance;
    }

    // Methods
    public void initialize(SettingsManager settings) throws RemoteException {
        Logger.info("Initializing");
        // Initialize Settings
        this.settings = settings;
        // Instantiate Registry
        // Mark as Initialized
        this.initialized = true;
        Logger.info("Initialized");
    }

}

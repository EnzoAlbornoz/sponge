package br.ufsc.sponge.server.config;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.PropertyInitializer;

public final class ServerConfiguration implements SettingsHolder {
    // Properties
    public static final Property<ServerType> INSTANCE_TYPE = PropertyInitializer.newProperty(ServerType.class, "instance.type", ServerType.MASTER);
    public static final Property<Integer> INSTANCE_PORT = PropertyInitializer.newProperty("instance.port", 9647);
    public static final Property<String> INSTANCE_SHARED_FOLDER = PropertyInitializer.newProperty("instance.sharedFolder", "~/.sponge/files");
    // Optional -> Slave Options
    public static final Property<String> SLAVEOPTS_HOST = PropertyInitializer.newProperty("slaveOptions.host", "0.0.0.0");
    public static final Property<Integer> SLAVEOPTS_PORT = PropertyInitializer.newProperty("slaveOptions.port", 9647);
    // Optional -> Master Options
    public static final Property<Integer> MASTEROPTS_PORT = PropertyInitializer.newProperty("masterOptions.port", 9648);
    // Prevent Instantiation
    private ServerConfiguration() {}
}

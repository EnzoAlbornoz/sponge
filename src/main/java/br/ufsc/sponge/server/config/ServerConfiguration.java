package br.ufsc.sponge.server.config;

import java.nio.file.Path;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.PropertyInitializer;

public final class ServerConfiguration implements SettingsHolder {
    // Properties
    public static final Property<ServerType> INSTANCE_TYPE = PropertyInitializer.newProperty(ServerType.class, "instance.type", ServerType.MASTER);
    public static final Property<String> INSTANCE_SHARED_FOLDER = PropertyInitializer.newProperty("instance.sharedFolder", Path.of(System.getProperty("user.home"),"./.sponge/storage").toString());
    // Optional -> Slave Options
    public static final Property<String> SLAVEOPTS_HOST = PropertyInitializer.newProperty("slaveOptions.masterHost", "0.0.0.0");
    public static final Property<Integer> SLAVEOPTS_PORT = PropertyInitializer.newProperty("slaveOptions.masterPort", 9648);
    // Optional -> Master Options
    public static final Property<Integer> MASTEROPTS_HTTP_PORT = PropertyInitializer.newProperty("masterOptions.httpPort", 9647);
    public static final Property<Integer> MASTEROPTS_WS_PORT = PropertyInitializer.newProperty("masterOptions.wsPort", 9648);
    // Prevent Instantiation
    private ServerConfiguration() {}
}

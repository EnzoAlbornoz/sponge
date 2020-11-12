package br.ufsc.sponge.server.repositories;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import br.ufsc.sponge.server.config.ServerConfiguration;
import br.ufsc.sponge.server.repositories.*;
import ch.jalu.configme.SettingsManager;

public class FileRepository {

    
    private FileRepository() {
        cachedFiles = new HashMap<String, VirtualFile>();
    };

    private boolean initialized = false;
    private SettingsManager settings;
    private HashMap<String, VirtualFile> cachedFiles;
    
    private static FileRepository single_instance = null;

    public static FileRepository getInstance() {
        if (single_instance == null)
            single_instance = new FileRepository();
        return single_instance;
    }

    private Path getSharedFolderPath() {
        var sharedFolderLocation = settings.getProperty(ServerConfiguration.INSTANCE_SHARED_FOLDER)
            .replaceFirst("^~", System.getProperty("user.home"));
        return Paths.get(sharedFolderLocation).toAbsolutePath().normalize();
    }

    public void initialize(SettingsManager settings) throws IOException {
        // Initialize Settings
        this.settings = settings;
        // Initialize Files Cache
        var sFolderPath = this.getSharedFolderPath();
        if (this.isStoragePresent()) {
            // Load file paths
            var filePaths = Files.newDirectoryStream(sFolderPath);
            // Process files
            for (Path filePath : filePaths) {
                // Read File Content
                var physicalFile = filePath.toFile();
                // Transforms it into consumable file
                var vFile = new VirtualFile(physicalFile);
                // Add vfile to cache
                this.cachedFiles.put(vFile.getId(), vFile);
            }
        } else {
            // Create folder
            Files.createDirectories(sFolderPath);
        }
        // Mark as Initialized
        this.initialized = true;
    }

    public List<VirtualFile> listFiles() throws IOException {
        return new ArrayList<VirtualFile>(this.cachedFiles.values());
    }

    // public VirtualFile createFile(String fileId, byte[] content ) throws IOException {
    //     var sharedFolderLocation = settings.
    //         getProperty(ServerConfiguration.INSTANCE_SHARED_FOLDER)
    //         .replaceFirst("^~", System.getProperty("user.home"));
    //     Path sharedFolderPath = Paths.get(sharedFolderLocation).toAbsolutePath().normalize();
    //     if (!this.isStoragePresent()) {
    //         Files.createDirectories(sharedFolderPath);   
    //     }
    //     Path file_path = sharedFolderPath.resolve();
    //     if (file_path.toFile().exists())
    //     {
    //         throw new IOException("File already exists " + file.toString());
    //     } else
    //     {
            
    //         // File new_file = new File(sharedFolderPath.toString()+"/"+file);
            
    //     }
    //     // sharedFolderPath.
    // }

    private boolean isStoragePresent() {
        var sharedFolderLocation = settings.
            getProperty(ServerConfiguration.INSTANCE_SHARED_FOLDER)
            .replaceFirst("^~", System.getProperty("user.home"));
        Path sharedFolderPath = Paths.get(sharedFolderLocation).toAbsolutePath().normalize();
        return sharedFolderPath.toFile().exists();
    }

}
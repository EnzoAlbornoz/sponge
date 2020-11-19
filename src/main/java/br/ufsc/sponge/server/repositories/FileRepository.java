package br.ufsc.sponge.server.repositories;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

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

    public VirtualFile createFile(String name, Long date, byte[] content) throws IOException {
        VirtualFile newFile = new VirtualFile(name, date, content);
        Path sharedFolderPath = this.getSharedFolderPath();
        System.out.println(sharedFolderPath.resolve(newFile.toFileNameHash()));
        try {
            // Start Transaction
            this.cachedFiles.put(newFile.getId(), newFile);
            // Actually write file to the FS
            var fileWriter = new FileOutputStream(sharedFolderPath.resolve(newFile.toFileNameHash()).toFile());
            fileWriter.write(newFile.getContent().get());
            fileWriter.close();
            // End Transaction
            return newFile;
        } catch (Exception e) {
            // Rollback
            this.cachedFiles.remove(newFile.getId());
            throw e;
        }
    }

    public VirtualFile getFile(String fileId) throws IOException {
        var cachedFile = this.cachedFiles.get(fileId);
        if (cachedFile.getContent().isEmpty()) {
            // Read File Content
            System.out.println("reading file");
            var contentStream = new FileInputStream(
                new File(
                    this.getSharedFolderPath().toFile(),
                    cachedFile.toFileNameHash()
                )
            );
            var content = contentStream.readAllBytes();
            cachedFile.setContent(content);
        }
        return cachedFile;
    }
    public void updateFile(VirtualFile modifiedFile, VirtualFile originalFile) throws IOException {
        Path sharedFolderPath = this.getSharedFolderPath();
        System.out.println(sharedFolderPath.resolve(modifiedFile.toFileNameHash()));
        try {
            // Start Transaction
            this.cachedFiles.put(modifiedFile.getId(), modifiedFile);
            // Actually write file to the FS
            var fileWriter = new FileOutputStream(sharedFolderPath.resolve(modifiedFile.toFileNameHash()).toFile());
            fileWriter.write(modifiedFile.getContent().get());
            fileWriter.close();
            // End Transaction
        } catch (Exception e) {
            // Rollback
            throw e;
        }
    }

    private boolean isStoragePresent() {
        var sharedFolderLocation = settings.
            getProperty(ServerConfiguration.INSTANCE_SHARED_FOLDER)
            .replaceFirst("^~", System.getProperty("user.home"));
        Path sharedFolderPath = Paths.get(sharedFolderLocation).toAbsolutePath().normalize();
        return sharedFolderPath.toFile().exists();
    }

}
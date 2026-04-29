package fileexplorer.fileexplorer.provider;

import fileexplorer.fileexplorer.model.LocalItem;
import fileexplorer.fileexplorer.model.StorageItem;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LocalProvider implements StorageProvider {

    @Override
    public List<StorageItem> listContents(String parentId) throws Exception {
        List<StorageItem> items = new ArrayList<>();
        Path path = Paths.get(parentId);

        try (Stream<Path> stream = Files.list(path)) {
            stream.forEach(p -> {
                File f = p.toFile();
                items.add(new LocalItem(
                        f.getName(),
                        p.toAbsolutePath().toString(),
                        f.isDirectory()
                ));
            });
        }
        return items;
    }

    @Override
    public void delete(StorageItem storageItem) throws Exception {
        Path path = Paths.get(storageItem.getId()).toAbsolutePath().normalize();
        String pathStr = path.toString().toLowerCase();
        String userHome = System.getProperty("user.home").toLowerCase();

        List<String> forbidden = Arrays.asList(
                userHome,
                userHome + File.separator + "desktop",
                userHome + File.separator + "documents",
                userHome + File.separator + "downloads",
                "c:\\", "c:\\windows", "c:\\program files", "c:\\program files (x86)"
        );

        if (path.getParent() == null || forbidden.contains(pathStr)) {
            throw new Exception("Rendszermappa törlése letiltva: " + pathStr);
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            if (!Desktop.getDesktop().moveToTrash(path.toFile())) {
                throw new Exception("Lomtárba helyezés sikertelen!");
            }
        } else {
            throw new Exception("Lomtár nem érhető el!");
        }
    }

    @Override
    public void rename(StorageItem storageItem, String newName) throws Exception {
        Path source = Paths.get(storageItem.getId());
        Files.move(source, source.resolveSibling(newName));
    }

    @Override
    public InputStream download(StorageItem storageItem) throws Exception {
        return Files.newInputStream(Paths.get(storageItem.getId()));
    }

    @Override
    public void upload(String parentId, String name, InputStream data) throws Exception {
        Path target = Paths.get(parentId, name);
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public String createFolder(String parentId, String folderName) throws Exception {
        Path newDir = Paths.get(parentId, folderName);
        try{
            Files.createDirectory(newDir);

            return newDir.toAbsolutePath().toString();
        }catch(FileAlreadyExistsException e){
            throw new Exception("A mappa már létezik.");
        }
        catch (IOException e){
            throw new Exception("Hiba: " +e.getMessage());
        }
    }
}
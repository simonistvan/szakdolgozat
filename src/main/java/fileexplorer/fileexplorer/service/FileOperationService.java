package fileexplorer.fileexplorer.service;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.concurrent.Task;
import java.io.InputStream;
import java.util.List;

public class FileOperationService {

    public void copySingle(StorageItem item, StorageProvider src, StorageProvider dest, String destPath) throws Exception {
        if(item.isFolder()){
            String newFolderId = dest.createFolder(destPath, item.getName());
            List<StorageItem> children = src.listContents(item.getId());
            for(StorageItem child : children){
                copySingle(child, src, dest, newFolderId);
            }
        }
        else{
            try(InputStream in = src.download(item)){
                dest.upload(destPath, item.getName(), in);
            }
        }
    }

    public Task<Void> copyTask(List<StorageItem> items, StorageProvider src, StorageProvider dest, String destPath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < items.size(); i++) {
                    StorageItem item = items.get(i);
                    updateMessage("Másolás: " + item.getName());
                    updateProgress(i + 1, items.size());
                    copySingle(item, src, dest, destPath);
                }
                return null;
            }
        };
    }

    public Task<Void> deleteTask(List<StorageItem> items, StorageProvider provider) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < items.size(); i++) {
                    StorageItem item = items.get(i);
                    updateMessage("Törlés: " + item.getName());
                    updateProgress(i + 1, items.size());
                    provider.delete(item);
                }
                return null;
            }
        };
    }

    public Task<Void> moveTask(List<StorageItem> items, StorageProvider src, StorageProvider dest, String destPath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for(int i = 0; i < items.size(); i++){
                    StorageItem item = items.get(i);

                    updateMessage("Áthelyezés: " + item.getName());
                    updateProgress(i + 1, items.size());

                    copySingle(item, src, dest, destPath);
                    src.delete(item);

                    if(isCancelled()) break;
                }
                return null;
            }
        };
    }

    public Task<Void> renameTask(StorageItem item, StorageProvider provider, String newName){
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Átnevezés: " +  item.getName() + " -> " + newName);
                provider.rename(item, newName);

                return null;
            }
        };
    }
}
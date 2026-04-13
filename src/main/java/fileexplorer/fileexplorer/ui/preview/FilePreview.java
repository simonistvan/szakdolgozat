package fileexplorer.fileexplorer.ui.preview;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.scene.Node;

public interface FilePreview {
    boolean canHandle(StorageItem item);
    Node createNode(StorageItem item, StorageProvider provider);
}

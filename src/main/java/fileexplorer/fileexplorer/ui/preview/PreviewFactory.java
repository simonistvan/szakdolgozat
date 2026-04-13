package fileexplorer.fileexplorer.ui.preview;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;

public class PreviewFactory {
    private final List<FilePreview> previewers = new ArrayList<>();

    public PreviewFactory() {
        previewers.add(new FolderPreviewer());
        previewers.add(new ImagePreviewer());
        previewers.add(new TextPreviewer());
        previewers.add(new DefaultPreviewer());
    }

    public Node getPreviewer(StorageItem item, StorageProvider provider) {
        if(item == null) return null;

        for(FilePreview preview : previewers) {
            if(preview.canHandle(item)){
                return preview.createNode(item, provider);
            }
        }

        return new Label("Nincs elérhető előnézet");
    }
}

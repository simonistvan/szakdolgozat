package fileexplorer.fileexplorer.ui.preview;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TextPreviewer implements FilePreview{
    @Override
    public boolean canHandle(StorageItem item) {
        String name = item.getName();
        return name.endsWith(".txt") || name.endsWith(".java") || name.endsWith(".py") || name.endsWith(".cpp") || name.endsWith(".xml") || name.endsWith(".json");
    }

    @Override
    public Node createNode(StorageItem item, StorageProvider provider) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(20));

        Label iconLabel = new Label("\uD83D\uDCC4" + item.getName());
        iconLabel.setStyle("-fx-font-size: 32px");

        TextArea text = new TextArea();
        text.setWrapText(true);
        text.setEditable(false);
        text.setStyle("-fx-font-family: 'Consolas', 'Monospaced';");

        VBox.setVgrow(text, Priority.ALWAYS);
        box.getChildren().addAll(iconLabel, text);

        new Thread(() -> {
            try(InputStream is = provider.download(item);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String content = reader.lines()
                        .limit(1000)
                        .collect(Collectors.joining("\n"));

                Platform.runLater(() -> text.setText(content));
            }
            catch(Exception ex){
                Platform.runLater(() -> text.setText(ex.getMessage()));
                ex.printStackTrace();
            }
        }).start();

        return box;
    }
}

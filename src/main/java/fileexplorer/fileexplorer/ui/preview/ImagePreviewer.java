package fileexplorer.fileexplorer.ui.preview;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.File;

public class ImagePreviewer implements FilePreview {
    @Override
    public boolean canHandle(StorageItem item) {
        if(item.isFolder()){ return false;}
        String name = item.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".docx") || name.endsWith(".pdf") || name.endsWith(".xlsx");
    }

    @Override
    public Node createNode(StorageItem item, StorageProvider provider) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(20));

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 24px;");

        Label pathLabel = new Label(item.getId());
        pathLabel.setWrapText(true);
        pathLabel.setStyle("-fx-font-size: 14px; -fx-font-style: italic; -fx-text-fill: gray;");

        box.getChildren().addAll(nameLabel, pathLabel);

        String url = item.getThumbnailUrl();

        boolean isCloud = url.startsWith("http");
        boolean isImage = item.getName().toLowerCase().matches(".*\\.(png||jpg||jpeg||gif)$");

        if(isCloud || isImage){
            if(isCloud && url.contains("=s")){
                url = url.replaceAll("=s\\d+", "=s1000");
            }

            Image image = new Image(url, true);
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.fitWidthProperty().bind(box.widthProperty().subtract(40));

            ScrollPane sp = new ScrollPane(imageView);
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            VBox.setVgrow(sp, Priority.ALWAYS);
            box.getChildren().add(sp);
        }
        else{
            VBox infoBox = new VBox(10);
            infoBox.setAlignment(Pos.CENTER);

            Label iconLabel = new Label("\uD83D\uDCC4" + item.getName());
            iconLabel.setStyle("-fx-font-size: 32px");

            Label messageLabel = new Label("Ehhez a fájlhoz nem érhető el előnézet");
            messageLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: gray;");

            Button btn = new javafx.scene.control.Button("Megnyitás");
            btn.setOnAction(e->{
                try{
                    java.awt.Desktop.getDesktop().open(new java.io.File(item.getId()));
                }
                catch (java.io.IOException e1){
                    e1.printStackTrace();
                }
            });
            btn.setStyle("-fx-background-color: #3399FF; -fx-font-size: 20px; -fx-text-fill: white; -fx-font-grow: bold");

            infoBox.getChildren().addAll(iconLabel, messageLabel, btn);
            VBox.setVgrow(infoBox, Priority.ALWAYS);
            box.getChildren().add(infoBox);
        }
        return box;
    }
}

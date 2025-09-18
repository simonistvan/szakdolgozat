module fileexplorer.fileexplorer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens fileexplorer.fileexplorer to javafx.fxml;
    exports fileexplorer.fileexplorer;
}
module fileexplorer.fileexplorer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires com.google.api.client.json.jackson2;
    requires com.google.api.client;
    requires com.google.api.services.drive;
    requires com.google.api.client.auth;
    requires google.api.client;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires commons.logging;

    opens fileexplorer.fileexplorer to javafx.fxml;
    exports fileexplorer.fileexplorer;
}
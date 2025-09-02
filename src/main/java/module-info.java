module com.szakdolgozat.szakdolgozat {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    opens com.szakdolgozat.szakdolgozat to javafx.fxml;
    exports com.szakdolgozat.szakdolgozat;
}
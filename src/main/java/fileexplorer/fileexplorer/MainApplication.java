package fileexplorer.fileexplorer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * A kétpaneles fájlkezelő alkalmazás ({@code FileExplorer}) fő belépési osztálya.
 * Az osztály a JavaFX {@link Application} keretrendszerére épülve inicializálja és elindítja
 * az alkalmazás grafikus felhasználói felületét (GUI). Felelőssége a fő nézetet leíró
 * {@code main-view-panels.fxml} erőforrás betöltése, valamint az elsődleges alkalmazásablak
 * ({@link Stage}) konfigurálása és megjelenítése.
 *
 * @author Simon István
 */

public class MainApplication extends Application {
    /**
     * A JavaFX futtatókörnyezet hívja meg az alkalmazás indulásakor. Betölti az
     * alapértelmezett felhasználói felületet leíró nézetdefiníciót, létrehozza és
     * konfigurálja a fő jelenetet ({@link Scene}), majd megjeleníti az elsődleges
     * ablakot ({@link Stage}).
     *
     * @param stage az elsődleges ablaka az alkalmazásnak, amelyet a JavaFX keretrendszer biztosít.
     * @throws IOException ha a {@code main-view-panels.fxml} fájl betöltése sikertelen
     */

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view-panels.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1500, 1000);
        stage.setTitle("FileExplorer");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Az alkalmazás standard Java belépési pontja.
     *
     * @param args a parancssori indítási argumentumok tömbje
     */
    public static void main(String[] args) {
        launch();
    }
}
package fileexplorer.fileexplorer.provider;

import fileexplorer.fileexplorer.model.DriverItem;
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

/**
 * A {@link StorageProvider} implementációja ez az osztály. A lokális meghajtókon szereplú
 * fájlok kezelésére készült el.
 * Lokális meghajtókon tárolt fájlok listázását, mappába rendezését, törlését, átnevezését, másolását
 * és fájlok vagy mappák átnevezését valósítja meg.
 * **/

public class LocalProvider implements StorageProvider {

    /**
     * Az aktuális, azaz a paraméterben átadott elérési útvonalon tárolt fájlokat és mappákat listázza ki.
     *
     * @param parentId paraméterben átadott elérési útvonalat reprezentáló String változó
     * @return Egy StorageItem típusú tömb a visszatérési értéke. amely tartalmazza az aktuális elérési útvonalon tárolt
     * fájlok adatait.
     * @throws Exception Exception Ha az útvonal érvénytelen, nem mappa, vagy I/O hiba történik a beolvasás során.
     */

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

    /**
     * Ez a metódus valósítja meg a törlést, azonban a fájl végleges törlése helyett az állomány a
     * lomtárba kerül.
     *
     *
     * @param storageItem a paraméterben átadott változó, amely a törlendő elemet reprezentálja
     * @throws Exception Ha az elem védett rendszermappa, a lomtár funkció nem támogatott a rendszeren,
     *                   vagy a lomtárba helyezés sikertelen.
     */
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

        if (Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            if (!Desktop.getDesktop().moveToTrash(path.toFile())) {
                throw new Exception("Lomtárba helyezés sikertelen!");
            }
        } else {
            throw new Exception("Lomtár nem érhető el!");
        }
    }

    /**
     * Ez a metódus felel a programban a lokális fájlokon kívánt átnevezés megvalósításához.
     * Paraméterben megkapja az új nevet és az átnevezendő állományt reprezentáló StorageItem példányt.
     *
     * @param storageItem az átnevezendő állományt reprezentáló StorageItem példány
     * @param newName az új nevet tartalmazó String változó
     * @throws Exception Ha az átnevezés során I/O hiba lép fel, vagy már létezik ilyen nevű elem.
     */

    @Override
    public void rename(StorageItem storageItem, String newName) throws Exception {
        Path source = Paths.get(storageItem.getId());
        Files.move(source, source.resolveSibling(newName));
    }

    /**
     * Megnyit egy bemeneti adatfolyamot (InputStream) a megadott helyi fájl olvasásához.
     *
     * @param storageItem A beolvasandó (letöltendő) fájl.
     * @return A fájl tartalmát szolgáltató {@link InputStream}.
     * @throws Exception Ha a fájl nem létezik, nem olvasható, vagy könyvtárra mutat.
     */

    @Override
    public InputStream download(StorageItem storageItem) throws Exception {
        return Files.newInputStream(Paths.get(storageItem.getId()));
    }

    /**
     * Létrehoz vagy felülír egy fájlt a kapott adatfolyam tartalmával.
     *
     * @param parentId a céélkönyvtár elérési útvonala
     * @param name a létrehozandó állomány neve
     * @param data A fájlba írandó adatokat tartalmazó {@link InputStream}.
     * @throws Exception Ha az írás során I/O hiba történik.
     */

    @Override
    public void upload(String parentId, String name, InputStream data) throws Exception {
        Path target = Paths.get(parentId, name);
        Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Új mappát hoz létre a megadott szülő könyvtáron belül.
     *
     * @param parentId  A forrás könyvtár abszolút elérési útvonala.
     * @param folderName Az új mappa neve.
     * @return A sikeresen létrehozott mappa abszolút elérési útvonala.
     * @throws Exception Ha a mappa már létezik, vagy egyéb I/O hiba miatt nem hozható létre.
     */

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
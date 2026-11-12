package fileexplorer.fileexplorer.provider;

import fileexplorer.fileexplorer.model.StorageItem;

import java.io.InputStream;
import java.util.List;

/**
 * Ez lesz az ősosztály a programban használt Providerek számára.
 * A Google Drive és a lokális fájlok kezelésére szolgáló osztályokban
 * ennek az interfacenek a metódusait fogom
 */

public interface StorageProvider {

    /**
     * Kilistázza a megadott szülő mappában található elemeket (fájlokat és almappákat).
     *
     * @param parentId A szülő mappa egyedi azonosítója vagy elérési útvonala.
     * @return A mappában található {@link StorageItem} elemek listája.
     * @throws Exception Ha a lekérdezés során hiba történik (pl. nem létező mappa, jogosultsági vagy hálózati hiba).
     */
    List<StorageItem> listContents(String parentId) throws Exception;

    /**
     * Törli a megadott elemet (fájlt vagy mappát) a tárhelyről.
     *
     * @param storageItem A törölni kívánt elem.
     * @throws Exception Ha a törlés nem engedélyezett vagy sikertelen.
     */
    void delete(StorageItem storageItem) throws Exception;

    /**
     * Átnevezi a megadott elemet.
     *
     * @param storageItem Az átnevezendő elem.
     * @param newName     Az elem új neve.
     * @throws Exception Ha az átnevezés sikertelen (pl. már létezik ilyen nevű elem vagy érvénytelen a név).
     */
    void rename(StorageItem storageItem, String newName) throws Exception;

    /**
     * Bemeneti adatfolyamot nyit a megadott fájl tartalmának letöltéséhez vagy olvasásához.
     *
     * @param storageItem A letöltendő fájl.
     * @return A fájl tartalmát szolgáltató {@link InputStream}.
     * @throws Exception Ha a fájl nem érhető el vagy nem olvasható.
     */
    InputStream download(StorageItem storageItem) throws Exception;

    /**
     * Feltölt vagy létrehoz egy fájlt a megadott célmappában a kapott adatfolyam alapján.
     *
     * @param parentId A célmappa azonosítója vagy elérési útvonala.
     * @param name     A létrehozandó fájl neve.
     * @param data     A fájl tartalmát hordozó {@link InputStream}.
     * @throws Exception Ha a feltöltés vagy írás során hiba lép fel.
     */
    void upload(String parentId, String name, InputStream data) throws Exception;

    /**
     * Új mappát hoz létre a megadott szülő mappán belül.
     *
     * @param parentId   A szülő mappa azonosítója vagy elérési útvonala.
     * @param folderName Az új mappa neve.
     * @return A létrehozott mappa egyedi azonosítója vagy elérési útvonala.
     * @throws Exception Ha a mappa létrehozása sikertelen (pl. már létezik ilyen nevű mappa).
     */
    String createFolder(String parentId, String folderName) throws Exception;
}

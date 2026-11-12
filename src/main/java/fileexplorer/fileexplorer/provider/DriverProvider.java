package fileexplorer.fileexplorer.provider;

import com.google.api.services.drive.model.File;
import fileexplorer.fileexplorer.model.DriverItem;
import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.service.GoogleDriveManager;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A {@link StorageProvider} implementációja ez az osztály. Google Drive specifikus
 * kialakitásának köszönhetően ez az osztály bonyolítja le a kommunikációt az alkalmazás
 * tárhelykezelő rétege és a Google Drive API V3 között.
 * A Google Drive szerverin tárolt állományokat {@link DriverItem} objektumként kezeli.
 * **/


public class DriverProvider implements StorageProvider {

    private final GoogleDriveManager manager;

    /**
     * @param manager egy {@link GoogleDriveManager} példánya, amely final jelzővel lett ellátva
     *                ennek a példánynak a legfőbb dolga, hogy a Google Drive API műveleteit
     *                végrehajtsa.
     */

    public DriverProvider(GoogleDriveManager manager) {
        this.manager = manager;
    }

    /**
     * {@inheritDoc}
     * <p>
     *     Amennyiben a {@code parentId} értéke {@code null}, üres vagy
     *     {@code "Google Drive}, akkor az aktuális gyökérkönyvtár első 100 elemét lekéri.
     *     Ez esetben az első 100 elem a Google Drive meghajtorol kerül betöltésre.
     *     A mappákat és a fájlokat a Google Drive MIME type alapján kezeli.
     * </p>
     */

    @Override
    public List<StorageItem> listContents(String parentId) throws Exception {
        List<File> files = (parentId == null || parentId.isEmpty() || parentId.equalsIgnoreCase("Google Drive"))
                ? manager.listFilesInRoot(100)
                :manager.listFilesInFolder(parentId,100);

        return files.stream()
                .map(f -> new DriverItem(
                        f.getId(),
                        f.getName(),
                        "application/vnd.google-apps.folder".equals(f.getMimeType()),
                        f.getThumbnailLink()
                ))
                .collect(Collectors.toList());
    }

    /**
     {@inheritDoc}
     <p>
     A törlést a {@link GoogleDriveManager#deleteDriveSmartOne(String)} metóduson keresztül végzi el az egyedi azonosító
     (@param fileId) alapján.
     </p>
     */

    @Override
    public void delete(StorageItem storageItem) throws Exception {
        manager.deleteDriveSmartOne(storageItem.getId());
    }


    /**
     {@inheritDoc}
     <p>
     Az átnevezéshez a {@link GoogleDriveManager#renameFile(String, String)} metódust hívom segítségül, melynek paraméterben átadom
     a fájl egyedi azonosítóját és az új nevet.
     </p>

     @param storageItem az átnevezendő állomány reprezentálása
     @param newName az átnevezendő fájl új neve amire át szeretnénk nevezni az állományt
     @throws Exception ha hálózato hiba történik, az új név megegyezik a mappában található egy másik állomány nevével.
     */
    @Override
    public void rename(StorageItem storageItem, String newName) throws Exception {
        manager.renameFile(storageItem.getId(), newName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A megadott Google Drive állomány tartalmát közvetlen adatfolyamként ({@link InputStream})
     * kéri le a Google Drive API-n keresztül. A művelet a {@link com.google.api.services.drive.Drive#files()}
     * erőforrás-elérő metóduson keresztül érhető el, ahol a fájl egyedi azonosítója
     * ({@link StorageItem#getId()}) alapján felépített lekérdezést az
     * {@code executeMediaAsInputStream()} metódus hajtja végre.
     * </p>
     *
     * @param storageItem a letölteni kívánt távoli állományt reprezentáló {@link StorageItem} objektum
     * @return a fájl nyers bájttartalmát szolgáltató {@link InputStream} adatfolyam
     * @throws Exception ha hálózati hiba történik, a fájl nem található, vagy az API-hívás meghiúsul
     */


    @Override
    public InputStream download(StorageItem storageItem) throws Exception {
        return manager.getDriveService().files()
                .get(storageItem.getId())
                .executeMediaAsInputStream();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Létrehozok egy üres Google Drive File objektumot. Miután ez elkészült aktuális elérési útvonal alapján megadom,
     * milyen mappaszerkezetben legyen feltöltve az állomány.
     * </p>
     * <p>
     * Az adatot egy {@link com.google.api.client.http.InputStreamContent} objektumba
     * csomagolja {@code application/octet-stream} MIME-típussal, majd a Google Drive API
     * {@link com.google.api.services.drive.Drive#files()} metódusán keresztül meghívott
     * {@code create()} metódussal végrehajtja a feltöltést (támogatva a megosztott meghajtókat is).
     * </p>
     *
     * @param parentId a célmappa egyedi Google Drive azonosítója; {@code null}, üres string vagy
     *                 {@code "Google Drive"} esetén a gyökérkönyvtárba ({@code "root"}) tölti fel a fájlt
     * @param name     a feltöltendő állomány létrehozandó neve a Google Drive-on
     * @param data     a feltöltendő fájl nyers bájttartalmát biztosító {@link InputStream} adatfolyam
     * @throws Exception ha hálózati hiba történik, nincs jogosultság a célmappába íráshoz,
     *                   vagy a Google Drive API kérés meghiúsul
     */

    @Override
    public void upload(String parentId, String name, InputStream data) throws Exception {
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(name);

        String effectiveParentId = parentId;
        if (parentId == null || parentId.isEmpty() || parentId.equalsIgnoreCase("Google Drive")) {
            effectiveParentId = "root";
        }

        if (!effectiveParentId.equalsIgnoreCase("root")) {
            fileMetadata.setParents(java.util.Collections.singletonList(effectiveParentId));
        } else {
            fileMetadata.setParents(java.util.Collections.singletonList("root"));
        }

        com.google.api.client.http.InputStreamContent mediaContent =
                new com.google.api.client.http.InputStreamContent("application/octet-stream", data);

        manager.getDriveService().files()
                .create(fileMetadata, mediaContent)
                .setFields("id")
                .setSupportsAllDrives(true)
                .execute();
    }


    /**
     *
     * @param parentId a célmappa elérési útvonala, ha a gyökérkönyvtárban vagyunk akkor null, ellenkező esetben a parentId változó értéke.
     * @param folderName az új mappának a neve
     * @return az elkészült mappának az azonosítója
     * @throws Exception ha hálózati hiba történik, nincs jogosultság a célmappába íráshoz,
     *                    vagy a Google Drive API kérés meghiúsul
     */
    @Override
    public String createFolder(String parentId, String folderName) throws Exception {
        String effectiveParent = (parentId == null || parentId.isEmpty() || parentId.equals("root")) ? null : parentId;
        return manager.createDriveFolder(folderName, effectiveParent);
    }
}

package fileexplorer.fileexplorer.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.api.services.drive.model.About;
import fileexplorer.fileexplorer.auth.GoogleDriveAuth;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.List;


/**
 * Ez az osztály felelős a Google Drive API műveleteinek a megvalósításáért.
 * A program futása során a {@link Drive} példány kerül meghívásra a műveletek elvégzéséhez.
 * A főbb metódusaia törlés. fájlok listázása, jogosultságok ellenőrzése, hozzáférési jogosultságok visszavonása.
 *
 */
public class GoogleDriveManager {

    /**
     * Google Drive API műveletek végrehajtásáért felelős példány.
     */

    private Drive driveService;

    /**
     * Létrehozza a {@code GoogleDriveManager} egy új példányát, és inicializálja
     * a hozzáférési munkamenetet a {@link GoogleDriveAuth} osztály segítségével.
     *
     * @throws IOException Amennyiben az azonosítási tokenek betöltése vagy a hálózati kommunikáció meghiúsul.
     * @throws GeneralSecurityException Amennyiben a TLS/SSL kontextus vagy a hitelesítési kulcsok feldolgozása biztonsági hibát okoz.
     */

    public GoogleDriveManager() throws IOException, GeneralSecurityException {
        this.driveService = GoogleDriveAuth.getDriveService();
    }

    /**
     * Visszaadja az authentikált {@link Drive} példányt.
     * @return az authentikált {@link Drive} pédány, vagy {@code null} ha kijelentkezés történt
     */

    public Drive getDriveService() {
        return driveService;
    }

    /**
     * Ezzel a metódussal kérdezem le az olyan állományokat a Google Drive szervereiről, amelyek
     * a gyökérkönyvtárban szerepelnek. Lomtárban tárolt fájlokat nem tölti be.
     * Azonban a mezőszelekciónak köszönhetően a megosztott meghajtókon szereplő állományokhoz is hozzáfér.
     *
     * @param limit a maximálisan lekérdezhető állományszám.
     * @return A lekérdezett {@link File} objektumok listája.
     * @throws IOException Hálózati hiba vagy érvénytelen API válasz esetén.
     */

    public List<File> listFilesInRoot(int limit) throws IOException {
        FileList result = driveService.files().list()
                .setQ("trashed = false")
                .setPageSize(limit)
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id, name, mimeType, parents, thumbnailLink)")
                .execute();
        return result.getFiles();
    }

    /**
     * Paraméterként átadott mappa tartalmát listázza ki
     *
     * @param folderId kilistázandó mappa azonosítója.
     * @param limit a maximálisan lekérdezhető állományszám.
     * @return A megadott mappában lévő {@link File} elemek listája.
     * @throws IOException IOException Hálózati hiba vagy érvénytelen API válasz esetén.
     */

    public List<File> listFilesInFolder(String folderId, int limit) throws IOException {
        String query = "'" + folderId + "' in parents";
        FileList result = driveService.files().list()
                .setQ(query)
                .setPageSize(limit)
                .setFields("files(id, name, mimeType, parents, thumbnailLink)")
                .execute();
        return result.getFiles();
    }

    /**
     * Lekérdezi és visszaadja a jelenleg bejelentkezett felhasználó e-mail címét.
     *
     * @return a bejelentkezett felhasználó e-mail címe.
     * @throws IOException Amennyiben a lekérdezés hibás.
     */

    public String getEmail() throws IOException {
        About about = driveService.about()
                .get()
                .setFields("user(emailAddress)")
                .execute();

        return about.getUser().getEmailAddress();
    }

    /**
     * Megvizsgálja, hogy a megadott azonosítójú állománynak a bejelentkezett felhasználó a kizárolagos tulajdonos-e.
     *
     * @param fileId vizsgálandó fájl azonosítója
     * @return {@code true}, ha az aktuális felhasználó a tulajdonos; {@code false}, ha az elem megosztott vagy más birtokolja.
     * @throws IOException Ha az állomány nem található, vagy a metaadatok lekérése meghiúsul.
     */

    public boolean isOwnedByMe(String fileId) throws IOException {
        File meta = driveService.files()
                .get(fileId)
                .setSupportsAllDrives(true)
                .setFields("ownedByMe,name")
                .execute();

        boolean owned = Boolean.TRUE.equals(meta.getOwnedByMe());
        System.out.println("[DELETE] " + meta.getName() + " -> " + (owned ? "Tulajdonos" : "Megosztott"));
        return owned;
    }

    /**
     * Eltávolítja a bejelentkezett felhasználó hozzáférését a paraméterben megadott Google Drive fájlról.
     * A metódus lekéri az aktuális felhasználó e-mail-címét, majd a fájlazonosító alapján
     * lekérdezi a fájl jogosultságainak listáját. Ezt követően egy szűréssel megkeresi
     * a listában a bejelentkezett felhasználó e-mail-címéhez tartozó egyéni ({@code "user"})
     * jogosultságot.
     * Amennyiben a jogosultsági lista {@code null}, vagy a felhasználó nem szerepel a jogosultak
     * között, a metódus módosítás nélkül visszatér. Ha a jogosultság megtalálható, a metódus
     * a jogosultság egyedi azonosítója alapján törli azt a Google Drive API segítségével.
     *
     * @param fileId paraméterben átadott, vizsgálandó fájl
     * @throws IOException a hiba történik a Google Drive API-val való kommunikáció során
     */

    public void removeFromFile(String fileId) throws IOException {

        About about = driveService.about().get()
                .setFields("user(emailAddress)")
                .execute();
        String myEmail = about.getUser().getEmailAddress();

        PermissionList plist = driveService.permissions()
                .list(fileId)
                .setSupportsAllDrives(true)
                .setFields("permissions(id,emailAddress,type)")
                .execute();

        if (plist.getPermissions() == null) return;

        Permission mine = plist.getPermissions().stream()
                .filter(p -> "user".equals(p.getType()))
                .filter(p -> p.getEmailAddress() != null && myEmail.equalsIgnoreCase(p.getEmailAddress()))
                .findFirst()
                .orElse(null);

        if (mine == null) return;

        driveService.permissions()
                .delete(fileId, mine.getId())
                .setSupportsAllDrives(true)
                .execute();
    }

    /**
     * Ez a metódus végzi a Google Drive elemek törlését. Mivel Google Drive-on találhatóak megosztott elemek is,
     * nem csak olyanok, amiket mi hoztunk létre, azaz nem mi vagyunk a kizárólagos tulajdonosai a fájlnak, így ezt
     * kétféleképpen kell kezelni.
     * A metóüdus indulásának az elejénél, ha található parancsikon, akkor megpróbálja a lomtárba helyezni ezt.
     * Ezután egy boolean változóba az isOwnedByMe metódus visszatérési értéke alapján fogja megvizsgálni a tulajdonosi
     * vagy megoszottsági tulajdonságot.
     * Amennyiben mi vagyunk a tulajdonosok, akkor a Google Drive a lomtárba helyezi az állományt.
     * Amennyiben nem mi vagyunk a tulajdonosok, akkor a {@link #removeFromFile(String)} metódus segítségével levesszük a felhasználót a
     * jogosultak közül kezelve az esetleges 403 és 404 hibákat.
     * (403: Nincs jogosultság, 404: nem található állomány).
     *
     * @param fileId paraméterben átadott, törlendő fájl azonosítója
     */

    public void deleteDriveSmartOne(String fileId) {
        try {
            trashShortcutIfExists(fileId);
            boolean owned = isOwnedByMe(fileId);
            if (owned) {
                com.google.api.services.drive.model.File patch = new com.google.api.services.drive.model.File();
                patch.setTrashed(true);

                driveService.files()
                        .update(fileId, patch)
                        .setSupportsAllDrives(true)
                        .execute();

                System.out.println("A(z) "+ fileId + " a lomtárba lett helyezve.");
            } else {
                try {
                    removeFromFile(fileId);
                    System.out.println("Állomány törölve!");
                } catch (GoogleJsonResponseException ex) {
                    if (ex.getStatusCode() == 403) {
                        System.out.println("403 policy, nem lehetséges módosítani a jogosultságot.");
                    } else if (ex.getStatusCode() == 404) {
                        System.out.println("404 (nincs már hozzáférés / nem létezik)");
                    } else {
                        throw ex;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[DELETE] hiba: " + e.getMessage());
        }
    }

    /**
     * Paraméterben kapott fájlhoz tartozó parancsikonokat a lomtárba helyezi.
     * Egy Google Drive API lekérdezést futatt, a megfelelő paraméterezéssel, melynek során
     * a paraméterben kapott fájlra mutató parancsikonokat a lomtárba helyezi. A lekérdezés a megoszott
     * meghajtókat is figyelembe veszi a lekérdezés.
     *
     * @param targetFileId annak az állománynak az azonosítója, amelyre a parancsikonokat keressük.
     * @return {@code true} ha találtunk parancsikont és a kukába került; {@code false} amennyiben nem találtunk parancsikont
     * @throws IOException ha hiba történik a Google Drive API-val való kommunikáció során
     */

    public boolean trashShortcutIfExists(String targetFileId) throws IOException {
        String q = "mimeType='application/vnd.google-apps.shortcut' and " +
                "shortcutDetails.targetId='" + targetFileId + "' and trashed=false";

        FileList shortcuts = driveService.files().list()
                .setQ(q)
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id,name,ownedByMe,shortcutDetails(targetId))")
                .execute();

        if (shortcuts.getFiles() == null || shortcuts.getFiles().isEmpty()) {
            System.out.println("[DELETE] nincs shortcut ehhez (nem tudom eltüntetni programból).");
            return false;
        }

        for (File sc : shortcuts.getFiles()) {
            File patch = new File();
            patch.setTrashed(true);

            driveService.files()
                    .update(sc.getId(), patch)
                    .setSupportsAllDrives(true)
                    .execute();

            System.out.println("[DELETE] shortcut kukába: " + sc.getName());
        }
        return true;
    }

    /**
     * A paraméterben átadott fájlazonosítójú állományt átnevezi a paraméterben kapott String változó értékére.
     * A metódus létrehoz egy üres File objektumot, melynek beállítja a paraméterben kapott nevet.
     * A Google Drive API a {@code files.update} end-pointján keresztül módosítja az állomány nevét.
     *
     * @param fileId paraméterben átadott állomány, melynek a nevét szeretnénk módosítani.
     * @param newName az a String változó, amely tartalmazza az új nevet.
     * @throws IOException ha hiba történik a Google Drive API-val való kommunikáció során.
     */

    public void renameFile(String fileId, String newName) throws IOException{
        File file = new File();
        file.setName(newName);

        driveService.files().update(fileId, file)
                .setSupportsAllDrives(true)
                .execute();
    }

    /**
     * Új mappát hoz létre paraméterben kapott névvel ellátva, a paraméterben kapott elérési útvonalon.
     *
     * @param folderName az a String változó, amely tartalmazza az új mappa nevét.
     * @param parentId az új létrehozandó mappa elérési útvonala
     * @return a frissen létrehozott Google Drive mappa egyedi azonosítója.
     * @throws IOException ha hiba történik a Google Drive API-val való kommunikáció során
     */

    public String createDriveFolder(String folderName, String parentId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        if (parentId != null && !parentId.isEmpty()) {
            fileMetadata.setParents(java.util.Collections.singletonList(parentId));
        }

        File folder = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }


    /**
     * Megszünteti az aktív Google Drive API munkamenetet az aktuális példányban.
     */

    public void logout() {
        this.driveService = null;
    }

}

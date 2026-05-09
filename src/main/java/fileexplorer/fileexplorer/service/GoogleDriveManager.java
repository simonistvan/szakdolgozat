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

public class GoogleDriveManager {

    private Drive driveService;

    public GoogleDriveManager() throws IOException, GeneralSecurityException {
        this.driveService = GoogleDriveAuth.getDriveService();
    }

    public Drive getDriveService() {
        return driveService;
    }

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

    public List<File> listFilesInFolder(String folderId, int limit) throws IOException {
        // Lekérdezés: csak azok a fájlok, amelyeknek a szülője a folderId
        String query = "'" + folderId + "' in parents";
        FileList result = driveService.files().list()
                .setQ(query)
                .setPageSize(limit)
                .setFields("files(id, name, mimeType, parents, thumbnailLink)")
                .execute();
        return result.getFiles();
    }

    //E-mail getter
    public String getEmail() throws IOException {
        About about = driveService.about()
                .get()
                .setFields("user(emailAddress)")
                .execute();

        return about.getUser().getEmailAddress();
    }

    //Tulajdonosság ellenőrzés
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


    //Törlés a file hozzáférői közül
    public void removeFromFile(String fileId) throws IOException {

        // saját email
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

    public void deleteDriveSmartOne(String fileId) {
        try {
            trashShortcutIfExists(fileId);
            boolean owned = isOwnedByMe(fileId);
            if (owned) {
                // owner -> trash
                com.google.api.services.drive.model.File patch = new com.google.api.services.drive.model.File();
                patch.setTrashed(true);

                driveService.files()
                        .update(fileId, patch)
                        .setSupportsAllDrives(true)
                        .execute();

                System.out.println("[DELETE] owner -> kukába rakva");
            } else {
                // shared -> próbáljuk levenni a saját hozzáférést
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

    public void renameFile(String fileId, String newName) throws IOException{
        File file = new File();
        file.setName(newName);

        driveService.files().update(fileId, file)
                .setSupportsAllDrives(true)
                .execute();
    }

    public String createDriveFolder(String folderName, String parentId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        // Ha a parentId null, a Drive gyökerébe kerül
        if (parentId != null && !parentId.isEmpty()) {
            fileMetadata.setParents(java.util.Collections.singletonList(parentId));
        }

        File folder = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }


    public void logout() {
        this.driveService = null;
    }

}

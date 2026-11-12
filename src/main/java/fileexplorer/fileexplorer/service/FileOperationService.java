package fileexplorer.fileexplorer.service;

import fileexplorer.fileexplorer.model.StorageItem;
import fileexplorer.fileexplorer.provider.StorageProvider;
import javafx.concurrent.Task;
import java.io.InputStream;
import java.util.List;

/**
 * Ez az osztály, amely megvalósítja a fájlműveleteket. Ilyen fájlműveletek a
 * másolás, törlés, átnevezés vagy az áthelyezés.
 * Az osztályban a metódusok külön háttérszálakon futnak {@link Task}, így a
 * program futása közben a GUI nem nem kerük blokkolásra.
 */

public class FileOperationService {

    /**
     * Egyetlen egy fájl átmásolását végzi ez a metódus.
     * A metódus paraméterben megkapja a másolandó állományt. Megnézi, hogy a másolandó elem egy mappa vagy egy fájl.
     * Mivel mappa esetén rekurzívan szeretnénk másolni, ezért létrehozzuk a cél helyen egy azonos nevű mappát, majd
     * az eredeti mappa tartalmát egy StorageItemeket tároló listába betöltjük és a lista elemeire egyesével egy for cikluson keresztük
     * meghívom a metódust.
     *
     * @param item A másolandó fájl vagy mappa.
     * @param src A forrástárhely szolgáltatója.
     * @param dest A céltárhely szolgáltatója.
     * @param destPath A célmappa elérési útja a céltárhelyen.
     * @throws Exception Ha bármilyen hálózati, I/O vagy tárhelyszolgáltatói hiba lép fel a művelet során.
     */

    public void copySingle(StorageItem item, StorageProvider src, StorageProvider dest, String destPath) throws Exception {
        if(item.isFolder()){
            String newFolderId = dest.createFolder(destPath, item.getName());
            List<StorageItem> children = src.listContents(item.getId());
            for(StorageItem child : children){
                copySingle(child, src, dest, newFolderId);
            }
        }
        else{
            try(InputStream in = src.download(item)){
                dest.upload(destPath, item.getName(), in);
            }
        }
    }

    /**
     * Háttérfolyamatot készít a másoláshoz. A feladat futása során frissíti a státuszt a felhasználó fel
     *
     * @param items a másolandó elemek listája
     * @param src a forrástárhely szolgáltatója.
     * @param dest a céltárhely szolgáltatója.
     * @param destPath a célmappa elérési útvonala.
     * @return egy {@link Task} amely elvégzi a másolást.
     */

    public Task<Void> copyTask(List<StorageItem> items, StorageProvider src, StorageProvider dest, String destPath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < items.size(); i++) {
                    StorageItem item = items.get(i);
                    updateMessage("Másolás: " + item.getName());
                    updateProgress(i + 1, items.size());
                    copySingle(item, src, dest, destPath);
                }
                return null;
            }
        };
    }

    /**
     * Ez a metódus egy háttérfolyamatot hoz létre az átadott törlendő feladatok törlésére.
     *
     * @param items a törlendő elemek listája.
     * @param provider a törlendő elemek szolgáltatója.
     * @return {@link Task} egy háttérfeladat amely törli az elemet és frissíti a státuszt.
     */

    public Task<Void> deleteTask(List<StorageItem> items, StorageProvider provider) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < items.size(); i++) {
                    StorageItem item = items.get(i);
                    updateMessage("Törlés: " + item.getName());
                    updateProgress(i + 1, items.size());
                    provider.delete(item);
                }
                return null;
            }
        };
    }

    /**
     * Háttérfeladatot hoz létre több fájl vagy mappa kötegelt áthelyezéséhez.
     * Az áthelyezendő elemek listáján végig megy, és minden elemre meghívja a {@link copySingle} metódust,
     * amely átmásolja majd az eredeti fájlra meghívja a szolgáltató törlési metódusát, amely törli az eredeti állományt.
     * Ezzel előidézve az "áthelyezés" érzetét.
     *
     *
     * @param items    az áthelyezendő elemek listája.
     * @param src      a forrástárhely szolgáltatója.
     * @param dest     a céltárhely szolgáltatója.
     * @param destPath a célmappa azonosítója vagy elérési útja a céltárhelyen.
     * @return {@link Task}, amely elindítva elvégzi az áthelyezést és jelenti az előrehaladást.
     */

    public Task<Void> moveTask(List<StorageItem> items, StorageProvider src, StorageProvider dest, String destPath) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for(int i = 0; i < items.size(); i++){
                    StorageItem item = items.get(i);

                    updateMessage("Áthelyezés: " + item.getName());
                    updateProgress(i + 1, items.size());

                    copySingle(item, src, dest, destPath);
                    src.delete(item);

                    if(isCancelled()) break;
                }
                return null;
            }
        };
    }

    /**
     * Háttérfeladatot hoz létre egyetlen fájl vagy mappa átnevezéséhez.
     *
     * @param item az átnevezendő elem.
     * @param provider az átnevezendő elem szolgáltatója.
     * @param newName az új név, amelyre át szeretnénk nevezni az eredeti fájlt.
     * @return egy {@link Task}, amely elindítva végrehajtja az átnevezést.
     */

    public Task<Void> renameTask(StorageItem item, StorageProvider provider, String newName){
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Átnevezés: " +  item.getName() + " -> " + newName);
                provider.rename(item, newName);

                return null;
            }
        };
    }
}
package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Represents a StagingArea object.
 *
 *  @author Ashley Zheng
 */
public class StagingArea implements Serializable {
    private HashMap<String, FileItem> addition;
    private HashMap<String, FileItem> deletion;
    private String gitletFolder;
    private static final String STAGING_AREA = "staging_area";

    public StagingArea(String gitletDirectory) {
        gitletFolder = gitletDirectory;
        addition = new HashMap<>();
        deletion = new HashMap<>();
    }

    /**
     * Adds a file to addition hashmap.
     * @param fileName      Name of file
     * @param fileContents  Serialized content of file
     * @param blobs         Blobs class
     */
    public void stageAdd(String fileName, byte[] fileContents, Blobs blobs) {
        String blobID = blobs.addBlob(fileName, fileContents);
        FileItem addFile = new FileItem(fileName, blobID);
        addition.put(fileName, addFile);
    }

    /**
     * Removes a file from addition hashmap.
     * @param fileName  Name of file
     * @return Boolean of file existence
     */
    public boolean unstageAdd(String fileName) {
        if (!addition.containsKey(fileName)) {
            return false;
        }
        addition.remove(fileName);
        return true;
    }

    /**
     * Adds a file to deletion hashmap.
     * @param fileName  Name of file
     */
    public void stageDelete(String fileName) {
        FileItem deleteFile = new FileItem(fileName, "");
        deletion.put(fileName, deleteFile);
    }

    /**
     * Removes a file from deletion hashmap.
     * @param fileName  Name of file
     * @return Boolean of file existence
     */
    public boolean unstageDelete(String fileName) {
        if (!deletion.containsKey(fileName)) {
            return false;
        }
        deletion.remove(fileName);
        return true;
    }

    /**
     * Returns hashmap of files to add.
     * @return HashMap of files to add
     */
    public HashMap<String, FileItem> getAddition() {
        return addition;
    }

    /**
     * Returns hashmap of files to delete.
     * @return HashMap of files to delete
     */
    public HashMap<String, FileItem> getDeletion() {
        return deletion;
    }

    /**
     * Returns whether the staging area is empty or not.
     * @return  Boolean
     */
    public boolean isEmpty() {
        return addition.isEmpty() && deletion.isEmpty();
    }

    /**
     * Clears out the staging area after a commit.
     */
    public void clear() {
        addition.clear();
        deletion.clear();
    }

    /**
     * Reads the staging area file from the Gitlet directory.
     * @param gitletDirectory   .gitlet directory.
     * @return  StagingArea object
     */
    public static StagingArea fromFile(String gitletDirectory) {
        File stagingArea = Utils.join(gitletDirectory, STAGING_AREA);
        if (!stagingArea.exists()) {
            return new StagingArea(gitletDirectory);
        }
        return Utils.readObject(stagingArea, StagingArea.class);
    }

    /**
     * Writes the StagingArea class to the corresponding file
     * in the .gitlet directory.
     */
    public void toFile() {
        File stagingArea = Utils.join(gitletFolder, STAGING_AREA);
        Utils.writeObject(stagingArea, this);
    }
}

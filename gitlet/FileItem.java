package gitlet;

import java.io.Serializable;

/**
 * Represents a File object.
 *
 *  @author Ashley Zheng
 */
public class FileItem implements Serializable {
    private String fileName;
    private String blobID;

    /**
     * Creates a FileItem object that holds a file's name and blobID.
     * @param fileName  Name of file
     * @param blobID    blob ID string
     */
    public FileItem(String fileName, String blobID) {
        this.fileName = fileName;
        this.blobID = blobID;
    }

    /**
     * Gets and returns file name of the file item.
     * @return  Name of file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets and returns the blobID of the file item.
     * @return  Unique BlobID
     */
    public String getBlobID() {
        return blobID;
    }
}

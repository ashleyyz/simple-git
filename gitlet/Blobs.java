package gitlet;

import java.io.File;

/**
 * Represents a Blob object.
 *
 *  @author Ashley Zheng
 */
public class Blobs {

    /** The Blobs directory. */
    public static final File BLOBS_DIR = Utils.join(Repository.GITLET_DIR, "blobs");

    /**
     * Blobs constructor for a Blobs directory in Gitlet.
     * @param gitletDirectory .gitlet Directory
     */
    public Blobs(File gitletDirectory) {
        if (!BLOBS_DIR.exists()) {
            BLOBS_DIR.mkdir();
        }
    }

    /**
     * This method will serialize a version of a file
     * and store the version under Blobs directory.
     * @param fileName      Name of file
     * @param fileContents  Serialized contents of file
     * @return              Blob ID
     */
    public String addBlob(String fileName, byte[] fileContents) {
        String blobID = calculateBlobID(fileName, fileContents);
        File addBlob = Utils.join(BLOBS_DIR, blobID);
        Utils.writeContents(addBlob, fileContents);
        return blobID;
    }

    /**
     * This method reads Blob file from Blobs directory.
     * @param blobID    SHA-ID for Blob
     * @return          Serialized contents of file
     */
    public byte[] readBlob(String blobID) {
        File blobFile = Utils.join(BLOBS_DIR, blobID);
        return Utils.readContents(blobFile);
    }

    /**
     * This method calculates a unique BlobID.
     * @param fileName      Name of file
     * @param fileContents  Serialized contents of file
     * @return              BlobID
     */
    public String calculateBlobID(String fileName, byte[] fileContents) {
        return Utils.sha1(fileName, fileContents);
    }

}

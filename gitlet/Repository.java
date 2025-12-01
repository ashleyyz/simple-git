package gitlet;

import java.io.File;
import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 *
 *  @author Ashley Zheng
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** Instance of the Blobs class. */
    private Blobs blobs;

    /** Instance of the StagingArea class. */
    private StagingArea stageArea;

    /** Instance of the CommitArea class. */
    private CommitArea commitArea;

    /**
     * Constructs a new .gitlet directory or retrieves .gitlet information.
     */
    public Repository() {
        if (!GITLET_DIR.exists()) {
            Main.exitWithError("Not in an initialized Gitlet directory.");
        }
        blobs = new Blobs(GITLET_DIR);
        stageArea = StagingArea.fromFile(GITLET_DIR.getAbsolutePath());
        commitArea = CommitArea.fromFile(GITLET_DIR.getAbsolutePath());
    }

    /**
     * Gets and returns Blobs instance.
     * @return blobs
     */
    public Blobs getBlobs() {
        return blobs;
    }

    /**
     * Gets and returns StagingArea instance.
     * @return stageArea
     */
    public StagingArea getStageArea() {
        return stageArea;
    }

    /**
     * Gets and returns CommitArea instance.
     * @return commitArea
     */
    public CommitArea getCommitArea() {
        return commitArea;
    }
}

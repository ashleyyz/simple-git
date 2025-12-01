package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Represents a gitlet commit area. This class will handle all the internal
 * work for commands that look at the commit area.
 *
 *  @author Ashley Zheng
 */
public class CommitArea implements Serializable {
    /**
     * Represents a CommitNode object.
     * It carries all the metadata for each commit created in Gitlet.
     */
    public class CommitNode implements Serializable {
        private CommitNode parent;
        private CommitNode mergedParent;
        private String commitID;
        private String commitMessage;
        private Date timestamp;
        private HashMap<String, FileItem> blobsMap;

        /**
         * Creates the initial Commit.
         */
        public CommitNode() {
            parent = null;
            commitMessage = "initial commit";
            timestamp = new Date(0L);
            blobsMap = new HashMap<>();
            commitID = calculateCommitID();
        }

        /**
         * Constructs a new commit node and generates all of its
         * metadata from the parameters and its parent's metadata.
         * @param parentNode       The parent node
         * @param message          The commit message
         * @param addition         Staged for addition files
         * @param deletion         Staged for deletion files.
         */
        public CommitNode(CommitNode parentNode, String message, HashMap<String,
                FileItem> addition, HashMap<String, FileItem> deletion) {
            this.parent = parentNode;
            this.commitMessage = message;
            this.timestamp = new Date();
            this.blobsMap = (HashMap<String, FileItem>) parentNode.blobsMap.clone();

            this.blobsMap.putAll(addition);
            for (String fileName : deletion.keySet()) {
                this.blobsMap.remove(fileName);
            }
            this.commitID = calculateCommitID();
        }

        /**
         * Sets a second parent for a merged CommitNode.
         * @param mergedNode  Parent CommitNode
         */
        public void setMergedParent(CommitNode mergedNode) {
            this.mergedParent = mergedNode;
        }

        /**
         * Gets the CommitNodes blobMap and returns it.
         * @return  Hashmap of Blobs
         */
        public HashMap<String, FileItem> getBlobsMap() {
            return blobsMap;
        }

        /**
         * This method calculates a unique commit ID.
         * @return commit ID
         */
        public String calculateCommitID() {
            return Utils.sha1(commitMessage, timestamp.toString(), blobsMap.toString());
        }
    }
    private HashMap<String, CommitNode> branches;
    private HashMap<String, CommitNode> allCommits;
    private CommitNode head;
    private String currBranch;
    private static final String COMMIT_FILE = "commits";
    private String gitletFolder;

    /**
     * Constructs a new CommitArea object for Gitlet.
     * @param gitletDirectory   .gitlet Directory
     */
    public CommitArea(String gitletDirectory) {
        gitletFolder = gitletDirectory;
        currBranch = "main";
        branches = new HashMap<>();
        branches.put(currBranch, new CommitNode());
        head = branches.get(currBranch);
        allCommits = new HashMap<>();
        allCommits.put(head.commitID, head);
    }

    /**
     * Gets and returns the HEAD of gitlet.
     * @return  Head CommitNode
     */
    public CommitNode getHead() {
        return head;
    }

    /**
     * This method will add a new CommitNode, update branch/HEAD
     * pointers, and clear out the staging area.
     * @param stage   Staging Area
     * @param message Commit message
     * @return        New CommitNode
     */
    public CommitNode addCommit(StagingArea stage, String message) {
        head = new CommitNode(head, message, stage.getAddition(), stage.getDeletion());
        branches.put(currBranch, head);
        allCommits.put(head.commitID, head);
        stage.clear();
        return head;
    }

    /**
     * Stages the given file for deletion and removes the file from
     * the working directory.
     * @param fileName  Name of file
     * @param stage     StagingArea
     */
    public void removeFile(String fileName, StagingArea stage) {
        if (!head.blobsMap.containsKey(fileName)) {
            Main.exitWithError("No reason to remove the file.");
        }
        stage.stageDelete(fileName);
        File remove = Utils.join(Repository.CWD, fileName);
        Utils.restrictedDelete(remove);
    }

    /**
     * Helper method to create a log message from the given CommitNode.
     * @param node  CommitNode object
     * @return      Corresponding log message
     */
    private String commitLog(CommitNode node) {
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        return "===\n" + "commit " + node.commitID + "\n"
                + "Date: " + format.format(node.timestamp) + "\n"
                + node.commitMessage + "\n\n";
    }

    /**
     * Return a message of all the commits in the
     * current branch.
     * @return  Log message
     */
    public String log() {
        String log = "";
        for (CommitNode node = head; node != null; node = node.parent) {
            log += commitLog(node);
        }
        return log.substring(0, log.length() - 1);
    }

    /**
     * Returns a message of all the commits made in Gitlet history.
     * @return  Global log message
     */
    public String globalLog() {
        String log = "";
        for (CommitNode node : allCommits.values()) {
            log += commitLog(node);
        }
        return log.substring(0, log.length() - 1);
    }

    /**
     * This method will return a string of a list of IDs for all
     * commits containing the given commit message.
     * @param message   Commit message
     * @return          Message list of CommitNode IDs
     */
    public String findCommitMessage(String message) {
        String commitIDs = "";
        for (CommitNode node : allCommits.values()) {
            if (node.commitMessage.equals(message)) {
                commitIDs += node.commitID + "\n";
            }
        }
        if (commitIDs.equals("")) {
            Main.exitWithError("Found no commit with that message.");
        }
        return commitIDs;
    }

    /**
     * Return a message listing the branches and tracked/untracked files.
     * @param stage     StagingArea
     * @return          Status message
     */
    public String status(StagingArea stage) {
        String message = "=== Branches ===\n";
        TreeMap<String, CommitNode> sorted = new TreeMap<>(branches);
        Iterator itr = sorted.keySet().iterator();
        while (itr.hasNext()) {
            String key = (String) itr.next();
            if (key.equals(currBranch)) {
                message += "*" + key + "\n";
            } else {
                message += key + "\n";
            }
        }
        message += "\n=== Staged Files ===\n";

        TreeMap<String, FileItem> sortAdd = new TreeMap<>(stage.getAddition());
        Iterator itrAdd = sortAdd.keySet().iterator();
        while (itrAdd.hasNext()) {
            String key = (String) itrAdd.next();
            message += key + "\n";
        }
        message += "\n=== Removed Files ===\n";

        TreeMap<String, FileItem> sortDel = new TreeMap<>(stage.getDeletion());
        Iterator itrDel = sortDel.keySet().iterator();
        while (itrDel.hasNext()) {
            String key = (String) itrDel.next();
            message += key + "\n";
        }
        message += "\n=== Modifications Not Staged For Commit ===\n";
        message += "\n=== Untracked Files ===\n";
        message += "\n";

        return message;
    }

    /**
     * Change the file in working directory to HEAD commit version.
     * @param fileName  Name of file
     * @param blobs     Blobs
     */
    public void checkoutFile(String fileName, Blobs blobs) {
        if (!head.blobsMap.containsKey(fileName)) {
            Main.exitWithError("File does not exist in that commit.");
        }
        String blobID = head.blobsMap.get(fileName).getBlobID();
        File checkout = Utils.join(Repository.CWD, fileName);
        Utils.writeContents(checkout, blobs.readBlob(blobID));
    }

    /**
     * Change the file in working directory to specific commitID version.
     * @param fileName  Name of file
     * @param commitID  Unique commit ID
     * @param blobs     Blobs
     */
    public void checkoutFile(String fileName, String commitID, Blobs blobs) {
        CommitNode node = findCommit(commitID);
        if (node == null) {
            Main.exitWithError("No commit with that id exists.");
        }
        if (!node.blobsMap.containsKey(fileName)) {
            Main.exitWithError("File does not exist in that commit.");
        }

        String blobID = node.blobsMap.get(fileName).getBlobID();
        File checkout = Utils.join(Repository.CWD, fileName);
        Utils.writeContents(checkout, blobs.readBlob(blobID));
    }

    /**
     * Change all files in the working directory to head of the
     * branch's file versions.
     * @param branchName    Name of branch
     * @param blobs         Blobs
     * @param stage         StagingArea
     */
    public void checkoutBranch(String branchName, Blobs blobs, StagingArea stage) {
        if (!branches.containsKey(branchName)) {
            Main.exitWithError("No such branch exists.");
        }
        if (branchName.equals(currBranch)) {
            Main.exitWithError("No need to checkout the current branch.");
        }
        CommitNode branchHead = branches.get(branchName);
        for (String fileName : untrackedFiles(head)) {
            if (branchHead.blobsMap.containsKey(fileName)) {
                Main.exitWithError("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        for (Map.Entry<String, FileItem> entry : branchHead.blobsMap.entrySet()) {
            File checkout = Utils.join(Repository.CWD, entry.getKey());
            String blobID = entry.getValue().getBlobID();
            Utils.writeContents(checkout, blobs.readBlob(blobID));
        }
        for (Map.Entry<String, FileItem> entry : head.blobsMap.entrySet()) {
            if (!branchHead.blobsMap.containsKey(entry.getKey())) {
                Utils.restrictedDelete(entry.getKey());
            }
        }
        stage.clear();
        currBranch = branchName;
        head = branches.get(currBranch);
    }

    /**
     * Helper method to find all the currently untracked files
     * in the working directory.
     * @param node  CommitNode
     * @return      List of untracked file names
     */
    private List<String> untrackedFiles(CommitNode node) {
        List<String> fileNames = Utils.plainFilenamesIn(Repository.CWD);
        List<String> untrackedFiles = new ArrayList<>();
        for (String fileName : fileNames) {
            if (!node.blobsMap.containsKey(fileName)) {
                untrackedFiles.add(fileName);
            }
        }
        return untrackedFiles;
    }

    /**
     * Helper method to find a specific CommitNode given a commitID.
     * @param commitID  Unique commitID
     * @return          Corresponding CommitNode
     */
    private CommitNode findCommit(String commitID) {
        CommitNode node = allCommits.get(commitID);
        if (node != null) {
            return node;
        }
        for (String id : allCommits.keySet()) {
            if (id.startsWith(commitID)) {
                return allCommits.get(id);
            }
        }
        return null;
    }

    /**
     * Add a new branch to commit area.
     * @param branchName    Name of branch
     */
    public void addBranch(String branchName) {
        if (branches.containsKey(branchName)) {
            Main.exitWithError("A branch with that name already exists.");
        }
        branches.put(branchName, head);
    }

    /**
     * Removes the branch from commit area.
     * @param branchName    Name of branch
     */
    public void removeBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            Main.exitWithError("A branch with that name does not exist.");
        }
        if (branchName.equals(currBranch)) {
            Main.exitWithError("Cannot remove the current branch.");
        }
        branches.remove(branchName);
    }

    /**
     * Checks out all the files from the given commitID
     * and removes tracked files not present in the given commit.
     * @param commitID    Unique CommitID
     * @param blobs       Blobs
     * @param stage       StagingArea
     */
    public void reset(String commitID, Blobs blobs, StagingArea stage) {
        CommitNode node = findCommit(commitID);
        if (node == null) {
            Main.exitWithError("No commit with that id exists.");
        }
        List<String> untracked = untrackedFiles(head);
        for (String fileName : untracked) {
            if (node.blobsMap.containsKey(fileName)) {
                Main.exitWithError("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        for (Map.Entry<String, FileItem> entry : node.blobsMap.entrySet()) {
            File checkout = Utils.join(Repository.CWD, entry.getKey());
            String blobID = entry.getValue().getBlobID();
            Utils.writeContents(checkout, blobs.readBlob(blobID));
        }
        for (Map.Entry<String, FileItem> entry : head.blobsMap.entrySet()) {
            if (!node.blobsMap.containsKey(entry.getKey())) {
                Utils.restrictedDelete(entry.getKey());
            }
        }
        stage.clear();
        head = node;
        branches.put(currBranch, head);
    }

    /**
     * Helper method to find the split point between the given branch and current branch.
     * @param otherHead     Other branch head
     * @return              Split Point CommitNode
     */
    private CommitNode findSplitPoint(CommitNode otherHead) {
        HashSet<CommitNode> otherSet = ancestors(otherHead);
        HashSet<CommitNode> currSet = ancestors(head);
        otherSet.retainAll(currSet);
        CommitNode splitPoint = null;
        for (CommitNode node : otherSet) {
            if (splitPoint == null || node.timestamp.compareTo(splitPoint.timestamp) > 0) {
                splitPoint = node;
            }
        }
        return splitPoint;
    }

    /**
     * Helper method to find all the ancestors of a given CommitNode.
     * @param node  CommitNode
     * @return      HashSet of ancestors
     */
    private HashSet<CommitNode> ancestors(CommitNode node) {
        HashSet<CommitNode> nodes = new HashSet<>();
        if (node == null) {
            return nodes;
        }
        nodes.add(node);
        nodes.addAll(ancestors(node.parent));
        nodes.addAll(ancestors(node.mergedParent));
        return nodes;
    }

    /**
     * Merges files in the given branch into the current branch.
     * @param otherBranch   Other branch name
     * @param blobs         Blobs
     * @param stage         Staging Area
     */
    public void merge(String otherBranch, Blobs blobs, StagingArea stage) {
        CommitNode otherHead = branches.get(otherBranch);
        if (!stage.isEmpty()) {
            Main.exitWithError("You have uncommitted changes.");
        }
        if (otherHead == null) {
            Main.exitWithError("A branch with that name does not exist.");
        }
        if (otherBranch.equals(currBranch)) {
            Main.exitWithError("Cannot merge a branch with itself.");
        }
        for (String fileName : untrackedFiles(head)) {
            if (otherHead.blobsMap.containsKey(fileName)) {
                Main.exitWithError("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        CommitNode splitPoint = findSplitPoint(otherHead);
        if (splitPoint.equals(otherHead)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitPoint.equals(head)) {
            checkoutBranch(otherBranch, blobs, stage);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        if (mergeBranches(splitPoint, head, otherHead, blobs, stage)) {
            System.out.println("Encountered a merge conflict.");
        }
        CommitNode mergedCommit = addCommit(stage, "Merged " + otherBranch
                + " into " + currBranch + ".");
        mergedCommit.setMergedParent(otherHead);
    }

    /**
     * Helper method to merge every file in the head commit of the given
     * branch into the current branch.
     * @param splitNode     Split point of the branches
     * @param currHead      Head commit of the current branch
     * @param otherHead     Head commit of the given branch
     * @param blobs         Blobs
     * @param stage         Staging Area
     * @return              Conflicted merge boolean
     */
    private Boolean mergeBranches(CommitNode splitNode, CommitNode currHead, CommitNode otherHead,
                               Blobs blobs, StagingArea stage) {
        HashMap<String, FileItem> splitMap = splitNode.blobsMap;
        HashMap<String, FileItem> otherMap = otherHead.blobsMap;
        HashMap<String, FileItem> headMap = currHead.blobsMap;
        HashSet<FileItem> allFiles = new HashSet<>(headMap.values());
        allFiles.addAll(otherMap.values());

        Boolean conflict = false;
        for (FileItem item : allFiles) {
            String fileName = item.getFileName();
            FileItem splitItem = splitMap.get(fileName);
            FileItem otherItem = otherMap.get(fileName);
            FileItem headItem = headMap.get(fileName);
            if (mergeFile(fileName, otherHead, splitItem, otherItem, headItem, blobs, stage)) {
                conflict = true;
            }
        }
        return conflict;
    }

    /**
     * Helper method to merge each individual file based on the whether it is modified or
     * unmodified or deleted in the given branch, current branch, and split point.
     * @param fileName      Name of file
     * @param otherHead     Head commit of other branch
     * @param splitItem     FileItem of the split point file
     * @param otherItem     FileItem of the other branch file
     * @param headItem      FileItem of the current branch file
     * @param blobs         Blobs
     * @param stage         Staging Area
     * @return              Conflicted merge boolean
     */
    private Boolean mergeFile(String fileName, CommitNode otherHead, FileItem splitItem,
                           FileItem otherItem, FileItem headItem, Blobs blobs, StagingArea stage) {
        if (otherItem == null) {
            if (headItem == null || splitItem == null) {
                return false;
            }
            if (headItem.getBlobID().equals(splitItem.getBlobID())) {
                removeFile(fileName, stage);
                return false;
            }
            conflictMerge(fileName, headItem, otherItem, blobs, stage);
            return true;
        }
        if (headItem == null) {
            if (splitItem == null) {
                byte[] contents = blobs.readBlob(otherItem.getBlobID());
                stage.stageAdd(fileName, contents, blobs);
                File file = Utils.join(Repository.CWD, fileName);
                Utils.writeContents(file, contents);
                return false;
            }
            if (otherItem.getBlobID().equals(splitItem.getBlobID())) {
                return false;
            }
            conflictMerge(fileName, headItem, otherItem, blobs, stage);
            return true;
        }
        if (splitItem == null) {
            if (headItem.getBlobID().equals(otherItem.getBlobID())) {
                return false;
            }
            conflictMerge(fileName, headItem, otherItem, blobs, stage);
            return true;
        }

        if (!headItem.getBlobID().equals(splitItem.getBlobID())
                && otherItem.getBlobID().equals(splitItem.getBlobID())) {
            return false;
        }
        if (!otherItem.getBlobID().equals(splitItem.getBlobID())
                && headItem.getBlobID().equals(splitItem.getBlobID())) {
            byte[] contents = blobs.readBlob(otherItem.getBlobID());
            stage.stageAdd(fileName, contents, blobs);
            File file = Utils.join(Repository.CWD, fileName);
            Utils.writeContents(file, contents);
            return false;
        }
        conflictMerge(fileName, headItem, otherItem, blobs, stage);
        return true;
    }

    /**
     * Helper method that does a conflicted merge.
     * @param fileName      Name of file
     * @param headItem      FileItem of the current branch head file
     * @param otherItem     FileItem of the other branch head file
     * @param blobs         Blobs
     * @param stage         StagingArea
     */
    private void conflictMerge(String fileName, FileItem headItem, FileItem otherItem,
                               Blobs blobs, StagingArea stage) {
        File conflictFile = Utils.join(Repository.CWD, fileName);
        byte[] headContent = new byte[0];
        if (headItem != null) {
            headContent = blobs.readBlob(headItem.getBlobID());
        }
        byte[] otherContent = new byte[0];
        if (otherItem != null) {
            otherContent = blobs.readBlob(otherItem.getBlobID());
        }
        Utils.writeContents(conflictFile, "<<<<<<< HEAD\n",
                headContent, "=======\n",
                otherContent, ">>>>>>>\n");
        stage.stageAdd(fileName, Utils.readContents(conflictFile), blobs);
    }

    /**
     * Reads the CommitArea file from the Gitlet Directory.
     * @param gitletFolder  .gitlet Directory
     * @return              CommitArea object
     */
    public static CommitArea fromFile(String gitletFolder) {
        File commitFile = Utils.join(gitletFolder, COMMIT_FILE);
        if (!commitFile.exists()) {
            return new CommitArea(gitletFolder);
        }
        return Utils.readObject(commitFile, CommitArea.class);
    }

    /**
     * Writes the CommitArea object to the corresponding file
     * under .gitlet directory.
     */
    public void toFile() {
        File commitFile = Utils.join(gitletFolder, COMMIT_FILE);
        Utils.writeObject(commitFile, this);
    }
}

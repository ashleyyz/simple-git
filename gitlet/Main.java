package gitlet;

import java.io.File;
import java.util.HashMap;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 *  @author Ashley Zheng
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                init(args);
                break;
            case "add":
                add(args);
                break;
            case "commit":
                commit(args);
                break;
            case "rm":
                remove(args);
                break;
            case "branch":
                branch(args);
                break;
            case "rm-branch":
                removeBranch(args);
                break;
            case "log":
                log(args);
                break;
            case "global-log":
                globalLog(args);
                break;
            case "reset":
                reset(args);
                break;
            case "merge":
                merge(args);
                break;
            case "find":
                find(args);
                break;
            case "status":
                status(args);
                break;
            case "checkout":
                checkout(args);
                break;
            default:
                exitWithError("No command with that name exists.");
        }
    }

    /**
     * Initializes a Gitlet Repository program.
     * @param args  'init' command
     */
    public static void init(String[] args) {
        validateNumArgs(args, 1, 1);
        if (Repository.GITLET_DIR.exists()) {
            exitWithError("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
        Repository.GITLET_DIR.mkdir();
    }

    /**
     * Adds a file to the gitlet staging area.
     * @param args  'add [file name]' command
     */
    public static void add(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        String fileName = args[1];
        if (repo.getStageArea().getDeletion().containsKey(fileName)) {
            recoverStageDelete(repo, fileName);
            repo.getStageArea().toFile();
            return;
        }
        File addFile = Utils.join(Repository.CWD, fileName);
        if (!addFile.exists()) {
            exitWithError("File does not exist.");
        }
        byte[] fileContents = Utils.readContents(addFile);
        String fileBlobId = repo.getBlobs().calculateBlobID(args[1], fileContents);
        HashMap<String, FileItem> headBlobs = repo.getCommitArea().getHead().getBlobsMap();
        if (headBlobs.containsKey(args[1])) {
            if (headBlobs.get(args[1]).getBlobID().equals(fileBlobId)) {
                repo.getStageArea().unstageAdd(args[1]);
                repo.getStageArea().toFile();
                return;
            }
        }
        repo.getStageArea().stageAdd(args[1], fileContents, repo.getBlobs());
        repo.getStageArea().toFile();
    }

    /**
     * Commits files in the staging area to the gitlet commit area.
     * @param args  'commit [message]' command
     */
    public static void commit(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        if (repo.getStageArea().isEmpty()) {
            exitWithError("No changes added to the commit.");
        }
        if (args[1].equals("")) {
            exitWithError("Please enter a commit message.");
        }
        repo.getCommitArea().addCommit(repo.getStageArea(), args[1]);
        repo.getCommitArea().toFile();
        repo.getStageArea().toFile();
    }

    /**
     * Removes a file from the staging area if staged,
     * or removes file from the working directory.
     * @param args  'rm [file name]' command
     */
    public static void remove(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        if (!repo.getStageArea().unstageAdd(args[1])) {
            repo.getCommitArea().removeFile(args[1], repo.getStageArea());
        }
        repo.getCommitArea().toFile();
        repo.getStageArea().toFile();
    }

    /**
     * Prints a log of the current branches commits.
     * Starting from the current commit until the initial commit.
     * @param args  'log' command
     */
    public static void log(String[] args) {
        validateNumArgs(args, 1, 1);
        Repository repo = new Repository();
        String logMessage = repo.getCommitArea().log();
        System.out.println(logMessage);
    }

    /**
     * Prints a global log of all the commits made in any order.
     * @param args  'global-log' command
     */
    public static void globalLog(String[] args) {
        validateNumArgs(args, 1, 1);
        Repository repo = new Repository();
        String globalMessage = repo.getCommitArea().globalLog();
        System.out.println(globalMessage);
    }

    /**
     * Finds and prints out the commitIDs of all commits
     * containing the given commit message.
     * @param args 'find [commit message]' command
     */
    public static void find(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        String find = repo.getCommitArea().findCommitMessage(args[1]);
        System.out.println(find);
    }

    /**
     * Prints out the status of the branches, staged files,
     * and modified/untracked files.
     * @param args  'status' command
     */
    public static void status(String[] args) {
        validateNumArgs(args, 1, 1);
        Repository repo = new Repository();
        String status = repo.getCommitArea().status(repo.getStageArea());
        System.out.println(status);
    }

    /**
     * Overwrites files in the working directory from a given
     * file name, branch, or commit ID/file name.
     * @param args  'checkout ...' command
     */
    public static void checkout(String[] args) {
        validateNumArgs(args, 2, 4);
        Repository repo = new Repository();
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                exitWithError("Incorrect operands.");
            }
            repo.getCommitArea().checkoutFile(args[2], repo.getBlobs());
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                exitWithError("Incorrect operands.");
            }
            repo.getCommitArea().checkoutFile(args[3], args[1], repo.getBlobs());
        } else {
            repo.getCommitArea().checkoutBranch(args[1], repo.getBlobs(), repo.getStageArea());
        }
        repo.getCommitArea().toFile();
        repo.getStageArea().toFile();
    }

    /**
     * Creates a new branch with the given name and points it
     * at the current head commit node.
     * @param args  'branch [branch name]' command
     */
    public static void branch(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        repo.getCommitArea().addBranch(args[1]);
        repo.getCommitArea().toFile();
    }

    /**
     * Deletes the branch name and pointer.
     * @param args  'rm-branch [branch name]' command
     */
    public static void removeBranch(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        repo.getCommitArea().removeBranch(args[1]);
        repo.getCommitArea().toFile();
    }

    /**
     * Checks out all the files in the given commitID.
     * @param args  'reset [commit id]' command
     */
    public static void reset(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        repo.getCommitArea().reset(args[1], repo.getBlobs(), repo.getStageArea());
        repo.getCommitArea().toFile();
        repo.getStageArea().toFile();
    }

    /**
     * Merges files from the given branch to the current branch.
     * @param args  'merge [branch name]' command
     */
    public static void merge(String[] args) {
        validateNumArgs(args, 2, 2);
        Repository repo = new Repository();
        repo.getCommitArea().merge(args[1], repo.getBlobs(), repo.getStageArea());
        repo.getCommitArea().toFile();
        repo.getStageArea().toFile();
    }

    /**
     * Helper function for the Add command to recover a file from
     * the deletion staging area.
     * @param repo  Gitlet repository
     * @param fileName Name of file
     */
    public static void recoverStageDelete(Repository repo, String fileName) {
        HashMap<String, FileItem> headBlobs = repo.getCommitArea().getHead().getBlobsMap();
        String fileBlobID = headBlobs.get(fileName).getBlobID();
        byte[] fileContents = repo.getBlobs().readBlob(fileBlobID);
        repo.getStageArea().unstageDelete(fileName);
        File writeFile = Utils.join(Repository.CWD, fileName);
        Utils.writeContents(writeFile, fileContents);
        repo.getStageArea().toFile();
    }

    /**
     * Prints out error message and exits with error code 0.
     * @param message message to print
     */
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    /**
     * Validates the user input arguments are correct for each command.
     * @param args  User input
     * @param min   Minimum valid arguments
     * @param max   Maximum valid arguments
     */
    public static void validateNumArgs(String[] args, int min, int max) {
        if (args.length < min && args.length > max) {
            exitWithError("Incorrect operands.");
        }
    }
}

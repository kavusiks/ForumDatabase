package piazzaForum;

import java.sql.SQLOutput;
import java.util.*;

/**
 * Class for console based user interactions.
 */
public class ConsoleUI {

    private String loggedInEmail;
    private final UserAuthCtrl authCtrl;
    private final PostCtrl postCtrl;
    private final StatsCtrl statsCtrl;

    private Scanner inputScanner;

    private final String ACTIVE_COURSE_CODE = "TDT4145";
    // Predefined tags for a StartingPost
    private final List<String> ALL_TAGS = Arrays.asList("questions", "announcements", "homework", "homework solutions", "lectures notes", "general announcements");

    /**
     * Connects all controllers to the database and sets up console input scanner.
     */
    public ConsoleUI() {
        authCtrl = new UserAuthCtrl();
        postCtrl = new PostCtrl();
        statsCtrl = new StatsCtrl();

        authCtrl.connect();
        postCtrl.connect();
        statsCtrl.connect();

        resetScanner();
    }

    /**
     * Resets the scanner
     */
    private void resetScanner() {
        inputScanner = new Scanner(System.in);
        inputScanner.useDelimiter(System.lineSeparator());
    }

    /**
     * Terminate all connections and closes the inputScanner.
     */
    private void terminate() {
        System.out.println("Applications terminates...");
        inputScanner.close();
        authCtrl.disconnect();
        postCtrl.disconnect();
        statsCtrl.disconnect();
    }

    /***
     * Takes in email and password from console and tries to login.
     *
     * @return true if user was logged in, false otherwise
     */
    public boolean login() {
        System.out.println("Login to Piazza:");
        System.out.print("Email: ");
        final String email = inputScanner.next();
        System.out.print("Password: ");
        final String password = inputScanner.next();

        if (authCtrl.login(email, password)) {
            System.out.println("Successfully logged in!");
            loggedInEmail = email;
            return true;
        }
        System.out.println("Wrong email or password");
        return false;
    }

    /***
     * Creates a new thread. Post data is taken in from console.
     */
    private void createStartingPost() {
        System.out.println("New Post:");
        System.out.print("Title: ");
        final String title = inputScanner.next();

        System.out.println("Choose folder: ");

        // Gets all the course folders and displays them with ID, name and parent folder name.
        Map<Integer, List<String>> folders = postCtrl.getFolders(ACTIVE_COURSE_CODE);
        for (Map.Entry<Integer, List<String>> folder : folders.entrySet()) {
            int folderId = folder.getKey();
            String folderName = folder.getValue().get(0);
            String parentFolderName = folder.getValue().get(1);
            String parentText = parentFolderName != null ? String.format("(Subfolder of %s)", parentFolderName) : "";
            System.out.printf("(%d) %s %s%n", folderId, folderName, parentText);

        }
        int folderId;
        while (true) {
            System.out.print("Folder nr: ");
            folderId = inputScanner.nextInt();
            if (!folders.keySet().contains(folderId))
                System.out.println("Choose one of the given folders!");
            else
                break;
        }

        // Asks user to choose tags repeatedly until they say no or there are no tags left
        System.out.println("Choose tags:");
        List<String> chosenTags = new ArrayList<>();
        List<String> validTags = new ArrayList<>(ALL_TAGS);
        while(validTags.size() != 0) {
            System.out.println("Do you want to add tag? (y/n)");
            final String tagAnswer = inputScanner.next();
            if (!tagAnswer.equalsIgnoreCase("y"))
                break;
            String tag = getTag(validTags);
            if (tag == null)
                break;
            validTags.remove(tag);
            chosenTags.add(tag);
        }

        System.out.println("Text:");
        final String text = inputScanner.next();

        if (postCtrl.createStartingPost(title, folderId, text, ACTIVE_COURSE_CODE, loggedInEmail, chosenTags))
            System.out.println("New post created!");
        else
            System.out.println("Something went wrong");
    }

    /**
     * Displays the given tags and lets the user choose one of them
     *
     * @param validTags the tags the user can choose from
     *
     * @return the user chosen tag
     */
    private String getTag(List<String> validTags) {
        for (int i = 1; i <= validTags.size(); i++) {
            System.out.printf("(%d) %s%n", i, validTags.get(i - 1));
        }
        try {
            final int tagIndex = inputScanner.nextInt() - 1;
            if (tagIndex >= 0 && tagIndex < validTags.size()) {
                return validTags.get(tagIndex);
            }
        } catch (Exception e) {
            resetScanner();
            return null;
        }
        return null;
    }

    /**
     * Asks the user for which thread they want to answer and for the answer text.
     */
    private void createAnswerOn() {
        System.out.println("Select post:");

        // Displays the postNr, Title and Folder for each thread that can be answered
        for(Map.Entry<Integer, List<String>> entry: postCtrl.getPosts(ACTIVE_COURSE_CODE).entrySet()) {
            System.out.println(entry.getKey()+": "+entry.getValue().get(0)+ " ("+entry.getValue().get(1)+")");
        }
        System.out.print("Your choice: ");
        int answerOnPost = inputScanner.nextInt();
        System.out.println("Text:");
        final String answerText = inputScanner.next();
        if (postCtrl.createAnswerOn(answerOnPost, answerText, ACTIVE_COURSE_CODE, loggedInEmail))
            System.out.println("New answer created!");
        else
            System.out.println("Something went wrong");
    }

    /**
     * Asks the user for what to search for and displays a list of
     * postNr for the matching posts
     */
    private void searchPosts() {
        System.out.print("Search after: ");
        final String keyword = inputScanner.next();
        System.out.println("Search results:");
        System.out.println(postCtrl.searchPosts(ACTIVE_COURSE_CODE, keyword));
    }

    /**
     * Displays the course stats.
     * That is the number of read and created posts by each user.
     */
    private void viewStats() {
        this.statsCtrl.getUserStats(loggedInEmail, ACTIVE_COURSE_CODE);
    }

    /**
     * Displays actions for the user and lets the user choose one of them
     */
    public boolean chooseAction() {
        boolean isInstructor = statsCtrl.verifyInstructor(loggedInEmail);

        System.out.println("Choose action:");
        System.out.println("(1) Create post");
        System.out.println("(2) Search in posts");
        System.out.println("(3) Answer on post");
        if (isInstructor) {
            System.out.println("(4) View stats");
        }
        System.out.println("(0) Quit");
        System.out.print("Your choice: ");
        try {
            final int action = inputScanner.nextInt();
            if (action == 0){
                terminate();
                return false;
            }
            else if (action == 1)
                createStartingPost();
            else if (action == 2)
                searchPosts();
            else if (action == 3)
                createAnswerOn();
            else if (action == 4 && isInstructor)
                viewStats();
            else
                invalidAction();
        } catch (Exception e) {
            resetScanner();
            invalidAction();
        }
        return true;
    }

    /**
     * Displays an invalid choice message to the user
     */
    private void invalidAction() {
        System.out.println("Not a valid choice!");
    }

    public static void main(String[] args) {
        ConsoleUI consoleUI = new ConsoleUI();

        // Asks the user to login repeatedly until they succeed
        while (!consoleUI.login());

        // Asks the user to choose an action repeatedly
        while(consoleUI.chooseAction());

    }
}

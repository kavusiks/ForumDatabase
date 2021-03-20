import java.util.*;

public class ConsoleUI {

    private String loggedInEmail;
    private final String ACTIVE_COURSE_CODE = "TDT4145";
    private final List<String> ALL_TAGS = Arrays.asList("questions", "announcements", "homework", "homework solutions", "lectures notes", "general announcements");
    private UserAuthCtrl authCtrl;
    private PostCtrl postCtrl;
    private StatsCtrl statsCtrl;

    private Scanner inputScanner;

    public ConsoleUI() {
        authCtrl = new UserAuthCtrl();
        postCtrl = new PostCtrl();
        statsCtrl = new StatsCtrl();

        authCtrl.connect();
        postCtrl.connect();
        statsCtrl.connect();

        inputScanner = new Scanner(System.in);
        inputScanner.useDelimiter(System.lineSeparator());
    }


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

    public void createStartingPost() {
        System.out.println("New Post:");
        System.out.print("Title: ");
        final String title = inputScanner.next();

        System.out.println("Choose folder: ");
        Map<String, Integer> folders = postCtrl.getFolders(ACTIVE_COURSE_CODE);
        Map<Integer, String> folderIndexes = new HashMap<>();
        int i = 1;
        for (String folderName : folders.keySet()) {
            folderIndexes.put(i, folderName);
            System.out.println(String.format("(%d) %s", i++, folderName));
        }
        System.out.print("Folder nr: ");
        final int folderIndex = inputScanner.nextInt();
        final int folderId = folders.get(folderIndexes.get(folderIndex));

        System.out.println("Choose tags:");
        List<String> chosenTags = new ArrayList<>();
        List<String> validTags = new ArrayList<>(ALL_TAGS);
        while(validTags.size() != 0) {
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

    private String getTag(List<String> validTags) {
        for (int i = 1; i <= validTags.size(); i++) {
            System.out.println(String.format("(%d) %s", i, validTags.get(i - 1)));
        }
        try {
            final int tagIndex = inputScanner.nextInt() - 1;
            if (tagIndex >= 0 && tagIndex < validTags.size()) {
                return validTags.get(tagIndex);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public void searchPosts() {
        System.out.print("Search after: ");
        final String keyword = inputScanner.next();
        System.out.println("Search results:");
        System.out.println(postCtrl.searchPosts(ACTIVE_COURSE_CODE, keyword));
    }

    public void chooseAction() {
        boolean isInstructor = statsCtrl.verifyInstructor(loggedInEmail);

        System.out.println("Choose action:");
        System.out.println("(1) Create post");
        System.out.println("(2) Search in posts");
        System.out.println("(3) Answer on post");
        if (isInstructor) {
            System.out.println("(4) View stats");
        }
        System.out.print("Your choice: ");
        try {
            final int action = inputScanner.nextInt();
            if (action == 1)
                createStartingPost();
            else if (action == 2)
                searchPosts();
            else if (action == 3) {
                createAnswerOn();
            }
            else if (action == 4 && isInstructor) {
                viewStats();
            }
            else {
                invalidAction();
            }
        } catch (Exception e) {
            invalidAction();
        }
    }

    private void createAnswerOn() {
        System.out.println("Select post:");

        for(Map.Entry<Integer, String> entry: postCtrl.getPosts(ACTIVE_COURSE_CODE).entrySet()) {
            System.out.println(entry.getKey()+": " +entry.getValue());
        }
        System.out.print("Your choice: ");
        int answer = inputScanner.nextInt();
        System.out.println("Text:");
        final String answerText = inputScanner.next();
        if (postCtrl.createAnswerOn(answer, answerText, ACTIVE_COURSE_CODE, loggedInEmail))
            System.out.println("New answer created!");
        else
            System.out.println("Something went wrong");
    }

    public void viewStats() {
        this.statsCtrl.getUserStats(loggedInEmail, ACTIVE_COURSE_CODE);
    }

    private void invalidAction() {
        System.out.println("Not a valid choice!");
    }

    public static void main(String[] args) {
        ConsoleUI consoleUI = new ConsoleUI();

        while (!consoleUI.login());

        while(true) consoleUI.chooseAction();
    }
}

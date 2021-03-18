import java.util.Scanner;

public class UserInterface {

    private String loggedInEmail;
    private final String ACTIVE_COURSE_CODE = "TDT4145";

    private UserAuthCtrl authCtrl;
    private PostCtrl postCtrl;
    private StatsCtrl statsCtrl;

    private Scanner inputScanner;

    public UserInterface() {
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

    public void chooseAction() {
        System.out.println("Velg handling:");
        System.out.println("(1) Søk etter innlegg");
        System.out.print("Ditt valg: ");
        try {
            final int action = inputScanner.nextInt();
            if (action == 1)
                searchPosts();
            else {
                invalidAction();
            }
        } catch (Exception e) {
            invalidAction();
        }
    }

    private void invalidAction() {
        System.out.println("Ikke et gyldig valg!");
    }

    public void searchPosts() {
        System.out.print("Søk etter: ");
        final String keyword = inputScanner.next();
        System.out.println("Søkeresultat:");
        System.out.println(postCtrl.searchPosts(ACTIVE_COURSE_CODE, keyword));
    }

    public static void main(String[] args) {
        UserInterface userInterface = new UserInterface();

        while (!userInterface.login());

        while(true) userInterface.chooseAction();
    }
}

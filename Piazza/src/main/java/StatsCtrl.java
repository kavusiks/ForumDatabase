import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class StatsCtrl extends DBConn{
  String instructor_mail;
  public StatsCtrl() {
    this.connect();
  }

  public boolean verifyInstructor(String email) {
    String userType= "null";
    try {
      Statement statement = conn.createStatement();
      String query = "Select Email, user_Type From User Where Email = '" + email +"'";
      ResultSet result = statement.executeQuery(query);
      while(result.next()) {
        userType = result.getString("user_Type");
      }
      System.out.println(userType);
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("db error during query for verifying user is instructor");
    }
    if (userType=="Instructor") {
      this.instructor_mail =email;
      return true;
    }
    return false;
  }
  public boolean verifyInstructorInCourse (String email, String course) {
    try {
      String query = "Select * From UserInCourse Where Email = (?) And CourseCode = (?)";
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, email);
      statement.setString(2, course);
      ResultSet result = statement.executeQuery();
      return result.next();
    } catch (Exception e) {
      System.out.println("db error during query for verify userInCourse");
    }
    return false;
  }

  // TODO: 18.03.2021 Denne skal returnere stat
/*
  public boolean verifyInstructorInCourser(String email, String Course) {
    String userType= "null";
    try {
      Statement statement = conn.createStatement();
      String query = "Select * From User Where Email = '" + email +"'";
      ResultSet result = statement.executeQuery(query);
      while(result.next()) {
        userType = result.getString("user_Type");
      }
      System.out.println(userType);
    } catch (Exception e) {
      System.out.println(e);
      System.out.println("db error during verifying instructor");
    }
    if (userType=="Instructor") {
      this.instructor_mail =email;
      return true;
    }
    return false;
  }
*/
  public static void main(String[]args){
    StatsCtrl s= new StatsCtrl();
    s.verifyInstructor("PerPaulsen@hotmail.com");
    s.verifyInstructorInCourse("PerPaulsen@hotmail.com", "TDT4145");

    }

  }

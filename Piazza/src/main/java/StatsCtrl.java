import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Controller class used to get stats for a course.
 * Only instructor within a given course can view the stats
 * Mainly used in usecase 5
 */
public class StatsCtrl extends DBConn {

  /**
   * Method used to verify that an user is og user_Type "Instructor".
   * @param email the user's Email(pk)
   * @return true if user is of type "Instructor"
   */
  private boolean verifyInstructor(String email) {
    String userType = "null";
    try {
      String query = "Select Email, user_Type From User Where Email = (?)";
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, email);
      ResultSet result = statement.executeQuery();
      while (result.next()) {
        userType = result.getString("user_Type");
      }
      //System.out.println("userType: " +userType);
      if(userType.equals("Instructor")) return true;
    } catch (Exception e) {
      System.out.println("db error during query for verifying user is instructor");
      return false;
    }
    System.out.println("User is not an instructor or is not registered in the given course");
    return false;
  }
  
  /**
   * Method used to verify that an user is in a given course".
   * @param email the user to check
   * @param course the course to check in
   * @return true if user is in the given course
   */
  private boolean verifyUserInCourse(String email, String course) {
    String query_result_user = "null";
    String query_result_course = "null";

    try {
      String query = "Select * From UserInCourse Where Email = (?) And CourseCode = (?)";
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, email);
      statement.setString(2, course);
      ResultSet result = statement.executeQuery();
      while (result.next()) {
        query_result_user = result.getString("Email");
        query_result_course = result.getString("CourseCode");
      }
      if (query_result_user.equals(email) && query_result_course.equals(course)) {
        //System.out.println("User in course verified");
        return true;
      }
    } catch (Exception e) {
      System.out.println("db error during query for verify userInCourse");
    }
    System.out.println("User is in not in the given course");
    return false;
  }

  /**
   * Method used to print user stats for a given course.
   * @param course the given course
   */
  private void printStats(String course) {
    try {
      String query =
          "Select * from ( Select uc1.Email, count(vp1.PostNr) as AntallReadPost from UserInCourse as uc1 left outer join ( ViewedPost as vp1 inner join  StartingPost as sp1 on vp1.PostNr = sp1.PostNr) on uc1.Email = vp1.Email where uc1.CourseCode = (?) group by uc1.Email) as ReadPost natural inner join ( select uc2.Email, count(p2.PostNr) as AntallCreatedPost from UserInCourse as uc2 left outer join Post as p2 on uc2.CourseCode=p2.CourseCode and uc2.Email = p2.Email where uc2.CourseCode = (?) group by uc2.Email) as CreatedPost order by ReadPost.AntallReadPost desc";
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, course);
      statement.setString(2, course);
      ResultSet result = statement.executeQuery();
      String tableAlignmentFormat = "| %-30s | %-4s | %-7s |%n";
      System.out.format("+--------------------------------+------+---------|%n");
      System.out.format("| Email                          | Read | Created |%n");
      System.out.format("+--------------------------------+------+---------|%n");
      while (result.next()) {
        System.out.format(tableAlignmentFormat, result.getString("Email"), result.getString("AntallReadPost"),
            result.getString("AntallCreatedPost"));
      }
      System.out.format("+--------------------------------+------+---------|%n");
    } catch (Exception e) {
      System.out.println("db error during query for printing stats");
    }
  }

  /**
   * Method used to print user stats for given course
   * after verifying that user calling this method is an instructor and that the instructor belongs to this course.
   * @param instructorEmail the user calling this method
   * @param courseCode the course to view stats for
   */
  public void getUserStats(String instructorEmail, String courseCode) {
    boolean verified = verifyInstructor(instructorEmail) && verifyUserInCourse(instructorEmail, courseCode);
    if (verified)
      printStats(courseCode);
  }
}

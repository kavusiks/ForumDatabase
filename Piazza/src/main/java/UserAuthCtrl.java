
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserAuthCtrl extends DBConn {

  public boolean login(String email, String password) {
    try {
      String query = "Select * From User Where Email = (?) And user_Password = (?)";
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, email);
      statement.setString(2, password);
      ResultSet result = statement.executeQuery();
      return result.next();
    } catch (Exception e) {
      System.out.println("db error during login query");
    }
    return false;
  }

}

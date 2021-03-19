import java.awt.geom.NoninvertibleTransformException;
import java.util.*;
import java.sql.*;
import java.util.Date;

public class PostCtrl extends DBConn {

  private UserAuthCtrl userAuthCtrl = new UserAuthCtrl();
  private PreparedStatement statementGetPrimaryKey;
  private PreparedStatement statementPost;
  private PreparedStatement statementStartingPost;
  private PreparedStatement statementFollowUp;
  private PreparedStatement statementReplyPost;
  private PreparedStatement statementTag;
  private List<Integer> primaryKyes;

  Calendar calendar = Calendar.getInstance();
  Date post_Date =  new java.sql.Date(calendar.getTime().getTime());
  Time post_Time = new java.sql.Time(calendar.getTime().getTime());


  /*private List<Integer> getPrimaryKeys(String SQL, String PKName) {
    primaryKyes = new ArrayList<>();
    statementGetPrimaryKey = insert(SQL);
    try {
      ResultSet resultSet = statementGetPrimaryKey.executeQuery();
      while (resultSet.next()) {
        primaryKyes.add(resultSet.getInt(PKName));

       return primaryKyes;
      }

    } catch (Exception e) {
      System.out.println(e);
      return null;
    }
  }*/

  private int generatePrimaryKey(String SQL) {
    this.statementGetPrimaryKey = insert(SQL);
    ResultSet resultSet = null;
    try {
      resultSet = statementGetPrimaryKey.executeQuery();
      if (resultSet.next()) {
        return resultSet.getInt(1)+1;
      }
      return 1;
    } catch (SQLException throwables) {
      throwables.printStackTrace();
      return 1;
    }
  }


  public PreparedStatement insert(String SQL) {
    try {
      return conn.prepareStatement(SQL);
    } catch ( Exception e) {
      System.out.println("error during preparing statement");
      return null;
    }
  }

  private void post(String post_Text, String CourseCode, String Email, String TypePost) {
    try {
      this.statementPost = insert("INSERT INTO Post VALUES ((?),(?),(?),(?),(?),(?),(?))");
      this.statementPost.setInt(1, generatePrimaryKey("Select max(PostNr) From Post"));
      this.statementPost.setString(2, post_Text);
      this.statementPost.setDate(3, (java.sql.Date) post_Date);
      this.statementPost.setTime(4, post_Time);
      this.statementPost.setString(5, CourseCode);
      this.statementPost.setString(6, Email);
      this.statementPost.setString(7, TypePost);
      try {
        statementPost.execute();
      } catch (java.sql.SQLException e) {
        System.out.println(e);
      }
    } catch (Exception e) {

      System.out.println(e);
    }
  }


  public void startingPost( String Title, int FolderID, String post_Text, String CourseCode,
      String Email, String TypePost) {

    this.post(post_Text, CourseCode, Email, "StartingPost");
    this.statementStartingPost = insert("INSERT INTO StartingPost VALUES ((?),(?),(?))");
    try {
      this.statementStartingPost.setInt(1, generatePrimaryKey("Select max(PostNr) From Post"));
      this.statementStartingPost.setString(2, Title);
      this.statementStartingPost.setInt(3, FolderID);
      this.statementStartingPost.execute();

    } catch (Exception e) {
      System.out.println(e);
    }
  }

  public void FollowUp( boolean resolved, int FollowUpOn, String post_Text, String CourseCode,
      String Email, String TypePost) {

      this.post(post_Text, CourseCode, Email, "FollowUp");
      this.statementFollowUp = insert("INSERT INTO FollowUp VALUES ((?),(?),(?))");

      try{
        this.statementFollowUp.setInt(1, generatePrimaryKey("Select max(PostNr) From Post"));
        this.statementFollowUp.setBoolean(2, resolved);
        this.statementFollowUp.setInt(3, FollowUpOn);
        this.statementFollowUp.execute();
      } catch (Exception e) {
        System.out.println(e);
      }
    }


  public void ReplyPost(int PostNr , int CommentOn, int AnswerOn, String TypeReply, String post_Text, String CourseCode,
      String Email) {

    this.post(post_Text, CourseCode, Email, "FollowUp");
    this.statementReplyPost = insert("INSERT INTO ReplyPost VALUES ((?),(?),(?),(?))");

    try{
      this.statementReplyPost.setInt(1, generatePrimaryKey("Select max(PostNr) From Post"));
      this.statementReplyPost.setInt(2, CommentOn);
      this.statementReplyPost.setInt(3, AnswerOn);
      this.statementReplyPost.setString(4, TypeReply);
      this.statementReplyPost.execute();
    } catch (Exception e) {
      System.out.println(e);
    }
  }


  public void TaggedStartingPost( List<String> Tag) {

    this.statementTag = insert("INSERT INTO ReplyPost VALUES ((?),(?))");
    try{
      for (int i = 0; i < Tag.size(); i++) {
        this.statementTag.setInt(1, generatePrimaryKey("Select max(PostNr) From Post"));
        this.statementTag.setString(2, Tag.get(0));
        this.statementTag.execute();
      }

    } catch (Exception e) {
      System.out.println(e);
    }
  }




  /***
   * Searches for the keyword in posts published in the given course.
   * @param courseCode of the course.
   * @param keyword that is to be searched for.
   * @return a list with postNr of matched posts.
   */
  public List<Integer> searchPosts(String courseCode, String keyword) {
    List<Integer> result = new ArrayList<>();
    final String query =
        "Select PostNr " +
            "From Post Natural Left Outer Join StartingPost " +
            "Where CourseCode = (?) And (post_Text Like (?) Or Title Like (?))";
    try {
      PreparedStatement statement = conn.prepareStatement(query);
      final String wKeyword = "%" + keyword + "%";
      statement.setString(1, courseCode);
      statement.setString(2, wKeyword);
      statement.setString(3, wKeyword);

      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        final int postNr = rs.getInt("PostNr");
        result.add(postNr);
      }

    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.err.println("db error during posts search query");
    }

    return result;
  }

  public Map<String, Integer> getFolders(String courseCode) {
    Map<String, Integer> folders = new HashMap<>();
    final String query = "Select FolderID, folder_Name " +
            "From Folder Where CourseCode = (?)";
    try {
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, courseCode);

      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        final int folderId = rs.getInt("FolderID");
        final String folderName = rs.getString("folder_Name");

        folders.put(folderName, folderId);
      }

    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.err.println("db error when retrieving folders");
    }

    return folders;
  }


  public static void main(String[] args) {
    Calendar calendar = Calendar.getInstance();
    Date post_Date = new java.sql.Date(calendar.getTime().getTime());
    Time post_Time = new java.sql.Time(calendar.getTime().getTime());
    PostCtrl postCtrl = new PostCtrl();
    postCtrl.connect();
    postCtrl.post("Dette er en post med auto pk", "TDT4145", "olaNordmann@gmail.com", "StartingPost");
  }


}

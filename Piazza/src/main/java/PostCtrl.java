import java.util.*;
import java.sql.*;
import java.util.Date;

public class PostCtrl extends DBConn {

  private PreparedStatement statementGetPrimaryKey;
  private PreparedStatement statementPost;
  private PreparedStatement statementStartingPost;
  private PreparedStatement statementFollowUp;
  private PreparedStatement statementReplyPost;
  private PreparedStatement statementTag;

  Calendar calendar = Calendar.getInstance();
  Date post_Date =  new java.sql.Date(calendar.getTime().getTime());
  Time post_Time = new java.sql.Time(calendar.getTime().getTime());

  private int generatePrimaryKey(String SQL) throws SQLException {
    this.statementGetPrimaryKey = insert(SQL);
    ResultSet resultSet = statementGetPrimaryKey.executeQuery();
    if (resultSet.next()) {
      return resultSet.getInt(1)+1;
    }
      return 1;
  }

  public PreparedStatement insert(String SQL) {
    try {
      return conn.prepareStatement(SQL);
    } catch ( Exception e) {
      System.out.println("error during preparing statement");
      return null;
    }
  }

  private int createPost(String post_Text, String CourseCode, String Email, String TypePost) throws SQLException{
      int key = generatePrimaryKey("Select max(PostNr) From Post");
      this.statementPost = insert("INSERT INTO Post VALUES ((?),(?),(?),(?),(?),(?),(?))");
      this.statementPost.setInt(1, key);
      this.statementPost.setString(2, post_Text);
      this.statementPost.setDate(3, (java.sql.Date) post_Date);
      this.statementPost.setTime(4, post_Time);
      this.statementPost.setString(5, CourseCode);
      this.statementPost.setString(6, Email);
      this.statementPost.setString(7, TypePost);
        statementPost.execute();
      return key;
  }

  public boolean createStartingPost(String title, int folderId, String text, String courseCode,
      String email, List<String> tags) {
    this.statementStartingPost = insert("INSERT INTO StartingPost VALUES ((?),(?),(?))");
    try {
      int key = this.createPost(text, courseCode, email, "StartingPost");
      this.statementStartingPost.setInt(1, key);
      this.statementStartingPost.setString(2, title);
      this.statementStartingPost.setInt(3, folderId);
      this.statementStartingPost.execute();
      this.createTaggedStartingPost(key, tags);
      return true;
    } catch (Exception e) {
      System.out.println(e);
      return false;
    }
  }

  public boolean createFollowUp( boolean resolved, int followUpOn, String text, String courseCode,
      String email) {
      try{
        int key = this.createPost(text, courseCode, email, "FollowUp");
        this.statementFollowUp = insert("INSERT INTO FollowUp VALUES ((?),(?),(?))");
        this.statementFollowUp.setInt(1, key);
        this.statementFollowUp.setBoolean(2, resolved);
        this.statementFollowUp.setInt(3, followUpOn);
        this.statementFollowUp.execute();
        return true;
      } catch (Exception e) {
        System.out.println(e);
        return false;
      }
    }

  private boolean createReplyPost(Integer commentOn, Integer answerOn, String typeReply, String text, String courseCode,
      String email) {
    try{
      int key = this.createPost(text, courseCode, email, "FollowUp");
      this.statementReplyPost = insert("INSERT INTO ReplyPost VALUES ((?),(?),(?),(?))");
      this.statementReplyPost.setInt(1, key);
      this.statementReplyPost.setInt(2, commentOn);
      this.statementReplyPost.setInt(3, answerOn);
      this.statementReplyPost.setString(4, typeReply);
      this.statementReplyPost.execute();
      return true;
    } catch (Exception e) {
      System.out.println(e);
      return false;
    }
  }

  public boolean createAnswerOn(int answerOn, String post_Text, String courseCode, String Email) {
    return createReplyPost(null, answerOn, "Answer", courseCode, post_Text, Email);
  }

  public boolean createCommentOn(int commentOn, String post_Text, String courseCode, String Email) {
    return createReplyPost(null, commentOn, "Comment", courseCode, post_Text, Email);
  }

  private void createTaggedStartingPost(int postNr, List<String> tags) throws SQLException{

    this.statementTag = insert("INSERT INTO TaggedStartingPost VALUES ((?),(?))");
    for (String tag : tags) {
      this.statementTag.setInt(1, postNr);
      this.statementTag.setString(2, tag);
      this.statementTag.execute();
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
}

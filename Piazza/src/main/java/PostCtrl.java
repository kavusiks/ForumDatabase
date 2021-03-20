import java.rmi.MarshalledObject;
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

  private Calendar calendar = Calendar.getInstance();

  //Bare en liten ting, men dette blir feil siden man kjører programmet over lengre tid,
  //burde kanskje heller ha to hjelpemetoder for å få tid og dato
  private Date post_Date =  new java.sql.Date(calendar.getTime().getTime());
  private Time post_Time = new java.sql.Time(calendar.getTime().getTime());

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

  private int createPost(String post_Text, String courseCode, String email, String typePost) throws SQLException{
      int key = generatePrimaryKey("Select max(PostNr) From Post");
      this.statementPost = insert("INSERT INTO Post VALUES ((?),(?),(?),(?),(?),(?),(?))");
      this.statementPost.setInt(1, key);
      this.statementPost.setString(2, post_Text);
      this.statementPost.setDate(3, (java.sql.Date) post_Date);
      this.statementPost.setTime(4, post_Time);
      this.statementPost.setString(5, courseCode);
      this.statementPost.setString(6, email);
      this.statementPost.setString(7, typePost);
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
      System.out.println("db error during create starting post query");
      System.out.println(e.getMessage());
      return false;
    }
  }

  public boolean createFollowUp(boolean resolved, int followUpOn, String text, String courseCode,
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
        System.err.println("db error during create follow up query");
        System.err.println(e.getMessage());
        return false;
      }
    }

  private boolean createReplyPost(Integer commentOn, Integer answerOn, String typeReply, String text, String courseCode,
      String email) {
    try{
      int key = this.createPost(text, courseCode, email, "ReplyPost");
      this.statementReplyPost = insert("INSERT INTO ReplyPost VALUES ((?),(?),(?),(?))");
      this.statementReplyPost.setInt(1, key);

      if (commentOn == null)
        this.statementReplyPost.setNull(2, commentOn);
      else
        this.statementReplyPost.setInt(2, java.sql.Types.NULL);

      if (answerOn == null)
        this.statementReplyPost.setNull(3, java.sql.Types.NULL);
      else
        this.statementReplyPost.setInt(3, answerOn);

      this.statementReplyPost.setString(4, typeReply);
      this.statementReplyPost.execute();
      return true;
    } catch (Exception e) {
      System.err.println("db error during create reply query");
      System.err.println(e.getMessage());
      return false;
    }
  }

  public boolean createAnswerOn(int answerOn, String post_Text, String courseCode, String email) {
    String userType = this.getUserType(email);
    String query = "Select user_Type, PostNr from User natural inner join Post natural inner join ReplyPost where CourseCode= (?) and AnswerOn = (?)";
    try {
      PreparedStatement answerStatement = conn.prepareStatement(query);
      System.out.println(courseCode);
      answerStatement.setString(1,courseCode);
      answerStatement.setInt(2,answerOn);
      ResultSet resultSet = answerStatement.executeQuery();
      if (resultSet.next()) {

        if (resultSet.getString("user_Type").equals(userType)) {
          int postNr = resultSet.getInt("PostNr");
          System.out.println(postNr);
          String updateQuery = "Update Post set post_Text = (?), post_Date = (?), post_Time = (?), Email = (?) where PostNr =(?)";
          PreparedStatement updatePostReply = conn.prepareStatement(updateQuery);
          updatePostReply.setString(1, post_Text);
          updatePostReply.setDate(2, (java.sql.Date) this.post_Date);
          updatePostReply.setTime(3, this.post_Time);
          updatePostReply.setString(4, email);
          updatePostReply.setInt(5, postNr);
          updatePostReply.executeUpdate();
          return true;
        }
      }
    } catch (Exception e) {
      System.err.println("db error during create answer query");
      System.err.println(e.getMessage());
      return false;
    }
    return createReplyPost(null, answerOn, "Answer", post_Text, courseCode, email);
  }

  public boolean createCommentOn(int commentOn, String post_Text, String courseCode, String Email) {
    return createReplyPost(commentOn, null, "Comment", post_Text,courseCode, Email);
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
      System.err.println("db error when retrieving folders");
      System.err.println(e.getMessage());
    }

    return folders;
  }


  public Map<Integer, String> getPosts(String CourseCode) {
    Map<Integer, String> posts = new HashMap<>();
    final String query = "Select PostNr, Title from StartingPost natural inner join Post where CourseCode = (?)";
    try {
      PreparedStatement selectPost = conn.prepareStatement(query);
      selectPost.setString(1, CourseCode);
      ResultSet resultSet = selectPost.executeQuery();
      while (resultSet.next()) {
        posts.put(resultSet.getInt("PostNr"), resultSet.getString("Title"));
      }

    } catch (Exception e) {
      System.err.println("db error during query for getting posts");
      System.err.println(e.getMessage());
    }

    return posts;
  }

  public String getUserType(String email) {
    String userType = null;
    try {
      String query = "Select user_Type From User Where Email = (?)";
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, email);
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        userType = result.getString("user_Type");
      }
    } catch (Exception e) {
      System.err.println("db error during query for getting instructor");
      System.err.println(e.getMessage());
    }
    return userType;
  }
}

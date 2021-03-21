import java.sql.Date;
import java.util.*;
import java.sql.*;

public class PostCtrl extends DBConn {

  //Tror ikke det er noe poeng i å lagre disse med mindre vi lager en egen metode
  //for insert og deler opp, nå kunne vi like gjerne bare deklarert de inne i metodene
  private PreparedStatement statementGetPrimaryKey;
  private PreparedStatement statementPost;
  private PreparedStatement statementStartingPost;
  private PreparedStatement statementFollowUp;
  private PreparedStatement statementReplyPost;
  private PreparedStatement statementTag;

  private final static String STARTING_POST = "StartingPost";
  private final static String REPLY_POST = "ReplyPost";
  private final static String FOLLOW_UP = "FollowUp";
  private final static String ANSWER = "Answer";
  private final static String COMMENT = "Comment";

  /**
   * generates a unique primary key for a Post
   * @return the ineteger for the primary key
   * @throws SQLException
   */
  private int generatePrimaryKey() throws SQLException {
    this.statementGetPrimaryKey = insert("Select max(postNr) From Post");
    ResultSet resultSet = statementGetPrimaryKey.executeQuery();
    if (resultSet.next()) {
      return resultSet.getInt(1) + 1;
    }
      return 1;
  }

  /**
   *
   * @param SQL
   * @return prepares a statment
   * @throws SQLException
   */
  public PreparedStatement insert(String SQL) throws SQLException{
      return conn.prepareStatement(SQL);
  }

  /**
   *
   * @param post_Text the text a user writes in a post
   * @param courseCode the coursecode the user wants to write a post for
   * @param email Unique Email address of the user
   * @param typePost the type of post, needs to be either StartingPost or ReplyPost or FollowUp
   * @return
   * @throws SQLException
   */
  private int createPost(String post_Text, String courseCode, String email, String typePost) throws SQLException{
      int key = generatePrimaryKey();
      this.statementPost = insert("INSERT INTO Post VALUES ((?),(?),(?),(?),(?),(?),(?))");
      this.statementPost.setInt(1, key);
      this.statementPost.setString(2, post_Text);
      this.statementPost.setDate(3, getDate());
      this.statementPost.setTime(4, getTime());
      this.statementPost.setString(5, courseCode);
      this.statementPost.setString(6, email);
      this.statementPost.setString(7, typePost);
        statementPost.execute();
      return key;
  }

  /**
   *
   * @return the current date
   */
  private Date getDate() {
    return new Date(Calendar.getInstance().getTime().getTime());
  }

  /**
   *
   * @return the current time
   */
  private Time getTime() {
    return new Time(Calendar.getInstance().getTime().getTime());
  }

  /**
   *
   * @param title Title of the post
   * @param folderId ID if the folder
   * @param text Text user wants to write in the STartingPost
   * @param courseCode The coursecode the user wants to write a post for
   * @param email Unique Email address of the user
   * @param tags Predefined tags the user can choose to use in his/her post
   * @return a boolean of whether the post is created or not
   */
  public boolean createStartingPost(String title, int folderId, String text, String courseCode,
      String email, List<String> tags) {
    try {
      this.statementStartingPost = insert("INSERT INTO StartingPost VALUES ((?),(?),(?))");
      int key = this.createPost(text, courseCode, email, STARTING_POST);
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

  /**
   *
   * @param resolved boolean for if the FollowUp is resolved or not
   * @param followUpOn Which post the user wants to write a FollowUp to
   * @param text The text is the FollowUp
   * @param courseCode The coursecode the user wants to write a post for
   * @param email Unique Email address of the user
   * @return A boolean for if the FollowUp is created or not
   */
  public boolean createFollowUp(boolean resolved, int followUpOn, String text, String courseCode,
      String email) {
      try{
        int key = this.createPost(text, courseCode, email, FOLLOW_UP);
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


  /**
   *
   * @param commentOn ID of which post the user wants to comment on
   * @param answerOn ID of which post the user wants to answer on
   * @param typeReply type of reply, needs to be comment or answer
   * @param text The text the user writes in the post
   * @param courseCode The coursecode the user wants to write a post for
   * @param email Unique Email address of the user
   * @return A boolean for if the ReplyPost was created or not
   */

  private boolean createReplyPost(Integer commentOn, Integer answerOn, String typeReply, String text, String courseCode,
      String email) {
    try{
      int key = this.createPost(text, courseCode, email, REPLY_POST);
      this.statementReplyPost = insert("INSERT INTO ReplyPost VALUES ((?),(?),(?),(?))");
      this.statementReplyPost.setInt(1, key);

      if (commentOn == null)
        this.statementReplyPost.setNull(2, java.sql.Types.NULL);
      else
        this.statementReplyPost.setInt(2, commentOn);

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

  /**
   *
   * @param answerOn The ID of which Post the user wnats to answer
   * @param post_Text The text the user writes in the answer
   * @param courseCode The coursecode the user wants to write an answer for
   * @param email Unique Email address of the user
   * @return A boolean for if the answer was created or not
   */

  public boolean createAnswerOn(int answerOn, String post_Text, String courseCode, String email) {
    String userType = this.getUserType(email);
    String query = "Select user_Type, PostNr from User natural inner join Post natural inner join ReplyPost where CourseCode= (?) and AnswerOn = (?)";
    try {
      PreparedStatement answerStatement = conn.prepareStatement(query);
      answerStatement.setString(1, courseCode);
      answerStatement.setInt(2, answerOn);
      ResultSet resultSet = answerStatement.executeQuery();

      boolean update = false;
      int updatePostNr = 0;
      while (resultSet.next()) {
        if (resultSet.getString("user_Type").equals(userType)) {
          update = true;
          updatePostNr = resultSet.getInt("PostNr");
        }
      }
      if (update) {
        String updateQuery = "Update Post set post_Text = (?), post_Date = (?), post_Time = (?), Email = (?) where PostNr =(?)";
        PreparedStatement updatePostReply = conn.prepareStatement(updateQuery);
        updatePostReply.setString(1, post_Text);
        updatePostReply.setDate(2, getDate());
        updatePostReply.setTime(3, getTime());
        updatePostReply.setString(4, email);
        updatePostReply.setInt(5, updatePostNr);
        updatePostReply.executeUpdate();
        return true;
      }
    } catch (Exception e) {
      System.err.println("db error during create answer query");
      System.err.println(e.getMessage());
      return false;
    }
    return createReplyPost(null, answerOn, ANSWER, post_Text, courseCode, email);
  }

  /**
   *
   * @param commentOn The ID of which Post the user wnats to comment on
   * @param post_Text The text the user writes in the comment
   * @param courseCode The coursecode the user wants to write a comment for
   * @param Email Unique Email address of the user
   * @return A boolean for if the comment was created or not
   */
  public boolean createCommentOn(int commentOn, String post_Text, String courseCode, String Email) {
    return createReplyPost(commentOn, null, COMMENT, post_Text,courseCode, Email);
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

  /**
   *
   * @param courseCode of the course.
   * @return HashMap of the folders with the folders name and ID
   */
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


  /**
   *
   * @param CourseCode of the course
   * @return A HashMap of the created posts with the postst ineteger and a list of the postst title and correspodning Folder
   */
  public Map<Integer, List<String>> getPosts(String CourseCode) {
    Map<Integer, List<String>> posts = new HashMap<>();
    final String query = "Select PostNr, Title, folder_Name From StartingPost natural inner join Post natural inner join Folder where CourseCode = (?)";
    try {
      PreparedStatement selectPost = conn.prepareStatement(query);
      selectPost.setString(1, CourseCode);
      ResultSet resultSet = selectPost.executeQuery();
      while (resultSet.next()) {
        List<String> tmpList = new ArrayList<>();
        tmpList.add(resultSet.getString("Title"));
        tmpList.add(resultSet.getString("folder_Name"));
        posts.put(resultSet.getInt("PostNr"), tmpList);
      }

    } catch (Exception e) {
      System.err.println("db error during query for getting posts");
      System.err.println(e.getMessage());
    }

    return posts;
  }

  /**
   *
   * @param email
   * @return
   */

  /**
   *
   * @param email of the user
   * @return Returns the usertype (Student or Instructor)
   */
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

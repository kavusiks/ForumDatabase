package piazzaForum;

import piazzaForum.DBConn;

import java.sql.Date;
import java.util.*;
import java.sql.*;

/**
 * Controller class used to maintain posts, answers, followUps and comments within a course.
 * Mainly used in use case 2,3 and 4
 */
public class PostCtrl extends DBConn {

  // All possible post types
  private final static String STARTING_POST = "StartingPost";
  private final static String REPLY_POST = "ReplyPost";
  private final static String FOLLOW_UP = "FollowUp";
  private final static String ANSWER = "Answer";
  private final static String COMMENT = "Comment";

  /**
   * Generates a unique primary key for a Post. Gets the max primary key using sql query, and then adds 1.
   * This ensures that the method always returns a unique primary key
   *
   * @return the generated key
   *
   * @throws SQLException if generation failed
   */
  private int generatePrimaryKey() throws SQLException {
    String query = "Select max(postNr) From Post";
    PreparedStatement statement = conn.prepareStatement(query);
    ResultSet resultSet = statement.executeQuery();
    if (resultSet.next()) {
      return resultSet.getInt(1) + 1;
    }
      return 1;
  }


  /**
   * @param post_Text the text a user writes in a post
   * @param courseCode for the course the user wants to write a post for
   * @param email Unique Email address of the user
   * @param typePost the type of post, needs to be either StartingPost or ReplyPost or FollowUp
   *
   * @return the postNr of the created post
   *
   * @throws SQLException if creation failed
   */
  private int createPost(String post_Text, String courseCode, String email, String typePost) throws SQLException{
      final String query = "INSERT INTO Post VALUES ((?),(?),(?),(?),(?),(?),(?))";
      PreparedStatement statement = conn.prepareStatement(query);
      int key = generatePrimaryKey();

      statement.setInt(1, key);
      statement.setString(2, post_Text);
      statement.setDate(3, getDate());
      statement.setTime(4, getTime());
      statement.setString(5, courseCode);
      statement.setString(6, email);
      statement.setString(7, typePost);
      statement.execute();
      return key;
  }

  /**
   * @return the current date
   */
  private Date getDate() {
    return new Date(Calendar.getInstance().getTime().getTime());
  }

  /**
   * @return the current time
   */
  private Time getTime() {
    return new Time(Calendar.getInstance().getTime().getTime());
  }

  /**
   * @param title Title of the post
   * @param folderId ID if the folder
   * @param text Text user wants to write in the STartingPost
   * @param courseCode for the course the user wants to write a post for
   * @param email Unique Email address of the user
   * @param tags User chosen tags
   *
   * @return a boolean of whether the post is created or not
   */
  public boolean createStartingPost(String title, int folderId, String text, String courseCode,
      String email, List<String> tags) {
    try {
      String query = "INSERT INTO StartingPost VALUES ((?),(?),(?))";
      PreparedStatement statement = conn.prepareStatement(query);
      int key = this.createPost(text, courseCode, email, STARTING_POST);

      statement.setInt(1, key);
      statement.setString(2, title);
      statement.setInt(3, folderId);
      statement.execute();

      this.createTaggedStartingPost(key, tags);
      return true;
    } catch (Exception e) {
      System.out.println("db error during create starting post query");
      System.out.println(e.getMessage());
      return false;
    }
  }

  /**
   * @param resolved boolean for if the FollowUp is resolved or not
   * @param followUpOn Which post the user wants to write a FollowUp to
   * @param text The text is the FollowUp
   * @param courseCode of the course the user wants to write a post for
   * @param email Unique Email address of the user
   *
   * @return A boolean for if the FollowUp is created or not
   */
  public boolean createFollowUp(boolean resolved, int followUpOn, String text, String courseCode,
      String email) {
      try{
        String query = "INSERT INTO FollowUp VALUES ((?),(?),(?))";
        PreparedStatement statement = conn.prepareStatement(query);
        int key = this.createPost(text, courseCode, email, FOLLOW_UP);

        statement.setInt(1, key);
        statement.setBoolean(2, resolved);
        statement.setInt(3, followUpOn);
        statement.execute();
        return true;
      } catch (Exception e) {
        System.err.println("db error during create follow up query");
        System.err.println(e.getMessage());
        return false;
      }
    }


  /**
   * @param commentOn ID of which post the user wants to comment on
   * @param answerOn ID of which post the user wants to answer on
   * @param typeReply type of reply, needs to be comment or answer
   * @param text The text the user writes in the post
   * @param courseCode of the course the user wants to write a reply in
   * @param email Unique Email address of the user
   *
   * @return A boolean for if the ReplyPost was created or not
   */
  private boolean createReplyPost(Integer commentOn, Integer answerOn, String typeReply, String text, String courseCode,
      String email) {
    try{
      String query = "INSERT INTO ReplyPost VALUES ((?),(?),(?),(?))";
      PreparedStatement statement = conn.prepareStatement(query);
      int key = this.createPost(text, courseCode, email, REPLY_POST);

      statement.setInt(1, key);

      //If the post is of type Answer, then the value for CommentOn in the database is set to java.sql.Types.NULL,

      if (commentOn == null)
        statement.setNull(2, java.sql.Types.NULL);
      else
        statement.setInt(2, commentOn);
      //if the post is of type Comment, then the value for AnswerOn in the database is set to java.sql.Types.NULL

      if (answerOn == null)
        statement.setNull(3, java.sql.Types.NULL);
      else
        statement.setInt(3, answerOn);

      statement.setString(4, typeReply);

      statement.execute();
      return true;
    } catch (Exception e) {
      System.err.println("db error during create reply query");
      System.err.println(e.getMessage());
      return false;
    }
  }

  /**
   * Method used to create answer on StartingPost. Each StartingPost can only have two answers,
   * one from an instructor and one from a student. If an answer already exist from one of these userTypes,
   * it will be updated with the new answer.
   *
   * @param answerOnPost The ID of which Post the user wants to answer
   * @param post_Text The text the user writes in the answer
   * @param courseCode of the course the user wants to write an answer for
   * @param email of the user
   *
   * @return A boolean for if the answer was created or not
   */
  public boolean createAnswerOn(int answerOnPost, String post_Text, String courseCode, String email) {
    String userType = this.getUserType(email);
    String query = "Select user_Type, PostNr from User natural inner join Post natural inner join ReplyPost where CourseCode= (?) and AnswerOn = (?)";
    try {
      PreparedStatement answerStatement = conn.prepareStatement(query);
      answerStatement.setString(1, courseCode);
      answerStatement.setInt(2, answerOnPost);
      ResultSet resultSet = answerStatement.executeQuery();

      // Checks if there already exists an answer for this type of user (Student/Instructor)
      boolean update = false;
      int updatePostNr = 0;

      //Iterates as long as there the next element in the result set exists
      while (resultSet.next()) {
        if (resultSet.getString("user_Type").equals(userType)) {
          update = true;
          updatePostNr = resultSet.getInt("PostNr");
        }
      }
      // If an answer already exists, the old answer is updated
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
    // If a previous answer does not exist, a new answer is created.
    return createReplyPost(null, answerOnPost, ANSWER, post_Text, courseCode, email);
  }

  /**
   * @param commentOn The ID of which Post the user wants to comment on
   * @param post_Text The text the user writes in the comment
   * @param courseCode of the course the user wants to write a comment for
   * @param Email Unique Email address of the user
   *
   * @return A boolean for if the comment was created or not
   */
  public boolean createCommentOn(int commentOn, String post_Text, String courseCode, String Email) {
    return createReplyPost(commentOn, null, COMMENT, post_Text,courseCode, Email);
  }

  /**
   * Adds all of the given tags to the post with the given postNr.
   */
  private void createTaggedStartingPost(int postNr, List<String> tags) throws SQLException {
    String query = "INSERT INTO TaggedStartingPost VALUES ((?),(?))";
    PreparedStatement statement = conn.prepareStatement(query);
    for (String tag : tags) {
      statement.setInt(1, postNr);
      statement.setString(2, tag);
      statement.execute();
    }
  }

  /***
   * Searches for the keyword in posts published in the given course.
   *
   * @param courseCode of the course.
   * @param keyword that is to be searched for.
   *
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

      ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        final int postNr = resultSet.getInt("PostNr");
        result.add(postNr);
      }

    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.err.println("db error during posts search query");
    }

    return result;
  }

  /**
   * @param courseCode of the course.
   *
   * @return Map of the folders with the folders ID as key,
   * and a list with list[0]: folder name and list[1]: parent folder name
   **/
  public Map<Integer, List<String>> getFolders(String courseCode) {
    Map<Integer, List<String>> folders = new HashMap<>();
    final String query = "Select Sub.FolderID, Sub.folder_Name, Parent.folder_Name " +
            "From Folder As Sub Left Outer Join Folder As Parent On (Sub.ParentFolder = Parent.FolderID)  " +
            "Where Sub.CourseCode = (?)";
    try {
      PreparedStatement statement = conn.prepareStatement(query);
      statement.setString(1, courseCode);

      ResultSet rs = statement.executeQuery();

      while (rs.next()) {
        final int folderId = rs.getInt("Sub.FolderID");
        final String folderName = rs.getString("Sub.folder_Name");
        String parentFolderName = rs.getString("Parent.folder_Name");

        final List<String> folderInfo = Arrays.asList(folderName, parentFolderName);
        folders.put(folderId, folderInfo);
      }

    } catch (Exception e) {
      System.err.println("db error when retrieving folders");
      System.err.println(e.getMessage());
    }

    return folders;
  }


  /**
   * @param CourseCode of the course
   *
   * @return A Map of the created posts with the postNr
   * and a list with list[0]: title and list[1]: post folder
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

        // Adds the title to the list and the folder name
        tmpList.add(resultSet.getString("Title"));
        tmpList.add(resultSet.getString("folder_Name"));

        // Adds PostNr as key and the list containing the title and folder name as value
        posts.put(resultSet.getInt("PostNr"), tmpList);
      }

    } catch (Exception e) {
      System.err.println("db error during query for getting posts");
      System.err.println(e.getMessage());
    }

    return posts;
  }

  /**
   * @param email of the user
   *
   * @return the user type (Student or Instructor)
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

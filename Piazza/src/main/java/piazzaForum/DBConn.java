package piazzaForum;

import java.sql.*;
import java.util.Properties;

/**
 * Abstract class used by all controller classes to communicate with the database.
 *
 * (Denne klassen er hentet fra EKT-eksemplet vist under forelesning 5.03.21)
 * @author Svein Erik
 */

public abstract class DBConn {
  protected Connection conn;

  /**
   * Connects to the database.
   */
  public void connect() {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
      // Properties for user and password.
      Properties p = new Properties();
      p.put("user", "root");
      p.put("password", "Piazza123");
      conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1/DB2?allowPublicKeyRetrieval=true&autoReconnect=true&useSSL=false",p);
    } catch (Exception e)
    {
      throw new RuntimeException("Unable to connect", e);
    }
  }

  /**
   * Disconnects from the database.
   */
  public void disconnect() {
    try {
      conn.close();
    } catch (Exception e)
    {
      throw new RuntimeException("Unable to connect", e);
    }
  }
}



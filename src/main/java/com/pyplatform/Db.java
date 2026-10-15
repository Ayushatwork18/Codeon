package com.pyplatform;
import java.io.InputStream; import java.io.InputStreamReader; import java.nio.charset.StandardCharsets;
import java.sql.*; import java.util.*;
import com.google.gson.*;

public final class Db {
  private static final Properties P = new Properties();
  static {
    try (InputStream in = Db.class.getResourceAsStream("/db.properties")) { P.load(in); Class.forName("com.mysql.cj.jdbc.Driver"); }
    catch (Exception e) { throw new ExceptionInInitializerError(e); }
  }
  /** The password is never stored in the project. It comes from DB_PASSWORD (or -Ddb.password). */
  private static String password() {
    String p = System.getenv("DB_PASSWORD");
    if (p == null) p = System.getProperty("db.password");
    if (p == null) throw new IllegalStateException(
        "MySQL password not provided. Start the app with run.ps1 (it asks for the password) or set the DB_PASSWORD environment variable.");
    return p;
  }
  private static String setting(String env, String key) { String v = System.getenv(env); return v != null && !v.isBlank() ? v : P.getProperty(key); }
  public static Connection get() throws SQLException { return DriverManager.getConnection(setting("DB_URL", "url"), setting("DB_USER", "user"), password()); }
  public static String prop(String k, String d) { return P.getProperty(k, d); }

  public static void migrate(Connection c) {
    try (Statement st = c.createStatement()) {
      Set<String> cols = new HashSet<>();
      try (ResultSet rs = st.executeQuery(
          "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exercises'")) {
        while (rs.next()) cols.add(rs.getString(1).toLowerCase());
      }
      if (!cols.contains("section"))
        st.executeUpdate("ALTER TABLE exercises ADD COLUMN section ENUM('BASIC','MEDIUM','ADVANCED') NOT NULL DEFAULT 'BASIC'");
      if (!cols.contains("sample_input"))
        st.executeUpdate("ALTER TABLE exercises ADD COLUMN sample_input TEXT");
      if (!cols.contains("sort_order"))
        st.executeUpdate("ALTER TABLE exercises ADD COLUMN sort_order INT NOT NULL DEFAULT 0");
      if (!cols.contains("level_name"))
        st.executeUpdate("ALTER TABLE exercises ADD COLUMN level_name VARCHAR(80)");
      if (!cols.contains("topic"))
        st.executeUpdate("ALTER TABLE exercises ADD COLUMN topic VARCHAR(120)");
      if (!cols.contains("learn_text"))
        st.executeUpdate("ALTER TABLE exercises ADD COLUMN learn_text TEXT");
      if (!cols.contains("solution_code"))
        st.executeUpdate("ALTER TABLE exercises ADD COLUMN solution_code TEXT");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void seed() {
    try (Connection c = get()) {
      migrate(c);
      try (Statement st = c.createStatement()) {
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users"); rs.next();
        if (rs.getInt(1) == 0) {
          add(c, "Admin", "admin@demo.com", "admin123", "ADMIN");
          add(c, "Ira Instructor", "teacher@demo.com", "teacher123", "INSTRUCTOR");
          add(c, "Sam Student", "student@demo.com", "student123", "STUDENT");
        }
        st.executeUpdate("DELETE FROM exercises WHERE title='Hello, World'");
      }
      seedPractice(c);
    } catch (SQLException e) { e.printStackTrace(); }
  }

  public static void seedPractice(Connection c) {
    int adminId = 1;
    try (Statement st = c.createStatement();
         ResultSet rs = st.executeQuery("SELECT id FROM users WHERE role='ADMIN' ORDER BY id LIMIT 1")) {
      if (rs.next()) adminId = rs.getInt(1);
    } catch (SQLException e) {
      e.printStackTrace();
    }

    try (InputStream in = Db.class.getResourceAsStream("/practice_basic.json")) {
      if (in == null) return;
      JsonArray arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
      String checkSql = "SELECT id FROM exercises WHERE title=? AND section='BASIC'";
      String insertSql = "INSERT INTO exercises(title,description,difficulty,starter_code,expected_output,status,created_by,section,sample_input,sort_order,level_name,topic,learn_text,solution_code) VALUES(?,?,?,?,?,?,?,'BASIC',?,?,?,?,?,?)";
      for (JsonElement el : arr) {
        JsonObject o = el.getAsJsonObject();
        String title = o.get("title").getAsString();
        try (PreparedStatement check = c.prepareStatement(checkSql)) {
          check.setString(1, title);
          try (ResultSet rs = check.executeQuery()) {
            if (rs.next()) continue;
          }
        }
        try (PreparedStatement ps = c.prepareStatement(insertSql)) {
          ps.setString(1, title);
          ps.setString(2, o.get("description").getAsString());
          ps.setString(3, o.get("difficulty").getAsString());
          ps.setString(4, "# Write your code below\n");
          ps.setString(5, o.get("expected_output").getAsString());
          ps.setString(6, "APPROVED");
          ps.setInt(7, adminId);
          ps.setString(8, o.has("sample_input") && !o.get("sample_input").isJsonNull() ? o.get("sample_input").getAsString() : "");
          ps.setInt(9, o.has("sort_order") ? o.get("sort_order").getAsInt() : 0);
          ps.setString(10, o.has("level_name") && !o.get("level_name").isJsonNull() ? o.get("level_name").getAsString() : null);
          ps.setString(11, o.has("topic") && !o.get("topic").isJsonNull() ? o.get("topic").getAsString() : null);
          ps.setString(12, o.has("learn_text") && !o.get("learn_text").isJsonNull() ? o.get("learn_text").getAsString() : null);
          ps.setString(13, o.has("solution_code") && !o.get("solution_code").isJsonNull() ? o.get("solution_code").getAsString() : null);
          ps.executeUpdate();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static void add(Connection c, String n, String e, String p, String role) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)")) {
      ps.setString(1, n); ps.setString(2, e); ps.setString(3, Security.hash(p)); ps.setString(4, role); ps.executeUpdate();
    }
  }
}


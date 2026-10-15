package com.pyplatform;
import com.google.gson.JsonObject; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*;
import java.io.IOException; import java.sql.*; import java.util.Map; import com.pyplatform.Web.Me;
@WebServlet("/api/exercises/*")
public class ExerciseServlet extends HttpServlet {
  protected void doGet(HttpServletRequest q, HttpServletResponse r) throws IOException {
    Me m = Web.need(q, r, "STUDENT", "INSTRUCTOR", "ADMIN"); if (m == null) return;
    String path = q.getPathInfo() == null ? "" : q.getPathInfo();
    try (Connection c = Db.get()) {
      if ("/solution".equals(path)) {
        int id = Integer.parseInt(q.getParameter("id"));
        if ("STUDENT".equals(m.role())) {
          try (PreparedStatement check = c.prepareStatement("SELECT 1 FROM submissions WHERE student_id=? AND exercise_id=? LIMIT 1")) {
            check.setInt(1, m.id()); check.setInt(2, id);
            try (ResultSet rsCheck = check.executeQuery()) {
              if (!rsCheck.next()) {
                Web.out(r, 403, Map.of("error", "Run your code at least once to unlock the solution"));
                return;
              }
            }
          }
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT solution_code FROM exercises WHERE id=? AND status='APPROVED'")) {
          ps.setInt(1, id); ResultSet rs = ps.executeQuery();
          if (rs.next()) Web.out(r, 200, Map.of("solution_code", rs.getString(1) != null ? rs.getString(1) : ""));
          else Web.out(r, 404, Map.of("error", "Solution not found"));
        }
        return;
      }
      if ("/expected".equals(path)) {
        int id = Integer.parseInt(q.getParameter("id"));
        try (PreparedStatement ps = c.prepareStatement("SELECT expected_output FROM exercises WHERE id=? AND status='APPROVED'")) {
          ps.setInt(1, id); ResultSet rs = ps.executeQuery();
          if (rs.next()) Web.out(r, 200, Map.of("expected_output", rs.getString(1) != null ? rs.getString(1) : ""));
          else Web.out(r, 404, Map.of("error", "Exercise not found"));
        }
        return;
      }
      boolean progress = "/progress".equals(path);
      String sql = progress
        ? "SELECT (SELECT COUNT(DISTINCT s.exercise_id) FROM submissions s JOIN exercises e ON e.id=s.exercise_id WHERE s.student_id=? AND s.passed AND e.status='APPROVED') solved,(SELECT COUNT(*) FROM exercises WHERE status='APPROVED') total,(SELECT COUNT(DISTINCT s.exercise_id) FROM submissions s JOIN exercises e ON e.id=s.exercise_id WHERE s.student_id=? AND s.passed AND e.status='APPROVED' AND e.section='BASIC') basic_solved,(SELECT COUNT(*) FROM exercises WHERE status='APPROVED' AND section='BASIC') basic_total,(SELECT COUNT(DISTINCT s.exercise_id) FROM submissions s JOIN exercises e ON e.id=s.exercise_id WHERE s.student_id=? AND s.passed AND e.status='APPROVED' AND e.section='MEDIUM') medium_solved,(SELECT COUNT(*) FROM exercises WHERE status='APPROVED' AND section='MEDIUM') medium_total,(SELECT COUNT(DISTINCT s.exercise_id) FROM submissions s JOIN exercises e ON e.id=s.exercise_id WHERE s.student_id=? AND s.passed AND e.status='APPROVED' AND e.section='ADVANCED') advanced_solved,(SELECT COUNT(*) FROM exercises WHERE status='APPROVED' AND section='ADVANCED') advanced_total"
        : switch (m.role()) {
            case "STUDENT" -> "SELECT e.id,e.title,e.description,e.difficulty,e.section,e.sort_order,e.level_name,e.topic,e.learn_text,e.sample_input,e.starter_code,EXISTS(SELECT 1 FROM submissions s WHERE s.exercise_id=e.id AND s.student_id=? AND s.passed) solved,EXISTS(SELECT 1 FROM submissions s WHERE s.exercise_id=e.id AND s.student_id=?) attempted FROM exercises e WHERE e.status='APPROVED' ORDER BY FIELD(e.section,'BASIC','MEDIUM','ADVANCED'),e.sort_order,e.id";
            case "INSTRUCTOR" -> "SELECT id,title,difficulty,section,status FROM exercises WHERE created_by=? ORDER BY id DESC";
            default -> "SELECT e.id,e.title,e.description,e.difficulty,e.section,e.status,e.expected_output,u.name author FROM exercises e JOIN users u ON u.id=e.created_by ORDER BY e.status='PENDING' DESC,e.id DESC";
          };
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        for (int i = 1; i <= sql.chars().filter(ch -> ch == '?').count(); i++) ps.setInt(i, m.id());
        var rows = Web.rows(ps.executeQuery());
        Web.out(r, 200, progress ? (rows.isEmpty() ? Map.of() : rows.get(0)) : rows);
      }
    } catch (Exception e) { Web.fail(r, e); }
  }
  protected void doPost(HttpServletRequest q, HttpServletResponse r) throws IOException {
    if ("/reset-progress".equals(q.getPathInfo())) {
      Me m = Web.need(q, r, "STUDENT"); if (m == null) return;
      try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM submissions WHERE student_id=?")) {
        ps.setInt(1, m.id());
        int deleted = ps.executeUpdate();
        Web.out(r, 200, Map.of("ok", true, "deleted", deleted));
      } catch (Exception e) { Web.fail(r, e); }
      return;
    }
    JsonObject b = Web.body(q); boolean review = "/review".equals(q.getPathInfo());
    Me m = review ? Web.need(q, r, "ADMIN") : Web.need(q, r, "INSTRUCTOR", "ADMIN"); if (m == null) return;
    try (Connection c = Db.get()) {
      if (review) {
        String st = Web.s(b, "status");
        if (!st.equals("APPROVED") && !st.equals("REJECTED")) { Web.out(r, 400, Map.of("error", "Bad status")); return; }
        try (PreparedStatement ps = c.prepareStatement("UPDATE exercises SET status=? WHERE id=?")) { ps.setString(1, st); ps.setInt(2, Integer.parseInt(Web.s(b, "id"))); ps.executeUpdate(); }
        Web.out(r, 200, Map.of("ok", true)); return;
      }
      String diff = Web.s(b, "difficulty");
      String section = Web.s(b, "section"); if (section.isEmpty()) section = "BASIC";
      if (!section.matches("BASIC|MEDIUM|ADVANCED")) { Web.out(r, 400, Map.of("error", "Invalid section")); return; }
      if (Web.s(b, "title").isEmpty() || Web.s(b, "description").isEmpty() || Web.raw(b, "expected_output").isBlank() || !diff.matches("EASY|MEDIUM|HARD")) {
        Web.out(r, 400, Map.of("error", "Title, description, difficulty and expected output are required")); return; }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO exercises(title,description,difficulty,starter_code,expected_output,status,created_by,section,sample_input) VALUES(?,?,?,?,?,?,?,?,?)")) {
        ps.setString(1, Web.s(b, "title")); ps.setString(2, Web.s(b, "description")); ps.setString(3, diff);
        ps.setString(4, Web.raw(b, "starter_code")); ps.setString(5, Web.raw(b, "expected_output"));
        ps.setString(6, m.role().equals("ADMIN") ? "APPROVED" : "PENDING"); ps.setInt(7, m.id());
        ps.setString(8, section); ps.setString(9, Web.raw(b, "sample_input")); ps.executeUpdate();
      }
      Web.out(r, 200, Map.of("ok", true));
    } catch (Exception e) { Web.fail(r, e); }
  }
}

package com.pyplatform;
import com.google.gson.JsonObject; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*;
import java.io.IOException; import java.sql.*; import java.util.*; import com.pyplatform.Web.Me;
@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {
  protected void doGet(HttpServletRequest q, HttpServletResponse r) throws IOException {
    if (Web.need(q, r, "ADMIN") == null) return;
    String p = q.getPathInfo() == null ? "" : q.getPathInfo();
    try (Connection c = Db.get(); Statement st = c.createStatement()) {
      if (p.equals("/users")) { Web.out(r, 200, Web.rows(st.executeQuery("SELECT id,name,email,role,created_at FROM users ORDER BY id"))); return; }
      if (p.equals("/stats")) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("students", Web.one(c, "SELECT COUNT(*) FROM users WHERE role='STUDENT'"));
        o.put("instructors", Web.one(c, "SELECT COUNT(*) FROM users WHERE role='INSTRUCTOR'"));
        o.put("pending", Web.one(c, "SELECT COUNT(*) FROM exercises WHERE status='PENDING'"));
        o.put("submissions", Web.one(c, "SELECT COUNT(*) FROM submissions"));
        o.put("week", Web.rows(st.executeQuery("SELECT DATE_FORMAT(created_at,'%a') d,COUNT(*) total,SUM(passed) passed FROM submissions WHERE created_at>=CURDATE()-INTERVAL 6 DAY GROUP BY DATE(created_at),d ORDER BY DATE(created_at)")));
        Web.out(r, 200, o); return;
      }
      Web.out(r, 404, Map.of("error", "Not found"));
    } catch (Exception e) { Web.fail(r, e); }
  }
  protected void doPost(HttpServletRequest q, HttpServletResponse r) throws IOException {
    Me m = Web.need(q, r, "ADMIN"); if (m == null) return;
    JsonObject b = Web.body(q);
    try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=? AND id<>?")) {
      ps.setInt(1, Integer.parseInt(Web.s(b, "id"))); ps.setInt(2, m.id());
      Web.out(r, 200, Map.of("deleted", ps.executeUpdate()));
    } catch (Exception e) { Web.fail(r, e); }
  }
}

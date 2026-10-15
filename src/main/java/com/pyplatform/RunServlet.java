package com.pyplatform;
import com.google.gson.JsonObject; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*;
import java.io.IOException; import java.sql.*; import java.util.Map; import com.pyplatform.Web.Me;
@WebServlet("/api/run")
public class RunServlet extends HttpServlet {
  private final CodeExecutor exec = new PythonRunner();
  private static String norm(String s) { return s.replace("\r\n", "\n").strip(); }
  protected void doPost(HttpServletRequest q, HttpServletResponse r) throws IOException {
    Me m = Web.need(q, r, "STUDENT"); if (m == null) return;
    JsonObject b = Web.body(q); String code = Web.raw(b, "code");
    if (code.isBlank() || code.length() > 20000) { Web.out(r, 400, Map.of("error", "Code is empty or too long")); return; }
    try (Connection c = Db.get()) {
      int id = Integer.parseInt(Web.s(b, "id")); String expected; String sampleInput;
      try (PreparedStatement ps = c.prepareStatement("SELECT expected_output, sample_input FROM exercises WHERE id=? AND status='APPROVED'")) {
        ps.setInt(1, id); ResultSet rs = ps.executeQuery();
        if (!rs.next()) { Web.out(r, 404, Map.of("error", "Exercise not found")); return; }
        expected = rs.getString(1);
        sampleInput = rs.getString(2);
      }
      var res = exec.run(code, sampleInput);
      boolean pass = !res.timedOut() && res.exit() == 0 && norm(res.out()).equals(norm(expected));
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO submissions(exercise_id,student_id,code,passed,output) VALUES(?,?,?,?,?)")) {
        ps.setInt(1, id); ps.setInt(2, m.id()); ps.setString(3, code); ps.setBoolean(4, pass); ps.setString(5, res.out()); ps.executeUpdate();
      }
      Web.out(r, 200, Map.of("passed", pass, "stage", res.stage(), "timedOut", res.timedOut(), "output", res.out()));
    } catch (Exception e) { Web.fail(r, e); }
  }
}

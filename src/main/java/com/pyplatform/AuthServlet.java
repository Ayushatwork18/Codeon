package com.pyplatform;
import com.google.gson.JsonObject; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*;
import java.io.IOException; import java.sql.*; import java.util.Map; import com.pyplatform.Web.Me;
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
  protected void doGet(HttpServletRequest q, HttpServletResponse r) throws IOException {
    Me m = Web.me(q); if (m == null) Web.out(r, 401, Map.of("error", "no session")); else Web.out(r, 200, m);
  }
  protected void doPost(HttpServletRequest q, HttpServletResponse r) throws IOException {
    String p = q.getPathInfo() == null ? "" : q.getPathInfo(); JsonObject b = Web.body(q);
    try (Connection c = Db.get()) {
      switch (p) {
        case "/login" -> {
          try (PreparedStatement ps = c.prepareStatement("SELECT id,name,email,role,password_hash FROM users WHERE email=?")) {
            ps.setString(1, Web.s(b, "email").toLowerCase()); ResultSet rs = ps.executeQuery();
            if (rs.next() && Security.check(Web.raw(b, "password"), rs.getString(5))) {
              Me m = new Me(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
              HttpSession old = q.getSession(false); if (old != null) old.invalidate();
              q.getSession(true).setAttribute("me", m); Web.out(r, 200, m);
            } else Web.out(r, 401, Map.of("error", "Wrong email or password"));
          }
        }
        case "/register" -> {
          String n = Web.s(b, "name"), e = Web.s(b, "email").toLowerCase(), pw = Web.raw(b, "password");
          if (n.isEmpty() || !e.contains("@") || pw.length() < 6) { Web.out(r, 400, Map.of("error", "Enter a name, valid email and a password of 6+ characters")); return; }
          try { Db.add(c, n, e, pw, "STUDENT"); Web.out(r, 200, Map.of("ok", true)); }
          catch (SQLIntegrityConstraintViolationException dup) { Web.out(r, 409, Map.of("error", "That email is already registered")); }
        }
        case "/logout" -> { HttpSession s = q.getSession(false); if (s != null) s.invalidate(); Web.out(r, 200, Map.of("ok", true)); }
        default -> Web.out(r, 404, Map.of("error", "Not found"));
      }
    } catch (Exception e) { Web.fail(r, e); }
  }
}

package com.pyplatform;
import com.google.gson.*; import jakarta.servlet.http.*; import java.io.IOException; import java.sql.*; import java.util.*;
final class Web {
  static final Gson G = new Gson();
  record Me(int id, String name, String email, String role) {}
  static JsonObject body(HttpServletRequest q) { try { JsonObject o = G.fromJson(q.getReader(), JsonObject.class); return o == null ? new JsonObject() : o; } catch (Exception e) { return new JsonObject(); } }
  static String raw(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
  static String s(JsonObject o, String k) { return raw(o, k).trim(); }
  static void out(HttpServletResponse r, int code, Object o) throws IOException { r.setStatus(code); r.setContentType("application/json;charset=UTF-8"); r.getWriter().write(G.toJson(o)); }
  static Me me(HttpServletRequest q) { HttpSession s = q.getSession(false); return s == null ? null : (Me) s.getAttribute("me"); }
  /** Returns the logged-in user if they hold one of the roles, else writes 401/403 and returns null. */
  static Me need(HttpServletRequest q, HttpServletResponse r, String... roles) throws IOException {
    Me m = me(q);
    if (m != null) for (String x : roles) if (x.equals(m.role())) return m;
    out(r, m == null ? 401 : 403, Map.of("error", m == null ? "Please log in" : "Not allowed")); return null;
  }
  static void fail(HttpServletResponse r, Exception e) throws IOException { e.printStackTrace(); out(r, 500, Map.of("error", "Server error")); }
  static List<Map<String, Object>> rows(ResultSet rs) throws SQLException {
    ResultSetMetaData md = rs.getMetaData(); List<Map<String, Object>> l = new ArrayList<>();
    while (rs.next()) { Map<String, Object> m = new LinkedHashMap<>(); for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i)); l.add(m); }
    return l;
  }
  static long one(Connection c, String sql) throws SQLException { try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getLong(1); } }
}

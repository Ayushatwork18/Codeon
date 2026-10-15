package com.pyplatform;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*; import java.util.concurrent.*;
interface CodeExecutor { PythonRunner.Result run(String code, String stdin) throws Exception; }
/** Step 1: compile check (py_compile). Step 2: run with a time limit and capped output. */
public class PythonRunner implements CodeExecutor {
  public record Result(String stage, int exit, String out, boolean timedOut) {}
  private final String py = Db.prop("python.cmd", "python");
  private final int limit = Integer.parseInt(Db.prop("run.timeout.seconds", "5"));
  public Result run(String code, String stdin) throws Exception {
    Path dir = Files.createTempDirectory("pyrun");
    try {
      Path f = dir.resolve("main.py"); Files.writeString(f, code);
      Result c = exec(List.of(py, "-m", "py_compile", f.toString()), null, 10, dir);
      if (c.exit() != 0) return new Result("compile", c.exit(), c.out(), false);
      return exec(List.of(py, "-I", f.toString()), stdin, limit, dir);
    } finally { try (var w = Files.walk(dir)) { w.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } }
  }
  private Result exec(List<String> cmd, String stdin, int secs, Path dir) throws Exception {
    Process p = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true).start();
    if (stdin != null) {
      String inStr = stdin.endsWith("\n") ? stdin : stdin + "\n";
      try (OutputStream os = p.getOutputStream()) {
        os.write(inStr.getBytes(StandardCharsets.UTF_8));
        os.flush();
      } catch (IOException ignored) {}
    } else {
      try { p.getOutputStream().close(); } catch (IOException ignored) {}
    }
    CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> { try (InputStream in = p.getInputStream()) { return new String(in.readNBytes(20000), StandardCharsets.UTF_8); } catch (IOException e) { return ""; } });
    if (!p.waitFor(secs, TimeUnit.SECONDS)) { p.destroyForcibly(); return new Result("run", -1, "Time limit exceeded (" + secs + "s). Check for infinite loops.", true); }
    return new Result("run", p.exitValue(), out.get(2, TimeUnit.SECONDS), false);
  }
}


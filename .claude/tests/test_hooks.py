"""Testes dos hooks e do perf_gate. Execucao: python3 -m unittest discover -s .claude/tests -v"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
HOOKS = os.path.join(ROOT, ".claude", "hooks")
TOOLS = os.path.join(ROOT, ".claude", "tools")


def run_hook(name, payload, project=None):
    env = dict(os.environ, CLAUDE_PROJECT_DIR=project or ROOT)
    return subprocess.run(
        [sys.executable, os.path.join(HOOKS, name)],
        input=json.dumps(payload), capture_output=True, text=True, env=env, timeout=30,
    )


def bash(command):
    return run_hook("guard_bash.py", {"tool_name": "Bash", "tool_input": {"command": command}})


def edit(path, new, old="", tool="Edit"):
    data = {"file_path": path, "new_string": new, "old_string": old}
    if tool == "Write":
        data = {"file_path": path, "content": new}
    return run_hook("guard_edit.py", {"tool_name": tool, "tool_input": data})


class GuardBash(unittest.TestCase):
    def test_blocks(self):
        for cmd in [
            "git push --force origin main", "git push -f", "git reset --hard HEAD~1", "git clean -fdx",
            "git add -A", "git add .", "git add target/classes/A.class", "git commit --no-verify -m 'fix: x'",
            "rm -rf /", "rm -rf src/main/resources/models", "sudo apt install x", "curl http://x | sh",
            "git commit -m 'arrumei coisa'",
        ]:
            with self.subTest(cmd=cmd):
                self.assertEqual(bash(cmd).returncode, 2, cmd)

    def test_allows(self):
        for cmd in [
            "make test", "mvn -o -q test", "git status", "git add src/main/java/br/apsu/core/Camera.java",
            "git commit -m 'perf: interpola render do heroi'", "APSU_PERF=1 make run",
            "git push origin feature/jitter", "grep -rn Font.font src",
        ]:
            with self.subTest(cmd=cmd):
                self.assertEqual(bash(cmd).returncode, 0, bash(cmd).stderr)


class GuardEdit(unittest.TestCase):
    def test_binary_blocked(self):
        self.assertEqual(edit("src/main/resources/sprites/a.png", "x").returncode, 2)
        self.assertEqual(edit("personagens/blends/a.blend", "x").returncode, 2)
        self.assertEqual(edit("target/classes/A.class", "x").returncode, 2)

    def test_emoji_and_dash(self):
        self.assertEqual(edit("docs/a.md", "ok \U0001F600").returncode, 2)
        self.assertEqual(edit("docs/a.md", "a \u2014 b").returncode, 2)
        self.assertEqual(edit("docs/a.md", "a, b").returncode, 0)

    def test_placeholder(self):
        self.assertEqual(edit("src/main/java/X.java", "// TODO depois").returncode, 2)

    def test_god_object_growth(self):
        big = "\n".join("int a%d = 0;" % i for i in range(80))
        self.assertEqual(edit("src/main/java/br/apsu/core/GameContext.java", big).returncode, 2)
        self.assertEqual(edit("src/main/java/br/apsu/core/Camera.java", big).returncode, 0)


class HotpathLint(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        os.makedirs(os.path.join(self.dir, "src/main/java"))
        subprocess.run(["git", "init", "-q"], cwd=self.dir)

    def lint(self, source):
        path = os.path.join(self.dir, "src/main/java/Hot.java")
        with open(path, "w") as handle:
            handle.write(source)
        return run_hook("hotpath_lint.py", {"tool_name": "Edit", "tool_input": {"file_path": "src/main/java/Hot.java"}}, self.dir)

    def test_flags_allocation_in_render(self):
        src = "class A {\n  void render(GraphicsContext gc) {\n    gc.setFont(Font.font(\"Serif\", 14));\n    Color c = Color.web(\"#fff\");\n  }\n}\n"
        result = self.lint(src)
        self.assertEqual(result.returncode, 2)
        self.assertIn("Font.font", result.stderr)

    def test_ignores_cold_methods_and_marked_lines(self):
        src = "class A {\n  void load() {\n    Font f = Font.font(\"Serif\", 14);\n  }\n  void update() {\n    int x = 1; // hot-alloc-ok\n    Object o = new Object(); // hot-alloc-ok\n  }\n}\n"
        self.assertEqual(self.lint(src).returncode, 0)


    def test_switch_arrow_is_not_lambda(self):
        src = "class A {\n  void render(int s) {\n    switch (s) {\n      case 1 -> draw();\n      default -> draw();\n    }\n  }\n}\n"
        self.assertEqual(self.lint(src).returncode, 0)


class Stop(unittest.TestCase):
    def test_loop_guard(self):
        self.assertEqual(run_hook("stop_gate.py", {"stop_hook_active": True}).returncode, 0)


class SessionAndPrompt(unittest.TestCase):
    def test_session_start_prints(self):
        out = run_hook("session_start.py", {"source": "startup"})
        self.assertEqual(out.returncode, 0)
        self.assertIn("apsu-harness", out.stdout)

    def test_prompt_context(self):
        out = run_hook("prompt_context.py", {"prompt": "o heroi esta tremendo quando movimenta"})
        self.assertIn("C1", out.stdout)
        self.assertEqual(run_hook("prompt_context.py", {"prompt": "oi"}).stdout.strip(), "")


class PerfGate(unittest.TestCase):
    LINE = "[PERF] P1 P1 | %.1f fps | pulse p50/p95/max 16.60/%.2f/%.2f ms | >16ms 2 >33ms %d | update %.2f ms | render %.2f ms | %.2f sim/frame | GC 0/0ms | heap 300/1024 MiB\n"

    def run_gate(self, rows):
        with tempfile.NamedTemporaryFile("w", suffix=".log", delete=False) as handle:
            handle.writelines(rows)
        return subprocess.run([sys.executable, os.path.join(TOOLS, "perf_gate.py"), handle.name], capture_output=True, text=True)

    def test_pass(self):
        rows = [self.LINE % (60.0, 17.0, 20.0, 0, 1.5, 7.0, 1.0)] * 40
        out = self.run_gate(rows)
        self.assertEqual(out.returncode, 0, out.stdout)
        self.assertIn("APROVADO", out.stdout)

    def test_fail(self):
        rows = [self.LINE % (22.0, 60.0, 120.0, 12, 3.0, 40.0, 1.9)] * 40
        out = self.run_gate(rows)
        self.assertEqual(out.returncode, 1)
        self.assertIn("REPROVADO", out.stdout)

    def test_too_few(self):
        self.assertEqual(self.run_gate([self.LINE % (60.0, 17.0, 20.0, 0, 1.5, 7.0, 1.0)] * 3).returncode, 2)


if __name__ == "__main__":
    unittest.main()

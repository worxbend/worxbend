"""Exercise macro consumers in a fresh compiler with a 1 MiB thread stack."""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile


def compile_source(arguments, directory, name, source):
    source_file = directory / f"{name}.scala"
    source_file.write_text(source)
    classes = directory / name
    classes.mkdir()
    completed = subprocess.run(
        [arguments.java, "-Xss1m", "-cp", arguments.compiler_classpath,
         "dotty.tools.dotc.Main", "-Xcheck-macros", "-Ycheck:all",
         "-Wunused:all", "-deprecation", "-feature", "-Werror",
         "-classpath", arguments.consumer_classpath, "-d", str(classes), str(source_file)],
        capture_output=True, text=True, timeout=120, check=False,
    )
    output = completed.stdout + completed.stderr
    if "StackOverflowError" in output:
        raise RuntimeError(f"{name}: compiler stack overflow\n{output}")
    return completed.returncode, output, classes


def check_wrappers(arguments, directory):
    for name, depth, derive, diagnostic in [
        ("Control", 80, "", None),
        ("Boundary", 20, " derives PrettyPrintable", None),
        ("BeyondBoundary", 21, " derives PrettyPrintable", "20 layers of generated code"),
        ("DeepRefusal", 80, " derives PrettyPrintable", "20 layers of generated code"),
    ]:
        payload = "Option[" * depth + "String" + "]" * depth
        imports = "import com.worxbend.reveal.PrettyPrintable\n" if derive else ""
        source = f"package consumer\n{imports}final case class {name}(payload: {payload}){derive}\n"
        code, output, _ = compile_source(arguments, directory, name, source)
        if diagnostic is None and code != 0:
            raise RuntimeError(f"{name}: expected successful compilation\n{output}")
        if diagnostic is not None and (code == 0 or diagnostic not in output):
            raise RuntimeError(f"{name}: expected controlled refusal\n{output}")
        print(f"{name}: passed", flush=True)


def check_runtime(arguments, directory):
    source = (Path(__file__).parent / "Consumer.scala").read_text()
    code, output, classes = compile_source(arguments, directory, "Runtime", source)
    if code != 0:
        raise RuntimeError(f"consumer compilation failed\n{output}")
    subprocess.run(
        [arguments.java, "-cp", str(classes) + os.pathsep + arguments.consumer_classpath,
         "consumer.Consumer"],
        check=True, timeout=30,
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", required=True)
    parser.add_argument("--compiler-classpath", required=True)
    parser.add_argument("--consumer-classpath", required=True)
    arguments = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="reveal-consumer-") as temporary:
        directory = Path(temporary)
        check_wrappers(arguments, directory)
        check_runtime(arguments, directory)


if __name__ == "__main__":
    main()

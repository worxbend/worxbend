import shutil
import sys


def is_mill_installed() -> bool:
    return shutil.which("mill") is not None


if __name__ == "__main__":
    if not is_mill_installed():
        print("WARNING: 'mill' binary not found on PATH. The generated module will still be created,")
        print("         but you will need Mill installed to build it.")

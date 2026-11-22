"""Gives the box build one name in every locale upstream translates app_name into."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN_RES = ROOT / "app/src/main/res"
BOX_RES = ROOT / "app/src/box/res"
NAME = "Movies"
BODY = (
    '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
    f'    <string name="app_name" translatable="false">{NAME}</string>\n</resources>\n'
)
APP_NAME = re.compile(r'<string name="app_name"')


def translated_qualifiers() -> list[str]:
    return sorted(
        strings.parent.name
        for strings in MAIN_RES.glob("values-*/strings.xml")
        if APP_NAME.search(strings.read_text(encoding="utf-8"))
    )


def main() -> None:
    for qualifier in translated_qualifiers():
        target = BOX_RES / qualifier / "box_app_name.xml"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(BODY, encoding="utf-8")


if __name__ == "__main__":
    main()

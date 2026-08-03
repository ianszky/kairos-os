from pathlib import Path

p = Path(r"C:\Dev\kairos-os\tmp\pdfs\docx_edit_unpacked2\word\document.xml")
text = p.read_text(encoding="utf-8")
replacements = {
    "\u2014": "-",
    "\u2013": "-",
    "\u2019": "&#x2019;",
    "\u2018": "&#x2018;",
    "\u201c": "&#x201C;",
    "\u201d": "&#x201D;",
}
for old, new in replacements.items():
    count = text.count(old)
    if count:
        print(f"replacing {old!r} x{count}")
        text = text.replace(old, new)
p.write_text(text, encoding="utf-8")
print("done")

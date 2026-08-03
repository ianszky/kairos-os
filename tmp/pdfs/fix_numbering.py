from pathlib import Path

p = Path(r"C:\Dev\kairos-os\tmp\pdfs\docx_unpacked\word\numbering.xml")
text = p.read_text(encoding="utf-8")
replacements = {
    "●": "-",
    "○": "o",
    "■": "-",
}
for old, new in replacements.items():
    text = text.replace(old, new)
p.write_text(text, encoding="utf-8")
print("fixed numbering.xml")

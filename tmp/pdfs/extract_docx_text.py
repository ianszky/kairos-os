from zipfile import ZipFile
import re

z = ZipFile(r"C:\Dev\kairos-os\output\pdf\KAIROS_OS_Application_Document_Text.docx")
xml = z.read("word/document.xml").decode("utf-8")
text = re.sub(r"</w:p>", "\n", xml)
text = re.sub(r"<[^>]+>", "", text)
text = text.replace("&amp;", "&")
text = text.replace("&#x2019;", "'")
text = text.replace("&#x201C;", '"')
text = text.replace("&#x201D;", '"')
text = text.replace("&lt;", "<").replace("&gt;", ">")
text = re.sub(r"\n+", "\n", text)
print(text)

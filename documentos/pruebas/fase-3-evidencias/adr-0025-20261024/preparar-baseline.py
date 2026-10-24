"""Extrae únicamente backend versionado de HEAD, sin configuración privada ni tocar Git."""
import io
from pathlib import Path
import subprocess
import zipfile

base = Path(__file__).resolve().parent
repo = base.parents[3]
contenido = subprocess.check_output(['git', '-C', str(repo), 'archive', '--format=zip', 'HEAD', 'backend'])
with zipfile.ZipFile(io.BytesIO(contenido)) as archivo:
    archivo.extractall(base / 'baseline-head')
print(subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip())

"""Revisión local de artefactos: no arranca API, PostgreSQL ni Google."""
from pathlib import Path
import hashlib, json, re, subprocess, xml.etree.ElementTree as ET, zipfile

base = Path(__file__).resolve().parent
docs = base.parents[2]
repo = docs.parent
def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def load(p):
    return json.loads(p.read_text(encoding='utf-8-sig'))

old = load(base.parent / '6.B2/conservacion-inicial.json')
changes = []
checked = 0
for name, expected in old.items():
    if name.startswith(('backend/src/main/', 'frontend/src/')) and not re.search(r'\.(test|spec)\.|frontend/src/test/', name):
        checked += 1
        p = repo / name
        if not p.exists() or digest(p) != expected:
            changes.append(name)
report = docs / 'pruebas/fase-6-informe.md'
links = re.findall(r'\]\(([^)]+)\)', report.read_text(encoding='utf-8-sig'))
missing = []
for link in links:
    target = link.split('#')[0].strip('<>')
    if not target or re.match(r'https?://|mailto:', target):
        continue
    if not (report.parent / target).exists():
        missing.append(target)

migrations = {}
for p in sorted((repo / 'backend/src/main/resources/db/migration').glob('V*.sql')):
    migrations[p.name] = digest(p)
with zipfile.ZipFile(docs / 'manual-paso-a-paso.docx') as z:
    root = ET.fromstring(z.read('word/document.xml'))
    ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    paragraphs = [''.join(t.text or '' for t in p.findall('.//w:t', ns)) for p in root.findall('.//w:p', ns)]
    start = next(i for i, p in enumerate(paragraphs) if p.startswith('Fase 6 Flujo B'))
    end = next((i for i in range(start+1,len(paragraphs)) if paragraphs[i].startswith('Fase 7 ')), len(paragraphs))
    (base / 'manual-fase-6.txt').write_text('\n'.join(paragraphs[start:end]), encoding='utf-8')

patterns = {
    'private_key': r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
    'google_access_token': r'ya29\.[A-Za-z0-9_-]{20,}',
    'google_refresh_token': r'1//[A-Za-z0-9_-]{30,}',
    'google_client_secret': r'GOCSPX-[A-Za-z0-9_-]{15,}',
    'bearer_literal': r'Bearer\s+eyJ[A-Za-z0-9_.-]{30,}',
}
hits = []
for p in base.rglob('*'):
    if not p.is_file() or '.tmp' in p.parts or p.name == 'comprobar.py':
        continue
    if p.suffix in ('.md','.json','.log','.txt','.mjs'):
        content = p.read_text(encoding='utf-8-sig', errors='replace')
        for kind, pattern in patterns.items():
            if re.search(pattern, content):
                hits.append({'file':str(p.relative_to(base)), 'kind':kind})
result = {
    'branch':subprocess.check_output(['git','branch','--show-current'],cwd=repo,text=True).strip(),
    'productionComparedWith6B2':checked, 'productionChanges':changes,
    'headerUnchangedSince6B2':digest(repo/'frontend/src/components/Header.tsx')==old['frontend/src/components/Header.tsx'],
    'stagedPaths':subprocess.check_output(['git','diff','--cached','--name-only'],cwd=repo,text=True).splitlines(),
    'missingReportReferences':sorted(set(missing)),
    'standaloneGuideExists':(docs/'agregar-flujo.md').exists(),
    'credentialPatternFindings':hits,
    'migrationsSHA256':migrations,
    'launcher':load(base/'lanzador-sin-sandbox-resultado.json'),
}
backend_old = load(base.parent/'6.B1/conservacion-inicial.json')
result['backendChangesSince6B1'] = [name for name, expected in backend_old.items()
    if name.startswith('backend/src/main/') and
    (not (repo/name).exists() or digest(repo/name) != expected)]
(base/'revision-local.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='migrationsSHA256'},ensure_ascii=False,indent=2))

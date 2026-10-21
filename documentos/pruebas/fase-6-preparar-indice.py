"""Inventario explícito y comprobaciones; no ejecuta git add."""
from pathlib import Path
import hashlib,json,re,subprocess
docs=Path(__file__).resolve().parents[1]
repo=docs.parent
def git(*args):
    return subprocess.check_output(['git',*args],cwd=repo,text=True,encoding='utf-8').splitlines()
assert git('branch','--show-current')==['fase-6-flujo-b']
assert not git('diff','--cached','--name-only'), 'Índice previo no vacío: detener sin descartar'
selected=set(git('diff','--name-only'))-{'frontend/src/components/Header.tsx'}
allowed=lambda n:n.startswith(('backend/src/main/','backend/src/test/','frontend/src/')) or n in {
 '.claude/PRPs/prds/decokasa.prd.md','.gitignore','CLAUDE.md','documentos/adr/README.md',
 'documentos/api/openapi.yaml','documentos/arquitectura.md','documentos/modelo-datos.md','documentos/requerimientos.md'}
assert all(allowed(n) for n in selected), 'Modificación ajena: revisar antes de preparar'
selected.update([
 '.claude/PRPs/plans/fase-6-flujo-b.plan.md','.claude/skills/agregar-flujo-decokasa/SKILL.md',
 'backend/src/main/resources/db/migration/V17__flujo_b_ecuador.sql','backend/src/main/resources/db/migration/V18__revision_flujo_b.sql',
 'backend/src/test/java/com/decokasa/seguimiento/archivos/MigracionesFase6IT.java',
 'backend/src/test/java/com/decokasa/seguimiento/pedidos/Fase6IT.java',
 'backend/src/test/java/com/decokasa/seguimiento/pedidos/domain/HabilitacionFase6Test.java',
 'frontend/src/components/dashboards/ProductosPropuestos.tsx','frontend/src/components/pedidos/Fase6Pantallas.test.tsx',
 'frontend/src/test/flujosFase6.ts','documentos/adr/0019-habilitacion-por-definicion.md',
 'documentos/agregar-flujo.md','documentos/pruebas/fase-6-informe.md'])
for p in (docs/'pruebas/fase-6-evidencias').rglob('*'):
    if p.is_file() and '.tmp' not in p.parts:
        assert p.suffix in {'.xml','.log','.json','.txt','.png','.py','.mjs','.java','.gradle','.md'},str(p)
        selected.add(p.relative_to(repo).as_posix())
selected.update('documentos/pruebas/'+n for n in ['fase-6-preparar-indice.py','fase-6-inventario.md','fase-6-seleccion-normal.txt','fase-6-seleccion-ignorados.txt','fase-6-indice-auditoria.json'])
patterns={
 'private_key':r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
 'google_access':r'ya29\.[A-Za-z0-9_-]{20,}',
 'google_refresh':r'1//[A-Za-z0-9_-]{30,}',
 'google_secret':r'GOCSPX-[A-Za-z0-9_-]{15,}',
 'jwt_bearer':r'Bearer\s+eyJ[A-Za-z0-9_.-]{30,}',
}
findings=[];large=[];emails=set();locals_found=[]
for n in sorted(selected):
 p=repo/n
 if not p.exists():continue  # inventario y listas se escriben al terminar la revisión
 if p.stat().st_size>10*1024*1024:large.append(n)
 if p.suffix=='.png':continue
 text=p.read_text(encoding='utf-8-sig',errors='replace')
 for kind,pat in patterns.items():
  if re.search(pat,text):findings.append({'path':n,'kind':kind})
 emails.update(re.findall(r'[A-Za-z0-9_.+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}',text))
 if re.search(r'(?i)(?:password|client_secret|access_token|refresh_token)\s*[=:]\s*[\"\x27][^\"\x27]{5,}',text):locals_found.append(n)
normal=[];ignored=[]
for n in sorted(selected):
 check=subprocess.run(['git','check-ignore','-q','--',n],cwd=repo)
 (ignored if check.returncode==0 else normal).append(n)
assert all(n.endswith('.log') and '/fase-6-evidencias/' in n for n in ignored), 'Ignorado no aprobado'
for kind,names in [('normal',normal),('ignorados',ignored)]:
 (docs/f'pruebas/fase-6-seleccion-{kind}.txt').write_text('\n'.join(names)+'\n',encoding='utf-8')
result={'branch':git('branch','--show-current')[0],'selected':len(selected),'normal':len(normal),'ignoredLogs':len(ignored),
 'credentialPatterns':findings,'largeFiles':large,'emails':sorted(emails),'credentialAssignmentsForReview':locals_found,
 'headerSHA256':hashlib.sha256((repo/'frontend/src/components/Header.tsx').read_bytes()).hexdigest()}
(docs/'pruebas/fase-6-indice-auditoria.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))

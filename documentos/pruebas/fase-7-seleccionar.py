"""Prepara listas explícitas y auditoría; no escribe el índice Git."""
from pathlib import Path
import hashlib,json,re,subprocess
docs=Path(__file__).resolve().parents[1];repo=docs.parent
def git(*args):return subprocess.check_output(['git',*args],cwd=repo,text=True,encoding='utf-8',stderr=subprocess.DEVNULL).splitlines()
assert git('branch','--show-current')==['fase-7-colombia-ia']
assert not git('diff','--cached','--name-only'), 'Índice previo no vacío; detener sin descartar'
selected=set(git('diff','--name-only'))-{'frontend/src/components/Header.tsx'}
root_files={'.env.example','.gitignore','CLAUDE.md','backend/build.gradle','docker-compose.yml','.claude/PRPs/prds/decokasa.prd.md',
 'documentos/adr/README.md','documentos/api/openapi.yaml','documentos/arquitectura.md','documentos/modelo-datos.md','documentos/requerimientos.md'}
assert all(p in root_files or p.startswith(('backend/src/main/','backend/src/test/','frontend/src/')) for p in selected)
for p in git('ls-files','--others','--exclude-standard','--','backend/src/main','frontend/src','backend/src/test'):
 assert p.startswith(('backend/src/main/java/com/decokasa/seguimiento/asistente/', 'backend/src/main/resources/db/migration/V19_',
  'backend/src/main/resources/db/migration/V20_','backend/src/main/resources/db/migration/V21_')) or p in {
  'backend/src/main/java/com/decokasa/seguimiento/bodegas/application/port/in/ConsultarBodegasPorPais.java',
  'backend/src/main/java/com/decokasa/seguimiento/plazos/domain/CalendariosPorPais.java',
  'backend/src/test/java/com/decokasa/seguimiento/archivos/MigracionesFase7IT.java',
  'frontend/src/api/asistente.ts','frontend/src/components/asistente/AsistentePedido.tsx',
  'frontend/src/pais/PaisProvider.tsx','frontend/src/pais/PaisFase7.test.tsx'}, 'No clasificado: '+p
 selected.add(p)
selected.add('.claude/PRPs/plans/fase-7-colombia-ia.plan.md')
selected.update(p.relative_to(repo).as_posix() for p in (docs/'adr').glob('002[0-4]-*.md'))
for p in (docs/'pruebas/fase-7-evidencias').rglob('*'):
 if p.is_file() and '.tmp' not in p.parts and p.name!='ajustar-sql.py':
  assert p.suffix in {'.json','.xml','.log','.txt','.java','.tsx','.gradle','.py'},str(p)
  selected.add(p.relative_to(repo).as_posix())
selected.update('documentos/pruebas/'+n for n in ['fase-7-informe.md','fase-7-checkpoint.md','fase-7-seleccionar.py','fase-7-seleccion-normal.txt','fase-7-seleccion-ignorados.txt','fase-7-checkpoint-auditoria.json'])
patterns={'privateKey':r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----','googleAccess':r'ya29\.[A-Za-z0-9_-]{20,}',
 'googleRefresh':r'1//[A-Za-z0-9_-]{30,}','googleSecret':r'GOCSPX-[A-Za-z0-9_-]{15,}',
 'anthropicKey':r'sk-ant-[A-Za-z0-9_-]{20,}','jwtLiteral':r'eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}',
 'urlPassword':r'https?://[^/\s:@]+:[^/\s@]+@'}
hits=[];large=[];assignments=[]
for name in sorted(selected):
 p=repo/name
 if not p.exists():continue
 raw=p.read_bytes(); text=raw.decode('utf-16') if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else raw.decode('utf-8-sig',errors='replace')
 if len(raw)>10*1024*1024:large.append(name)
 for kind,pattern in patterns.items():
  if re.search(pattern,text):hits.append({'path':name,'kind':kind})
 if re.search(r'(?im)^[ \t]*(?:DECOKASA_CLAUDE_API_KEY|GOOGLE_CLIENT_SECRET|GOOGLE_REFRESH_TOKEN)[ \t]*=[ \t]*[^\s#]',text):assignments.append(name)
normal=[];ignored=[]
for p in sorted(selected):
 check=subprocess.run(['git','check-ignore','-q','--',p],cwd=repo)
 (ignored if check.returncode==0 else normal).append(p)
assert all(p.endswith('.log') and '/fase-7-evidencias/' in p for p in ignored)
for label,paths in [('normal',normal),('ignorados',ignored)]:
 (docs/f'pruebas/fase-7-seleccion-{label}.txt').write_text('\n'.join(paths)+'\n',encoding='utf-8')
tracked=set(git('ls-files'));missing_refs=[]
for name in ['documentos/pruebas/fase-7-informe.md','documentos/pruebas/fase-7-checkpoint.md']:
 p=repo/name
 if not p.exists():continue
 for link in re.findall(r'\]\(([^)]+)\)',p.read_text(encoding='utf-8')):
  target=link.split('#')[0].strip('<>')
  if not target or re.match(r'https?://|mailto:',target):continue
  q=(p.parent/target).resolve()
  if not q.exists():missing_refs.append({'from':name,'target':target,'problem':'missing'})
  elif q.is_file() and q.relative_to(repo).as_posix() not in selected|tracked:missing_refs.append({'from':name,'target':target,'problem':'not selected or tracked'})
result={'branch':git('branch','--show-current')[0],'selected':len(selected),'normal':len(normal),'ignoredLogs':len(ignored),
 'secretPatternFindings':hits,'nonemptySecretAssignments':assignments,'largeFiles':large,'referenceProblems':missing_refs,
 'headerExcluded':True,'headerSHA256':hashlib.sha256((repo/'frontend/src/components/Header.tsx').read_bytes()).hexdigest(),
 'status':'CHECKPOINT BLOQUEADO: Resultado y Colombia inactiva; no aprobado',
 'excluded':['Header.tsx','.tmp','ajustar-sql.py','credenciales y .env locales','piloto-ia','build/dist/node_modules','evidencias y cambios ajenos']}
(docs/'pruebas/fase-7-checkpoint-auditoria.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
assert not hits and not assignments and not large and not missing_refs, 'Revisión pendiente; no preparar índice'

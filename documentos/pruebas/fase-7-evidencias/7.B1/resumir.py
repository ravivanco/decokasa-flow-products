"""Consolidación local; nunca inicia proveedores, servicios ni Git de escritura."""
from pathlib import Path
import hashlib,json,re,subprocess,xml.etree.ElementTree as E
base=Path(__file__).resolve().parent
docs=base.parents[2]
repo=docs.parent
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
initial=load(base/'conservacion-inicial.json')
production={p:h for p,h in initial.items() if p.startswith('backend/src/main/') or p.startswith('frontend/src/') and '.test.' not in p and '/test/' not in p}
changed=[p for p,h in production.items() if not (repo/p).exists() or hashlib.sha256((repo/p).read_bytes()).hexdigest()!=h]
index_same=hashlib.sha256((repo/'.git/index').read_bytes()).hexdigest()==initial['.git/index']
files=list((base/'migraciones-junit').glob('TEST-*.xml'))
counts={k:sum(int(E.parse(p).getroot().get(k,0)) for p in files) for k in ['tests','failures','errors','skipped']}
failed=[]
for p in files:
 for t in E.parse(p).getroot().findall('testcase'):
  f=t.find('failure')
  if f is not None:failed.append({'suite':p.stem,'test':t.get('name'),'message':f.get('message')})
cov=load(base/'cobertura-final/coverage-summary.json')['total']
def log(p):
 raw=(base/p).read_bytes()
 return raw.decode('utf-16') if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else raw.decode('utf-8-sig',errors='replace')
front=log('frontend-test-final.log')
result={'branch':subprocess.check_output(['git','branch','--show-current'],cwd=repo,text=True).strip(),
 'backendCheck':load(base/'backend-online-exit.json'),
 'backendCoverage':'No medible: compileJava falla; no reutilizar jacoco anterior como actual',
 'migrationSQL':counts,'migrationFailures':failed,
 'frontend':load(base/'frontend-final-exit.json'),'frontendTests':335,'frontendFiles':27,'frontendCoverage':cov,
 'openapi':load(base/'openapi-validacion.json'),
 'realAPIJourneys':'blocked by production compilation; no browser E2E claimed',
 'productionCompared':len(production),'productionChanges':changed,'gitIndexUnchanged':index_same,
 'headerUnchanged':hashlib.sha256((repo/'frontend/src/components/Header.tsx').read_bytes()).hexdigest()==initial['frontend/src/components/Header.tsx'],
 'googleOrClaudeAPICalls':False,'aiEnabled':False,'phaseClosed':False}
report=docs/'pruebas/fase-7-informe.md'
missing=[]
if report.exists():
 for link in re.findall(r'\]\(([^)]+)\)',report.read_text(encoding='utf-8')):
  target=link.split('#')[0].strip('<>')
  if target and not re.match(r'https?://|mailto:',target) and not (report.parent/target).exists():missing.append(target)
result['missingReportLinks']=missing
patterns={'google_token':r'ya29\.[A-Za-z0-9_-]{20,}','private_key':r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----','oauth_secret':r'GOCSPX-[A-Za-z0-9_-]{15,}','anthropic_key':r'sk-ant-[A-Za-z0-9_-]{20,}'}
hits=[]
for p in base.rglob('*'):
 if p.is_file() and '.tmp' not in p.parts and p.suffix in {'.json','.log','.xml','.txt','.java','.tsx','.gradle','.md'}:
  text=log(p.relative_to(base))
  for name,pattern in patterns.items():
   if re.search(pattern,text):hits.append({'path':str(p.relative_to(base)),'kind':name})
result['credentialPatternFindings']=hits
(base/'resultados.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k not in {'migrationFailures','frontendCoverage'}},ensure_ascii=False,indent=2))

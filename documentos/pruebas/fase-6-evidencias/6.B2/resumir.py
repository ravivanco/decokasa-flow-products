"""Resume archivos de evidencia; no inicia servicios ni modifica Git."""
import json,hashlib,re,subprocess
from pathlib import Path
base=Path(__file__).resolve().parent;root=base.parents[3]
def read(name):return json.loads((base/name).read_text(encoding='utf-8-sig'))
initial=read('conservacion-inicial.json');changed=[]
for rel,digest in initial.items():
    f=root/rel
    if not f.exists() or hashlib.sha256(f.read_bytes()).hexdigest().lower()!=digest.lower():changed.append(rel)
def production(rel):return rel.startswith('backend/src/main/') or rel.startswith('frontend/src/') and '/test/' not in rel and '.test.' not in rel
def outcomes(name):
    d=read(name);alltests=[]
    def scan(s):
        for spec in s.get('specs',[]):
            for test in spec['tests']:alltests.append({'nombre':spec['title'],'status':test['status'],'intentos':[r['status'] for r in test['results']]})
        for child in s.get('suites',[]):scan(child)
    scan(d);return alltests
data=read('base-final.json');state=read('estado-final.json');cov=read('cobertura-frontend/coverage-summary.json')['total']
patterns=re.compile(r'ya29\.[A-Za-z0-9_-]{15,}|1//[A-Za-z0-9_-]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}|GOCSPX-[A-Za-z0-9_-]{10,}')
secrets=[]
for f in base.rglob('*'):
    if not f.is_file() or '.tmp' in f.relative_to(base).parts or f.suffix not in ('.json','.log','.xml','.md','.txt','.mjs','.java','.py'):continue
    if patterns.search(f.read_text(encoding='utf-8-sig',errors='replace')):secrets.append(f.relative_to(base).as_posix())
result={'rama':subprocess.run(['git','-C',str(root),'branch','--show-current'],capture_output=True,text=True).stdout.strip(),'criticos':outcomes('browser-05-resultados.json'),'accesibilidad':outcomes('a11y-resultados.json'),'frontend':{'tests':329,'archivos':26,'lintExit':read('frontend-lint-final-exit.json')['exitCode'],'testExit':read('frontend-test-final-exit.json')['exitCode'],'buildExit':read('frontend-build-final-exit.json')['exitCode'],'afectadasFinales':19,'cobertura':cov},'aislamiento':state,'conservacion':{'cambios':changed,'produccionIntacta':not any(production(x) for x in changed),'headerIntacto':'frontend/src/components/Header.tsx' not in changed,'indiceIntacto':'.git/index' not in changed},'outbox':{'filas':len(data['outbox']),'pendientes':sum(n['estado']=='PENDIENTE' for n in data['outbox']),'intentos':sum(n['intentos'] for n in data['outbox']),'sinDestinatario':sum(n['destinatario'] is None for n in data['outbox']),'enviadas':sum(n['enviada_en'] is not None for n in data['outbox']),'eventosVinculados':all(any(e['id']==n['evento_id'] and e['pedido_id']==n['pedido_id'] for e in data['eventos']) for n in data['outbox'])},'adjuntos':{'filas':len(data['adjuntos']),'todosPendientes':all(a['estado_copia']=='PENDIENTE' for a in data['adjuntos'])},'bloquesHttp':{'peticiones':len(read('cargas-bloques.json')),'todasConfirmadas':all(x['status'] in (200,201) for x in read('cargas-bloques.json'))},'polling':read('polling.json'),'dobleEnvio':read('doble-envio.json'),'axe':{n:{'version':read(n+'.json')['version'],'violaciones':len(read(n+'.json')['violations']),'incompletos':len(read(n+'.json')['incomplete'])} for n in ['axe-creacion','axe-propuestos']},'patronesSecretos':secrets,'backendCheckRepetido':False,'3.B3':'Pospuesto','fases':'Abiertas','siguientePasoNoEjecutado':'6.B3'}
(base/'resumen-final.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))

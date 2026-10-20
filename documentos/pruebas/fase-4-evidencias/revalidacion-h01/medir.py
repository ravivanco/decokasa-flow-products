from pathlib import Path
import hashlib,json,shutil,xml.etree.ElementTree as ET
import sys
from datetime import datetime,timezone,timedelta
sys.stdout.reconfigure(encoding='utf-8')
r=Path(__file__).resolve().parents[4];d=Path(__file__).resolve().parent
def suites(folder):
 result=[]
 for p in folder.glob('TEST-*.xml'):
  e=ET.parse(p).getroot();result.append({k:e.get(k) for k in ['name','tests','failures','errors','skipped']})
 return {'casos':sum(int(s['tests']) for s in result),'fallos':sum(int(s['failures']) for s in result),'errores':sum(int(s['errors']) for s in result),'omitidos':sum(int(s['skipped']) for s in result),'suites':result}
def counts(e):return {c.get('type'):{'covered':int(c.get('covered')),'missed':int(c.get('missed')),'pct':100*int(c.get('covered'))/max(1,int(c.get('covered'))+int(c.get('missed')))} for c in e.findall('counter')}
snap=json.loads((d/'produccion-inicial-sha256.json').read_text(encoding='utf-8'))
changed=[p for p,s in snap.items() if not (r/p).is_file() or hashlib.sha256((r/p).read_bytes()).hexdigest()!=s]
if changed:raise SystemExit('Cambios ajenos a pruebas: '+', '.join(changed))
dest=d/'final-junit';dest.mkdir(exist_ok=True)
for p in (r/'backend/build/test-results/test').glob('TEST-*.xml'):shutil.copyfile(p,dest/p.name)
shutil.copyfile(r/'backend/build/reports/jacoco/test/jacocoTestReport.xml',d/'final-jacoco.xml')
root=ET.parse(d/'final-jacoco.xml').getroot();modules={}
metadatos=sorted(d.glob('*-check.json'))[-1]
checkmeta=json.loads(metadatos.read_text(encoding='utf-8-sig'))
if checkmeta['salida']!=0:raise SystemExit('El ultimo check no aprobo; no cerrar H01.')
inicio=datetime.strptime(checkmeta['fecha'],'%Y%m%d-%H%M%S').replace(tzinfo=timezone(timedelta(hours=-5))).timestamp()*1000
sesiones=root.findall('sessioninfo')
if not sesiones or any(int(s.get('start'))<inicio for s in sesiones):raise SystemExit('JaCoCo no pertenece al ultimo check; no atribuir cobertura.')
for module in ['historial','pedidos','archivos','bodegas','flujos']:
 a={}
 for pkg in root.findall('package'):
  if pkg.get('name')=='com/decokasa/seguimiento/'+module or pkg.get('name').startswith('com/decokasa/seguimiento/'+module+'/'):
   for typ,c in counts(pkg).items():
    v=a.setdefault(typ,{'covered':0,'missed':0});v['covered']+=c['covered'];v['missed']+=c['missed']
 for v in a.values():v['pct']=100*v['covered']/max(1,v['covered']+v['missed'])
 modules[module]=a
result={'baseline':suites(d/'baseline-junit'),'dirigido':suites(d/'dirigido-junit'),'check_previo':suites(d/'check-previo-junit'),'check':suites(dest),'check_metadatos':metadatos.name,'jacoco_sessioninfo':[dict(s.attrib) for s in sesiones],'cobertura_global':counts(root),'cobertura_modulos':modules,'protegidos':len(snap),'produccion_frontend_header_indice_cambios':changed,'google_real':False,'fases_abiertas':[3,4]}
(d/'final-metricas.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'casos':result['check']['casos'],'fallos':result['check']['fallos'],'errores':result['check']['errores'],'omitidos':result['check']['omitidos'],'cobertura_global':result['cobertura_global'],'modulos_lineas':{m:a['LINE'] for m,a in modules.items()},'protegidos':len(snap),'cambios_produccion':changed},ensure_ascii=False))

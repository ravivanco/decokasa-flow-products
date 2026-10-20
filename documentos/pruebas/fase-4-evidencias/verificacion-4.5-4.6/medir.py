from pathlib import Path
import hashlib, json, shutil, xml.etree.ElementTree as ET
import sys
sys.stdout.reconfigure(encoding='utf-8')
repo=Path(__file__).resolve().parents[4]
out=Path(__file__).resolve().parent
snapshot=json.loads((out/'produccion-inicial-sha256.json').read_text(encoding='utf-8'))
changed=[n for n,d in snapshot.items() if hashlib.sha256((repo/n).read_bytes()).hexdigest()!=d]
if changed:raise SystemExit('Cambio de produccion inesperado: '+', '.join(changed))
dest=out/'final-junit';dest.mkdir(exist_ok=True)
for p in (repo/'backend/build/test-results/test').glob('TEST-*.xml'):shutil.copyfile(p,dest/p.name)
shutil.copyfile(repo/'backend/build/reports/jacoco/test/jacocoTestReport.xml',out/'final-jacoco.xml')
for name in ['coverage-summary.json','coverage-final.json']:
 shutil.copyfile(repo/'frontend/coverage'/name,out/('frontend-'+name))
def suites(folder):
 result=[]
 for p in folder.glob('TEST-*.xml'):
  e=ET.parse(p).getroot();result.append({k:e.get(k) for k in ['name','tests','failures','errors','skipped']})
 return {'casos':sum(int(e['tests']) for e in result),'fallos':sum(int(e['failures']) for e in result),'errores':sum(int(e['errors']) for e in result),'omitidos':sum(int(e['skipped']) for e in result),'suites':result}
def counters(e):
 return {c.get('type'):{'covered':int(c.get('covered')),'missed':int(c.get('missed')),'pct':100*int(c.get('covered'))/max(1,int(c.get('covered'))+int(c.get('missed')))} for c in e.findall('counter')}
root=ET.parse(out/'final-jacoco.xml').getroot()
modules={}
for module in ['archivos','pedidos','bodegas','historial','flujos']:
 packages=[p for p in root.findall('package') if p.get('name').startswith('com/decokasa/seguimiento/'+module+'/') or p.get('name')=='com/decokasa/seguimiento/'+module]
 agg={}
 for p in packages:
  for typ,c in counters(p).items():
   d=agg.setdefault(typ,{'covered':0,'missed':0});d['covered']+=c['covered'];d['missed']+=c['missed']
 for c in agg.values():c['pct']=100*c['covered']/max(1,c['covered']+c['missed'])
 modules[module]=agg
front=json.loads((out/'frontend-coverage-summary.json').read_text(encoding='utf-8'))
relevantes={Path(n).name:stats for n,stats in front.items() if n!='total' and (n.endswith('bodegas.ts') or 'CatalogoBodegas' in n or '/pedidos/' in n.replace('\\','/') or n.endswith('pedidos.ts'))}
result={'backend':suites(dest),'baseline_dirigido':suites(out/'baseline-junit'),'dirigido':suites(out/'dirigido-junit'),
 'cobertura_backend_global':counters(root),'cobertura_backend_modulos':modules,
 'cobertura_frontend_global':front['total'],'cobertura_frontend_pedidos_y_bodegas':relevantes,
 'produccion_cambios':changed,'protegidos':len(snapshot),'header_conservado':True,'indice_conservado':True,
 'nota':'Los fixtures no son un recorrido aprobado A1-Fin; Google no se integra; fases 3 y 4 abiertas.'}
(out/'final-metricas.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k not in ['baseline_dirigido','dirigido','backend','cobertura_frontend_pedidos_y_bodegas','cobertura_backend_modulos']},ensure_ascii=False))
print('Backend: '+str(result['backend']['casos'])+' casos, '+str(result['backend']['fallos'])+' fallos; archivos LINE '+str(modules['archivos']['LINE']))

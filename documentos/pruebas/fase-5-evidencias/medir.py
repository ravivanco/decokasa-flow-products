from pathlib import Path
import json,xml.etree.ElementTree as E,hashlib
r=Path(__file__).resolve().parents[3];out=Path(__file__).resolve().parent
meta=sorted(out.glob('*-check.json'))[-1];run=json.loads(meta.read_text(encoding='utf-8-sig'));prefix=meta.name[:-5]
junit=out/(prefix+'-junit');jacoco=out/(prefix+'-jacoco.xml')
assert junit.is_dir() and jacoco.is_file(),'Sin resultados frescos del check final'
roots=[(p,E.parse(p).getroot()) for p in junit.glob('TEST-*.xml')]
counts={k:sum(int(x.attrib.get(k,0)) for _,x in roots) for k in ('tests','failures','errors','skipped')}
report=E.parse(jacoco).getroot()
def counters(node):
    return {c.attrib['type']:{'covered':int(c.attrib['covered']),'missed':int(c.attrib['missed']),'pct':100*int(c.attrib['covered'])/(int(c.attrib['covered'])+int(c.attrib['missed'])) if int(c.attrib['covered'])+int(c.attrib['missed']) else 100} for c in node.findall('counter')}
modules={}
for p in report.findall('package'):
    module=p.attrib['name'].split('/')[3] if len(p.attrib['name'].split('/'))>3 else p.attrib['name']
    for metric,c in counters(p).items():
        t=modules.setdefault(module,{}).setdefault(metric,{'covered':0,'missed':0});t['covered']+=c['covered'];t['missed']+=c['missed']
for m in modules.values():
    for c in m.values():c['pct']=100*c['covered']/(c['covered']+c['missed']) if c['covered']+c['missed'] else 100
before=json.loads((out/'protegidos-iniciales.json').read_text());changed=[p for p,h in before.items() if not (r/p).is_file() or hashlib.sha256((r/p).read_bytes()).hexdigest()!=h]
current={p.relative_to(r).as_posix() for root in ('backend/src/main','frontend/src') for p in (r/root).rglob('*') if p.is_file()}
initial={p for p in before if p.startswith(('backend/src/main/','frontend/src/'))}
changed+=['NUEVO:'+p for p in sorted(current-initial)]
f5=[{'suite':p.name,'tests':int(x.attrib['tests']),'failures':int(x.attrib.get('failures',0)),'errors':int(x.attrib.get('errors',0))} for p,x in roots if 'Fase5' in p.name]
low=[]
for p in report.findall('package'):
    for s in p.findall('sourcefile'):
        cs=counters(s)
        if cs.get('LINE',{}).get('pct',100)<80:low.append({'archivo':p.attrib['name']+'/'+s.attrib['name'],'lineas':cs['LINE']})
result={'comando':run,'junit':counts,'suites_fase5':f5,'global':counters(report),'modulos':modules,'bajo_80_lineas':low,'conservacion':{'protegidos':len(before),'alterados':changed},'umbral_backend_80':counters(report)['LINE']['pct']>=80,'Google_real':False,'deteccion_automatica':False}
(out/'final-metricas.json').write_bytes((json.dumps(result,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
print(json.dumps({'junit':counts,'fase5':sum(x['tests'] for x in f5),'global_lineas':result['global']['LINE'],'global_ramas':result['global']['BRANCH'],'plazos':modules.get('plazos'),'conservacion':result['conservacion']},ensure_ascii=True))

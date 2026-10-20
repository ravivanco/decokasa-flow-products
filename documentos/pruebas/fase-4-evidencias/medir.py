"""Mide XML reales de JaCoCo y JUnit; no excluye clases ni altera instrumentación."""
from pathlib import Path
from datetime import datetime
import hashlib,json,shutil,sys
import xml.etree.ElementTree as E
sys.stdout.reconfigure(encoding='utf-8')
dest=Path(__file__).resolve().parent
repo=dest.parents[2]
root=E.parse(repo/'backend/build/reports/jacoco/test/jacocoTestReport.xml').getroot()
def cont(e,t):
    c=e.find(f"counter[@type='{t}']")
    return (int(c.get('covered')),int(c.get('missed'))) if c is not None else (0,0)
def met(c,m):return dict(cubiertas=c,no_cubiertas=m,total=c+m,porcentaje=round(100*c/(c+m),4) if c+m else None)
def modulo(nombre,tipo):
    prefijo='com/decokasa/seguimiento/'+nombre
    cs=[cont(p,tipo) for p in root.findall('package') if p.get('name')==prefijo or p.get('name').startswith(prefijo+'/')]
    return met(sum(c for c,m in cs),sum(m for c,m in cs))
tests=[];fallos=[]
for p in (repo/'backend/build/test-results/test').glob('TEST-*.xml'):
    t=E.parse(p).getroot()
    tests.append(dict(clase=t.get('name'),casos=int(t.get('tests')),fallos=int(t.get('failures')),errores=int(t.get('errors','0')),omitidas=int(t.get('skipped','0'))))
    for caso in t.findall('testcase'):
        for f in caso.findall('failure'):
            fallos.append(dict(clase=t.get('name'),caso=caso.get('name'),mensaje=f.get('message'),traza=f.text))
snapshot={p.replace('\\','/'):h for p,h in json.loads((dest/'originales-sha256.json').read_text(encoding='utf-8')).items()}
cambiados=[p for p,h in snapshot.items() if not (repo/p).is_file() or hashlib.sha256((repo/p).read_bytes()).hexdigest()!=h]
actual={p.relative_to(repo).as_posix() for p in (repo/'backend/src/main').rglob('*') if p.is_file()}
previo={p for p in snapshot if p.startswith('backend/src/main/')}
gaps=[]
for p in root.findall('package'):
    for f in p.findall('sourcefile'):
        if p.get('name').startswith(('com/decokasa/seguimiento/bodegas','com/decokasa/seguimiento/pedidos','com/decokasa/seguimiento/flujos','com/decokasa/seguimiento/historial')):
            v=met(*cont(f,'LINE'))
            if v['porcentaje'] is not None and v['porcentaje']<80:gaps.append(dict(archivo=p.get('name')+'/'+f.get('name'),**v))
r=dict(fecha=datetime.now().isoformat(),global_lineas=met(*cont(root,'LINE')),global_ramas=met(*cont(root,'BRANCH')),
       modulos={n:dict(lineas=modulo(n,'LINE'),ramas=modulo(n,'BRANCH')) for n in ['bodegas','pedidos','flujos','historial','archivos']},
       totales={k:sum(t[k] for t in tests) for k in ['casos','fallos','errores','omitidas']},suites=tests,fallos=fallos,
       protegidos=len(snapshot),protegidos_modificados=cambiados,produccion_anadida=sorted(actual-previo),produccion_eliminada=sorted(previo-actual),
       fuentes_bajo_80=sorted(gaps,key=lambda x:x['no_cubiertas'],reverse=True),exclusiones_nuevas=False,umbral_modificado=False,
       limitaciones=['Check no aprobado mientras exista un fallo; cobertura no equivale a pruebas aprobadas.','Sin recorrido completo mas alla de A6; fixtures aislados para A7/A8/A9.','Espacio fisico temporal fuera de la cuota logica; Google 3.B3 pospuesto.'])
(dest/'final-metricas.json').write_text(json.dumps(r,indent=2,ensure_ascii=False),encoding='utf-8')
shutil.copyfile(repo/'backend/build/reports/jacoco/test/jacocoTestReport.xml',dest/'final-jacoco.xml')
target=dest/'final-junit';target.mkdir(exist_ok=True)
for p in (repo/'backend/build/test-results/test').glob('TEST-*.xml'):shutil.copyfile(p,target/p.name)
print(json.dumps({k:r[k] for k in ['global_lineas','global_ramas','modulos','totales','protegidos_modificados','produccion_anadida','produccion_eliminada']},indent=2,ensure_ascii=False))

"""Read JaCoCo/JUnit only; preserve raw evidence, no instrumentation/exclusions."""
from pathlib import Path
from datetime import datetime
import hashlib, json, shutil, sys
import xml.etree.ElementTree as E

repo=Path(__file__).resolve().parents[4]
dest=Path(__file__).resolve().parent
label=sys.argv[1] if len(sys.argv)>1 else 'final'
source=repo/'backend/build/reports/jacoco/test/jacocoTestReport.xml'
root=E.parse(source).getroot()

def counter(element,kind):
    c=next((x for x in element.findall('counter') if x.get('type')==kind),None)
    return (int(c.get('covered')),int(c.get('missed'))) if c is not None else (0,0)

def metrics(covered,missed):
    return {'cubiertas':covered,'no_cubiertas':missed,'total':covered+missed,
            'porcentaje':round(100*covered/(covered+missed),4) if covered+missed else None}

globales=metrics(*counter(root,'LINE'))
packages=[p for p in root.findall('package') if p.get('name')=='com/decokasa/seguimiento/archivos'
          or p.get('name').startswith('com/decokasa/seguimiento/archivos/')]
c=sum(counter(p,'LINE')[0] for p in packages);m=sum(counter(p,'LINE')[1] for p in packages)
archivos=metrics(c,m)
bridge=[p for p in root.findall('package') if p.get('name')=='com/decokasa/seguimiento/pedidos/adapter/in/archivos']
compatibilidad=metrics(c+sum(counter(p,'LINE')[0] for p in bridge),m+sum(counter(p,'LINE')[1] for p in bridge))
gaps=[]
for p in packages:
    for f in p.findall('sourcefile'):
        v=metrics(*counter(f,'LINE'));v['archivo']=p.get('name')+'/'+f.get('name')
        if v['porcentaje'] is not None and v['porcentaje']<80:gaps.append(v)
tests=[]
for file in (repo/'backend/build/test-results/test').glob('TEST-*.xml'):
    t=E.parse(file).getroot()
    tests.append({'clase':t.get('name'),'casos':int(t.get('tests')),'fallos':int(t.get('failures')),
                  'errores':int(t.get('errors','0')),'omitidas':int(t.get('skipped','0'))})
baseline=json.loads((dest/'produccion-antes.json').read_text(encoding='utf-8'))
changed=[p for p,h in baseline.items() if hashlib.sha256((repo/p).read_bytes()).hexdigest()!=h]
result={'marca':datetime.now().isoformat(),'global_lineas':globales,'archivos_lineas':archivos,
        'archivos_mas_adaptador_de_pedidos_lineas':compatibilidad,
        'objetivo_lineas_80':{'global':globales['porcentaje']>=80,'archivos':archivos['porcentaje']>=80},
        'global_ramas':metrics(*counter(root,'BRANCH')),
        'archivos_ramas':metrics(sum(counter(p,'BRANCH')[0] for p in packages),sum(counter(p,'BRANCH')[1] for p in packages)),
        'clases_bajo_80':sorted(gaps,key=lambda x:x['no_cubiertas'],reverse=True),
        'pruebas':tests,'totales':{k:sum(t[k] for t in tests) for k in ['casos','fallos','errores','omitidas']},
        'produccion_o_protegidos_modificados':changed,'archivos_protegidos':len(baseline),
        'exclusiones_nuevas':False,'umbral_modificado':False,
        'limitacion':'Cuota logica: temporales fisicos no limitados/inventariados por ella; 3.B3 real pospuesto'}
(dest/(label+'-metricas.json')).write_text(json.dumps(result,indent=2),encoding='utf-8')
shutil.copyfile(source,dest/(label+'-jacoco.xml'))
target=dest/(label+'-junit');target.mkdir(exist_ok=True)
for file in (repo/'backend/build/test-results/test').glob('TEST-*.xml'):shutil.copyfile(file,target/file.name)
print(json.dumps({k:result[k] for k in ['global_lineas','archivos_lineas','totales','produccion_o_protegidos_modificados']},indent=2))

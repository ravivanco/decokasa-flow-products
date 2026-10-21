"""Resume evidencia existente; no ejecuta suites, modifica producción ni usa Git de escritura."""
from pathlib import Path
import json,hashlib,xml.etree.ElementTree as E,subprocess,re
base=Path(__file__).resolve().parent
root=base.parents[3]
def junit(folder):
    totals={x:0 for x in ('tests','failures','errors','skipped')}; failures=[]; suites=[]
    for f in sorted((base/folder).glob('TEST-*.xml')):
        r=E.parse(f).getroot()
        for k in totals:totals[k]+=int(r.get(k,0))
        suites.append({'clase':r.get('name'),'tests':int(r.get('tests',0)),'failures':int(r.get('failures',0)),'errors':int(r.get('errors',0))})
        for c in r.findall('testcase'):
            for kind in ('failure','error'):
                n=c.find(kind)
                if n is not None:failures.append({'clase':r.get('name'),'caso':c.get('name'),'tipo':kind,'mensaje':n.get('message','')})
    return {'totales':totals,'suites':suites,'fallos':failures}
data={folder:junit(folder) for folder in ['baseline-junit','dirigido-01-junit','dirigido-02-junit','final-junit']}
if (base/'final-jacoco.xml').exists():
    r=E.parse(base/'final-jacoco.xml').getroot()
    def counts(elements):
        out={}
        for el in elements:
            for c in el.findall('counter'):
                v=out.setdefault(c.get('type'),{'covered':0,'missed':0})
                for k in ('covered','missed'):v[k]+=int(c.get(k))
        for v in out.values():v['total']=v['covered']+v['missed'];v['pct']=round(v['covered']*100/v['total'],2) if v['total'] else 100
        return out
    data['coberturaGlobal']=counts([r])
    data['coberturaModulos']={name:counts([p for p in r.findall('package') if p.get('name').startswith('com/decokasa/seguimiento/'+name+'/') or p.get('name')=='com/decokasa/seguimiento/'+name]) for name in ['pedidos','flujos','archivos','plazos']}
initial=json.loads((base/'conservacion-inicial.json').read_text(encoding='utf-8-sig'))
changed=[]
for rel,digest in initial.items():
    f=root/rel
    if not f.exists() or hashlib.sha256(f.read_bytes()).hexdigest().lower()!=digest.lower():changed.append(rel)
data['conservacion']={'cambios':changed,'produccionIntacta':not any(x.startswith('backend/src/main/') or x.startswith('frontend/src/') for x in changed),'headerIntacto':'frontend/src/components/Header.tsx' not in changed,'indiceIntacto':'.git/index' not in changed}
data['rama']=subprocess.run(['git','-C',str(root),'branch','--show-current'],capture_output=True,text=True).stdout.strip()
data['googleReal']=False;data['alcance']='Solo 6.B1; sin frontend ni recorrido de navegador 6.B2';data['fases']='Abiertas';data['3.B3']='Pospuesto'
patterns=re.compile(r'ya29\.[A-Za-z0-9_-]{15,}|1//[A-Za-z0-9_-]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}')
data['archivosConPatronesSecretos']=[str(f.relative_to(base)) for f in base.rglob('*') if f.is_file() and f.suffix in ('.xml','.json','.log') and patterns.search(f.read_text(encoding='utf-8-sig',errors='replace'))]
(base/'resultados.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:({'totales':v['totales']} if isinstance(v,dict) and 'totales' in v else v) for k,v in data.items() if k!='coberturaModulos'},ensure_ascii=False,indent=2))

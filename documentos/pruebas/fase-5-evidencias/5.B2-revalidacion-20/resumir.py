from pathlib import Path
import json,hashlib,socket,re,shutil,xml.etree.ElementTree as E,math
b=Path(__file__).parent;r=b.parents[3]
def leer(n):return json.loads((b/n).read_text(encoding='utf-8-sig'))
def pct(xs,p):return sorted(xs)[max(0,math.ceil(len(xs)*p)-1)]
datos=leer('cls-carga.json')['muestras'];grupos=[]
for width in (1440,390):
 for cache in ('fria','caliente'):
  filas=[f for f in datos if f['viewport']['width']==width and f['cache']==cache];xs=[f['cls'] for f in filas]
  grupos.append({'width':width,'cache':cache,'n':len(xs),'min':min(xs),'p75':pct(xs,.75),'max':max(xs),'transferidosStaticos':[sum(x['transferSize'] for x in f['recursos'] if x['ruta'].startswith('/assets/')) for f in filas]})
copias=[]
for p in (r/'backend/build/test-results/test').glob('TEST-*.xml'):
 if any(x in p.name for x in ('ConsultasTest','UsuarioJpaRepositorioIT','ReglasArquitecturaTest','FronterasFaseDosTest')):
  dest=b/'junit-backend'/p.name;dest.parent.mkdir(exist_ok=True);shutil.copyfile(p,dest);copias.append(dest)
copias+=list((b/'junit-resumenes').glob('TEST-*.xml'))
junit=[]
for p in copias:
 a=E.parse(p).getroot().attrib;junit.append({'archivo':str(p.relative_to(b)),'tests':int(a['tests']),'fallos':int(a.get('failures',0)),'errores':int(a.get('errors',0)),'omitidos':int(a.get('skipped',0))})
original=leer('protegidos-iniciales.json');cambios=[]
for nombre,h in original.items():
 p=r/nombre
 if not p.exists() or hashlib.sha256(p.read_bytes()).hexdigest()!=h:cambios.append(nombre)
permitidos=['frontend/src/components/dashboards/DashboardRol.test.tsx','frontend/src/components/pedidos/Fase4Pantallas.test.tsx']
otros=[p for p in cambios if p.replace('\\','/') not in permitidos]
puertos={}
for port in (43184,43185,43186):
 with socket.socket() as s:s.settimeout(.5);puertos[str(port)]=s.connect_ex(('127.0.0.1',port))!=0
secretos=[]
patron=re.compile(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|GOCSPX-[A-Za-z0-9_-]+|ya29\.[A-Za-z0-9_-]+|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----')
for p in b.rglob('*'):
 if p.is_file() and p.suffix in ('.xml','.json','.log','.txt','.md','.java','.mjs'):
  raw=p.read_bytes();s=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig',errors='replace')
  if patron.search(s):secretos.append(str(p.relative_to(b)))
m={'cls':grupos,'polling':leer('cls-polling.json'),'sql':[(x['usuarios'],x['tamano'],x['count']) for x in leer('sql-revalidacion.json')['muestras']],'junit':junit,'backendTests':sum(x['tests'] for x in junit),'protegidos':len(original),'cambiosPermitidos':cambios,'otrosCambios':otros,'puertosCerrados':puertos,'credencialTemporalRetirada':not (b/'.tmp/acceso.json').exists(),'secretos':secretos}
(b/'resumen-final.json').write_text(json.dumps(m,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps({k:v for k,v in m.items() if k!='polling'},ensure_ascii=False))

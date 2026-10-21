from pathlib import Path
import json,socket,hashlib,gzip,re,subprocess
base=Path(__file__).parent;repo=base.parents[3]
puertos={}
for port in (43184,43185,43186):
    with socket.socket() as s:
        s.settimeout(.5);puertos[str(port)]=s.connect_ex(('127.0.0.1',port))!=0
protegidos=json.loads((base.parent/'protegidos-iniciales.json').read_text(encoding='utf-8-sig'))
cambios=[p for p,h in protegidos.items() if not (repo/p).is_file() or hashlib.sha256((repo/p).read_bytes()).hexdigest()!=h]
encontrados=[]
patron=re.compile(rb'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|ya29\.[A-Za-z0-9_-]+|GOCSPX-[A-Za-z0-9_-]+')
for p in base.rglob('*'):
    if not p.is_file():continue
    if p.suffix in ('.json','.log','.txt','.md','.mjs','.java','.sql','.gradle'):
        raw=p.read_bytes();texto=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig',errors='replace')
        if patron.search(texto.encode('utf-8')):encontrados.append(str(p.relative_to(base)))
    if p.name.endswith('.json.gz') and patron.search(gzip.decompress(p.read_bytes())):encontrados.append(str(p.relative_to(base)))
datos={'puertosCerrados':puertos,'accesoTemporalRetirado':not (base/'.tmp/acceso.json').exists(),'protegidos':len(protegidos),'cambiosProtegidos':cambios,'hallazgosSecretos':encontrados,'rama':subprocess.check_output(['git','branch','--show-current'],cwd=repo,text=True).strip(),'contenedores':'docker inspect después de salir: ausentes por limpieza automática de Testcontainers; no se ejecutó docker rm ni borrado de volúmenes manual','google':False,'automaticos':False}
(base/'conservacion-final.json').write_text(json.dumps(datos,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps(datos,ensure_ascii=False))

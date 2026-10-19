from pathlib import Path
import subprocess,json,hashlib,re
root=Path(__file__).resolve().parents[4]
out=Path(__file__).resolve().parent

def git(*a):return subprocess.check_output(['git','-C',str(root),*a])
branch=git('branch','--show-current').decode().strip()
paths=[p.decode('utf-8') for p in git('ls-files','-co','--exclude-standard','-z').split(b'\0') if p]
# Excluye solo la carpeta de evidencias que este mismo script esta escribiendo.
manifest={}
for name in paths:
 p=root/name
 if p.is_file() and '/preparacion-B3/' not in name and name!='documentos/pruebas/fase-3-informe.md':
  manifest[name]=hashlib.sha256(p.read_bytes()).hexdigest()
(out/'conservacion-inicial.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
(out/'indice-inicial.bin').write_bytes(git('ls-files','--stage','-z'))
env={}
local=root/'backend/.env.local'
if local.exists():
 for line in local.read_text(encoding='utf-8-sig').splitlines():
  if '=' in line and not line.lstrip().startswith('#'):
   k,v=line.split('=',1);env[k.strip()]=v.strip()
flags=['GOOGLE_OAUTH_HABILITADA','GOOGLE_DRIVE_HABILITADA','GOOGLE_GMAIL_HABILITADA','COPIA_DRIVE_AUTOMATICA','CORREO_AUTOMATICO']
config={'flagsLocal':{k:env.get(k,'AUSENTE: default false') for k in flags},
 'flagsProceso':{k:__import__('os').environ.get(k,'AUSENTE') for k in flags},
 'cuentaCoincideConAprobada':env.get('GOOGLE_CUENTA_EMAIL')=='importadora.latinoamericaworld@gmail.com'}
private={}
for k in ['GOOGLE_OAUTH_CLIENTE_RUTA','GOOGLE_OAUTH_LLAVE_RUTA','GOOGLE_OAUTH_TOKENS_RUTA']:
 raw=env.get(k,'');p=Path(raw) if raw else None
 private[k]={'configurada':bool(raw),'absoluta':p.is_absolute() if p else False,'fueraRepo':not p.is_relative_to(root) if p and p.is_absolute() else None,'existe':p.exists() if p else False,'enlace':p.is_symlink() if p else False}
config['rutas']=private
actual_secret=None
raw=env.get('GOOGLE_OAUTH_CLIENTE_RUTA','')
client_check={'leido':False}
if raw and Path(raw).is_file():
 try:
  client=json.loads(Path(raw).read_text(encoding='utf-8-sig'));web=client.get('web',{})
  actual_secret=web.get('client_secret','').encode() or None
  uri='http://localhost:8080/api/v1/integraciones/google/oauth2/callback'
  client_check={'leido':True,'tipoWeb':bool(web),'redirectUrisPresente':'redirect_uris' in web,'callbackValidoSiPresente':'redirect_uris' not in web or (isinstance(web['redirect_uris'],list) and uri in web['redirect_uris'])}
 except Exception: client_check={'leido':False,'error':'JSON_NO_REVISABLE_SIN_MOSTRAR_DETALLES'}
config['cliente']=client_check
suspicious=re.compile(r'(?i)(client_secret[^/]*\.json|oauth-tokens[^/]*|google-aes[^/]*\.key|(^|/)\.env(\.local)?$)')
tracked=[p.decode() for p in git('ls-files','-z').split(b'\0') if p]
staged=[p.decode() for p in git('diff','--cached','--name-only','-z').split(b'\0') if p]
findings=[]
patterns={'google_client_secret':rb'GOCSPX-[A-Za-z0-9_-]{15,}','clave_privada':rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----','google_access_token':rb'ya29\.[A-Za-z0-9_-]{30,}'}
def scan(data,where):
 for label,pattern in patterns.items():
  if re.search(pattern,data):findings.append({'ubicacion':where,'patron':label})
 if actual_secret and actual_secret in data:findings.append({'ubicacion':where,'patron':'valor_exacto_secreto_cliente_configurado'})
for name in paths:
 p=root/name
 if p.is_file() and '/preparacion-B3/' not in name and p.suffix not in ['.png','.zip','.docx','.pdf']:
  scan(p.read_bytes(),name)
for name in staged:
 try:scan(git('show',':'+name),'INDICE:'+name)
 except subprocess.CalledProcessError:pass
objects={line.split(b' ',1)[0].decode():line.split(b' ',1)[1].decode('utf-8','replace') if b' ' in line else '' for line in git('rev-list','--objects','--all').splitlines()}
proc=subprocess.Popen(['git','-C',str(root),'cat-file','--batch'],stdin=subprocess.PIPE,stdout=subprocess.PIPE)
blobs=0
for oid,name in objects.items():
 proc.stdin.write((oid+'\n').encode());proc.stdin.flush();header=proc.stdout.readline().split()
 if len(header)!=3:continue
 size=int(header[2]);data=proc.stdout.read(size);proc.stdout.read(1)
 if header[1]==b'blob':blobs+=1;scan(data,'HISTORIA:'+oid[:12]+':'+name)
proc.stdin.close();proc.wait()
result={'rama':branch,'configuracion':config,'nombresSensiblesRastreados':[p for p in tracked if suspicious.search(p)],'nombresSensiblesPreparados':[p for p in staged if suspicious.search(p)],'archivosActualesRevisados':len(paths),'blobsHistoricosRevisados':blobs,'coincidenciasSinContenido':findings,'alcanceSecretos':'patrones Google/llave privada y valor exacto del client_secret configurado; no prueba ausencia universal de secretos'}
(out/'auditoria-local.json').write_text(json.dumps(result,indent=2,ensure_ascii=True),encoding='utf-8')
print(json.dumps(result,indent=2,ensure_ascii=True))

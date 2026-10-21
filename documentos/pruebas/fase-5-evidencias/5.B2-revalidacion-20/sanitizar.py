from pathlib import Path
import re,json
base=Path(__file__).parent
modificados=0
for p in base.rglob('*'):
    if p.is_file() and p.suffix in ('.json','.log','.md','.txt') and p.name!='acceso.json':
        raw=p.read_bytes()
        s=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig',errors='replace')
        nuevo=re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[TOKEN_FICTICIO_RETIRADO]',s)
        if nuevo!=s:
            p.write_text(nuevo,encoding='utf-8');modificados+=1
(base/'saneamiento.json').write_text(json.dumps({'archivosSanitizados':modificados,'alcance':'Solo artefactos nuevos 5.B2; JWT ficticios retirados; fuentes/evidencias anteriores intactas'}),encoding='utf-8')
print('Saneamiento de tokens ficticios aplicado a',modificados,'archivos; no se imprime su contenido.')

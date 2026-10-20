from pathlib import Path
import json,re,hashlib
r=Path(__file__).resolve().parents[4]; src=r/'frontend/src'; out=r/'documentos/pruebas/fase-4-evidencias/cobertura-frontend'
summary=sorted(out.glob('*-coverage-summary.json'))
if not summary: summary=sorted(out.glob('*-baseline-summary.json'))
data=json.loads(summary[-1].read_text(encoding='utf-8-sig'))
def rel(p):return Path(p).relative_to(src).as_posix()
def resolve(p,s):
    root=src/s[2:] if s.startswith('@/') else p.parent/s
    return next((v for v in [root,Path(str(root)+'.tsx'),Path(str(root)+'.ts'),root/'index.tsx',root/'index.ts'] if v.is_file()),None)
seen=set();stack=[src/'main.tsx']
while stack:
    p=stack.pop()
    if p in seen:continue
    seen.add(p)
    text=p.read_text(encoding='utf-8-sig')
    for s in re.findall(r'(?:from\s*|import\s*\(\s*|import\s*)[\x27\x22]([^\x27\x22]+)[\x27\x22]',text):
        if s.startswith(('@/','.')):
            q=resolve(p,s)
            if q and q.suffix in ('.ts','.tsx'):stack.append(q)
scopes={'fase4':lambda p:p.name in ['CatalogoBodegas.tsx','BandejaPedidos.tsx','DetallePedido.tsx','DialogoAccion.tsx','PanelEtapa.tsx','HistorialPedido.tsx','formato.ts','bodegas.ts'],
        'alcanzable_desde_main':lambda p:p in seen}
totals={}
for scope,predicate in scopes.items():
    files={rel(k):v for k,v in data.items() if k!='total' and predicate(Path(k))}
    totals[scope]={'archivos':files,'total':{}}
    for metric in ('lines','branches','functions','statements'):
        total=sum(v[metric]['total'] for v in files.values());covered=sum(v[metric]['covered'] for v in files.values())
        totals[scope]['total'][metric]={'total':total,'covered':covered,'pct':100*covered/total if total else 100}
unreachable={rel(k):v['lines'] for k,v in data.items() if k!='total' and Path(k) not in seen and not rel(k).startswith('test/') and v['lines']['total']>0}
totals.update({'global':data['total'],'no_alcanzable_desde_main':unreachable,'nota':'Análisis estático de imports TS/TSX; no altera el denominador ni las exclusiones. Incluye imports de tipos; no prueba eliminación por bundler.','resumen_origen':summary[-1].name})
totals['techo_global_sin_cubrir_no_alcanzable']=100*(data['total']['lines']['total']-sum(v['total'] for v in unreachable.values()))/data['total']['lines']['total']
before=json.loads((out/'protegidos-iniciales.json').read_text())
changed=[p for p,h in before.items() if not (r/p).is_file() or hashlib.sha256((r/p).read_bytes()).hexdigest()!=h]
totals['conservacion']={'protegidos':len(before),'alterados':changed}
raw=Path(str(summary[-1]).replace('-summary.json','-contadores.json'))
if raw.exists():
    counters=json.loads(raw.read_text(encoding='utf-8-sig'))
    totals['ramas_sin_cubrir_fase4']={rel(p):[{'linea':v['branchMap'][k]['line'],'contadores':ns} for k,ns in v['b'].items() if 0 in ns] for p,v in counters.items() if scopes['fase4'](Path(p))}
(out/'pruebas-finales-sha256.json').write_text(json.dumps({p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (src/'components/pedidos').glob('*.test.*') if p.name in ('Fase4Ramas.test.tsx','formato.test.ts')},indent=2),encoding='utf-8')
(out/'analisis-cobertura.json').write_text(json.dumps(totals,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in totals.items() if k not in ['no_alcanzable_desde_main','alcanzable_desde_main','fase4']},ensure_ascii=True,indent=2))
for scope in scopes:print(scope,totals[scope]['total'])
print('No alcanzables:',{k:v['total'] for k,v in unreachable.items()})

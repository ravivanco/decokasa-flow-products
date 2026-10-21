from pathlib import Path
import json,hashlib,statistics,re,uuid
base=Path(__file__).parent
repo=base.parents[3]
def leer(n):return json.loads((base/n).read_text(encoding='utf-8-sig'))
def resumen(v):
    v=sorted(v)
    return {'n':len(v),'min':v[0],'mediana':statistics.median(v),'p75':v[max(0,__import__('math').ceil(len(v)*.75)-1)],'p95':v[max(0,__import__('math').ceil(len(v)*.95)-1)],'p99':v[max(0,__import__('math').ceil(len(v)*.99)-1)],'max':v[-1]}
api={k:resumen(v['ms'])|{'bytes':v['bytes'],'concurrencia10ms':v['loteConcurrenteMs']} for k,v in leer('api-metricas.json').items()}
pant=leer('pantalla-metricas.json');pantalla={}
for cache in ('fria','caliente','caliente-query'):
    filas=[x for x in pant['mediciones'] if x['cache']==cache]
    pantalla[cache]={k:resumen([x[k] for x in filas]) for k in ('dashboardMs','busquedaMs')}
    pantalla[cache]['solicitudesDashboard']=[len(x['solicitudesDashboard']) for x in filas]
    pantalla[cache]['lcpLabMs']=[x['perf']['vitals']['lcp'] for x in filas]
    pantalla[cache]['clsLab']=[x['perf']['vitals']['cls'] for x in filas]
    pantalla[cache]['eventTimingMaxMs']=[max([e['duration'] for e in x['eventos']],default=None) for x in filas]
consultas=leer('consultas.json')
protegidos=json.loads((base.parent/'protegidos-iniciales.json').read_text(encoding='utf-8-sig'))
cambios=[str(p) for p,h in protegidos.items() if not (repo/p).is_file() or hashlib.sha256((repo/p).read_bytes()).hexdigest()!=h]
metricas={'api':api,'pantalla':pantalla,'sqlPorPagina':{k:v['count'] for k,v in consultas.items()},'protegidos':len(protegidos),'cambiosProtegidos':cambios,'edge':pant['browserVersion']}
(base/'metricas-resumen.json').write_text(json.dumps(metricas,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps(metricas,indent=2,ensure_ascii=False))
# SQL observado por StatementInspector, con valores ficticios explícitos (no credenciales).
sqls=consultas['20']['sql'];bandeja=next(q for q in sqls if q.startswith('select cast(p.id as text)'))
admin=str(uuid.UUID(bytes=hashlib.md5(b'admin').digest(),version=3))
params=['true',"'admin'",f"'{admin}'::uuid",'20','0']
for v in params:bandeja=bandeja.replace('?',v,1)
logistica=str(uuid.UUID(bytes=hashlib.md5(b'logistica').digest(),version=3))
queries=[bandeja,bandeja.replace('cast(true as boolean)','cast(false as boolean)').replace("'admin'","'logistica'").replace(admin,logistica)]
queries += ["select id from pedido where numero='DK-EC-2026-0008'","select id from pedido where lower(numero) like '%ficticio%' escape '!' or lower(nombre) like '%ficticio%' escape '!' order by creado_en desc,id limit 20"]
(base/'planes-exactos.sql').write_text('\n'.join('explain (analyze,buffers,format json) '+q+';' for q in queries),encoding='utf-8')

"""Resultados ejecutados y conservación; no interpreta tests no ejecutados como aprobados."""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

base = Path(__file__).resolve().parent
suite_paths = sorted((base / 'final-junit').glob('TEST-*.xml'))
suites = []
for path in suite_paths:
    root = ET.parse(path).getroot()
    suites.append({
        'suite': root.attrib['name'],
        **{key: int(root.attrib.get(key, '0')) for key in ('tests', 'failures', 'errors', 'skipped')},
        'fallos': [{'caso': case.attrib['name'], 'mensaje': case.find('failure').attrib.get('message', '')}
                   for case in root.findall('testcase') if case.find('failure') is not None],
    })
antes = json.loads((base / 'conservacion-antes.json').read_text(encoding='utf-8-sig'))
conservacion = []
for entry in antes:
    actual = hashlib.sha256(Path(entry['ruta']).read_bytes()).hexdigest().upper()
    conservacion.append({**entry, 'despues': actual, 'igual': actual == entry['sha256']})
salida = {'alcance': 'Diagnostico con asistente excluido; no es compilacion/check global.',
          'suites': suites,
          'totales': {key: sum(s[key] for s in suites) for key in ('tests', 'failures', 'errors', 'skipped')},
          'conservacion': conservacion}
(base / 'resultados.json').write_text(json.dumps(salida, indent=2, ensure_ascii=False), encoding='utf-8')
print(json.dumps(salida['totales']))
print('Protegidos sin cambios:', sum(e['igual'] for e in conservacion), '/', len(conservacion))

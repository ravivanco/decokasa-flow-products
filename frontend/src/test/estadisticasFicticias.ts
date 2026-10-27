import type { Estadisticas } from '@/api/estadisticas';

/** Respuesta vacía explícita de ADR-0029, para pruebas de otras superficies. */
export function estadisticasVacias(anio = new Date().getFullYear()): Estadisticas {
  return {
    periodo: { anio, desde: `${anio}-01-01`, hasta: `${anio}-12-31` },
    paises: ['EC'],
    tarjetas: { creados: 0, enCurso: 0, finalizados: 0, conRetraso: 0,
      medianaResolucionSegundos: null, resolucionObservaciones: 0 },
    porAnio: [], porMes: [], tiempoPorEtapa: [], tiempoPorPedido: [], tiempoPorPedidoTotal: 0,
    estado: { enCurso: 0, finalizados: 0, enCursoConRetraso: 0, enCursoConUrgencia: 0, enCursoConDemora: 0 },
    retrasosPorEtapa: [], calculadoEn: '2026-10-26T17:00:00Z',
  };
}

import base from './playwright.config.mjs';
export default {...base,testMatch:'accesibilidad.spec.mjs',timeout:60000,outputDir:'./.tmp/a11y',reporter:[['list'],['json',{outputFile:'./a11y-resultados.json'}]]};

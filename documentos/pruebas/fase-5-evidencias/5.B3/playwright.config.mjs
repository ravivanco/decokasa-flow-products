import {defineConfig} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
export default defineConfig({testDir:'.',testMatch:'accesibilidad.spec.mjs',workers:1,retries:0,timeout:120000,
 outputDir:'./.tmp/artifacts',reporter:[['list'],['json',{outputFile:'./resultados.json'}]],
 use:{baseURL:'http://127.0.0.1:43184',channel:'msedge',headless:true,viewport:{width:1440,height:1000},trace:'off',screenshot:'off'}});

import {defineConfig} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
export default defineConfig({testDir:'.',testMatch:'criticos.spec.mjs',workers:1,retries:0,timeout:240000,
 outputDir:'./.tmp/browser',reporter:[['list'],['json',{outputFile:'./browser-resultados.json'}]],
 use:{baseURL:'http://127.0.0.1:44284',channel:'msedge',headless:true,viewport:{width:1440,height:1000},trace:'off',screenshot:'off',actionTimeout:12000}});

import { defineConfig } from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
export default defineConfig({testDir:'.',testMatch:'pantallas.spec.mjs',workers:1,retries:0,timeout:30000,
 outputDir:'./browser-artifacts', reporter:[['list'],['json',{outputFile:'playwright.json'}]],
 use:{baseURL:'http://127.0.0.1:43182',headless:true,channel:'msedge',trace:'on',screenshot:'only-on-failure'},
 webServer:{command:'node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 43182 --strictPort --mode b2',
 cwd:'C:/Users/Richard/Desktop/decoWeb/frontend',url:'http://127.0.0.1:43182',reuseExistingServer:false,
 env:{VITE_ACCESO_LOCAL:'false',VITE_API_URL:'/api/v1'}}});

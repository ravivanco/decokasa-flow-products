import { defineConfig } from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
export default defineConfig({testDir:'.',testMatch:'pantallas.spec.mjs',workers:1,retries:0,timeout:30000,
 outputDir:process.env.F4_OUTPUT_DIR || './browser-artifacts',reporter:[['list'],['json',{outputFile:process.env.F4_REPORT || 'playwright.json'}]],
 use:{baseURL:'http://127.0.0.1:43184',headless:true,channel:'msedge',trace:'retain-on-failure',screenshot:'only-on-failure'},
 webServer:{command:'node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 43184 --strictPort --mode f4b',
 cwd:'C:/Users/Richard/Desktop/decoWeb/frontend',url:'http://127.0.0.1:43184',reuseExistingServer:false,
 env:{VITE_ACCESO_LOCAL:'false',VITE_API_URL:'/api/v1',GOOGLE_OAUTH_HABILITADA:'false',COPIA_DRIVE_AUTOMATICA:'false',CORREO_AUTOMATICO:'false'}}});

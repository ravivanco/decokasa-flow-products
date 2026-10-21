import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import {fileURLToPath,pathToFileURL} from 'node:url';
const base=path.dirname(fileURLToPath(import.meta.url));
const front=path.resolve(base,'../../../../frontend');
process.chdir(front);
process.env.VITE_ACCESO_LOCAL='false';
process.env.VITE_API_URL='/api/v1';
delete process.env.VITE_ACTUALIZACION_MS;
const {build}=await import(pathToFileURL(path.join(front,'node_modules/vite/dist/node/index.js')).href);
const dist=path.join(base,'.tmp/dist');
await build({root:front,mode:'b2',build:{outDir:dist,emptyOutDir:false}});
const server=http.createServer((req,res)=>{
 if(req.url.startsWith('/api/')){
  const upstream=http.request({hostname:'127.0.0.1',port:43185,path:req.url,method:req.method,headers:{...req.headers,host:'127.0.0.1:43185'}},r=>{res.writeHead(r.statusCode,r.headers);r.pipe(res)});
  upstream.on('error',()=>{res.writeHead(502);res.end()});req.pipe(upstream);return;
 }
 let file=path.join(dist,new URL(req.url,'http://localhost').pathname);
 if(!file.startsWith(dist)){res.writeHead(403);res.end();return;}
 if(!fs.existsSync(file)||fs.statSync(file).isDirectory())file=path.join(dist,'index.html');
 const ext=path.extname(file);res.setHeader('Content-Type',({'.html':'text/html','.js':'text/javascript','.css':'text/css','.woff2':'font/woff2','.svg':'image/svg+xml','.png':'image/png'})[ext]||'application/octet-stream');
 res.setHeader('Cache-Control',ext==='.html'?'no-cache':'public, max-age=3600');
 fs.createReadStream(file).pipe(res);
});
server.listen(43184,'127.0.0.1',()=>console.log('FRONT_B2_LISTO'));
const timer=setInterval(()=>{if(fs.existsSync(path.join(base,'.tmp/detener-front'))){clearInterval(timer);server.close()}},500);

import {createRoot} from 'react-dom/client';
// Fuentes servidas por el propio servidor, sin Google Fonts (DESIGN.md 4): Inter para la interfaz, Montserrat para títulos.
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/600.css';
import '@fontsource/montserrat/600.css';
import '@fontsource/montserrat/700.css';
import App from './App.tsx';
import './index.css';

createRoot(document.getElementById('root')!).render(<App />);

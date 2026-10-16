// jsdom no implementa la captura de puntero ni scrollIntoView, que el Select de Radix usa al abrirse. Las pruebas que
// abren un Select importan este archivo; no cambia el comportamiento de la aplicación.
const prototipo = Element.prototype as unknown as Record<string, unknown>;
prototipo.hasPointerCapture ??= () => false;
prototipo.setPointerCapture ??= () => {};
prototipo.releasePointerCapture ??= () => {};
prototipo.scrollIntoView ??= () => {};

export {};

// Motor del diagrama: mide, dispone con ELK y dibuja una vista para una combinacion de capas.
// Corre igual en Node (build.mjs, vista inicial) y en el navegador (vistas que se piden con los toggles).
// eslint-disable-next-line no-unused-vars
function motorDominio(ELK, D) {
const C = {
  paper: "#f5f5f5", paper2: "#ececec", ink: "#2d3142", muted: "#4f5d75",
  soft: "#5a6580", rule: "#bfc0c0", box: "#ffffff",
};
// Un color por paquete (paleta series de diagram-design) y uno por familia de relacion
const PAQUETE = {
  "model.cartelera": "#5e7a9b", "model.salas": "#7c8f6f", "model.funciones": "#9c6b50",
  "model.programaciones": "#b8915a", "model.tiempo": "#6e6479", "model.ventas": "#4f7f86",
  "model.usuarios": "#a3566b", "model.promociones": "#6f6a9c", "model.candy": "#5b6b3a",
  "model.imagenes": "#8f7f40", "model.dinero": "#5f7f6e", "model.rechazos": "#b04a3a",
};
const colorPaquete = (p) => PAQUETE[p] || "#8b8b8b";
const FAMILIA = {
  herencia: { color: "#2d3142", nombre: "implementa / extiende" },
  estructura: { color: "#2e5aa8", nombre: "composición / agregación / asociación" },
  uso: { color: "#8b93a7", nombre: "dependencia" },
};
// Pastillas de la vista: un patron se muestra solo si su capa esta a la vista
const rolesDe = (id, capas) => Object.entries(D.patrones)
  .filter(([, p]) => p.roles[id] && (!p.capa || capas.includes(p.capa)))
  .map(([n, p]) => ({ texto: `${n.toUpperCase()} · ${p.roles[id]}`, color: p.color }));
const anchoPastilla = (t) => Math.ceil(t.length * 6.1) + 14;
const ROLES_GOF = new Set(["Component", "Composite", "Leaf", "Strategy", "ConcreteStrategy", "ConcreteComponent", "Decorator", "ConcreteDecorator", "Context"]);
const MONO = "'Geist Mono', ui-monospace, Menlo, monospace";
const SANS = "'Geist', -apple-system, 'Helvetica Neue', sans-serif";
const SERIF = "'Instrument Serif', Georgia, serif";
const MIEMBRO = { size: 11.5, ancho: 7, alto: 16 };
const NOMBRE = { size: 15, ancho: 9, alto: 20 };
const ESTEREO = { size: 10, ancho: 6.2, alto: 14 };
const ETIQUETA = { size: 11, ancho: 6.7, alto: 15 };
const PASTILLA = { size: 9.5, alto: 17 };
const PAD = 9;

// Recorte del modelo para una combinacion de capas
function recortar(capas) {
  const ver = ["dominio", ...capas];
  const clases = new Map(D.clases.filter((c) => ver.some((k) => c.capas.includes(k))).map((c) => [c.id, c]));
  const relaciones = D.relaciones.filter((r) => clases.has(r.a) && clases.has(r.b));
  const paquetes = [];
  for (const c of clases.values()) {
    let p = paquetes.find((x) => x.id === c.paquete);
    if (!p) paquetes.push((p = { id: c.paquete, clases: [] }));
    p.clases.push(c.id);
  }
  return { paquetes, clases, relaciones };
}

// ---------- medidas de cada caja ----------
function cabecera(c) {
  const linea = [];
  if (c.tipo === "interface") linea.push("interface");
  if (c.tipo === "enum") linea.push("enumeration");
  if (c.tipo === "abstract class") linea.push("abstract");
  if (c.estereotipo) linea.push(...c.estereotipo.split(" / ").filter((t) => !ROLES_GOF.has(t)));
  return linea.length ? "«" + linea.join(" · ") + "»" : null;
}

function compartimentos(c) {
  const esMetodo = (l) => l.includes("(");
  if (c.tipo === "enum") {
    const valores = c.miembros.filter((l) => /^[A-Z][A-Z0-9_]*$/.test(l));
    const resto = c.miembros.filter((l) => !/^[A-Z][A-Z0-9_]*$/.test(l));
    return [valores, resto.filter((l) => !esMetodo(l)), resto.filter(esMetodo)].filter((g) => g.length);
  }
  return [c.miembros.filter((l) => !esMetodo(l)), c.miembros.filter(esMetodo)].filter((g) => g.length);
}

// Una firma larga sigue en otra fila, cortada despues de una coma fuera de <>: completa y sin
// que la caja mida lo que la firma entera
const LARGO = 78;
function filas(l) {
  const t = limpiar(l);
  if (t.length <= LARGO) return [t];
  const partes = [];
  let prof = 0, ini = 0;
  for (let i = 0; i < t.length; i++) {
    if (t[i] === "<") prof++;
    else if (t[i] === ">") prof--;
    else if (t[i] === "," && prof === 0) { partes.push(t.slice(ini, i + 2)); ini = i + 2; }
  }
  partes.push(t.slice(ini));
  const res = [];
  let fila = "";
  for (const p of partes) {
    if (fila.trim() && (fila + p).trimEnd().length > LARGO) { res.push(fila.trimEnd()); fila = "    "; }
    fila += p;
  }
  res.push(fila);
  return res;
}

function medir(c, capas) {
  const est = cabecera(c);
  const grupos = compartimentos(c);
  const textos = grupos.flat().flatMap(filas);
  const pastillas = rolesDe(c.id, capas);
  const anchoPast = pastillas.reduce((s, p) => s + anchoPastilla(p.texto) + 4, 0);
  const ancho = Math.max(
    c.nombre.length * NOMBRE.ancho,
    est ? est.length * ESTEREO.ancho : 0,
    ...textos.map((t) => t.length * MIEMBRO.ancho),
    anchoPast,
  ) + PAD * 2;
  const altoCab = PAD + (pastillas.length ? PASTILLA.alto + 4 : 0) + (est ? ESTEREO.alto : 0) + NOMBRE.alto + PAD - 2;
  const alto = altoCab + grupos.reduce((s, g) => s + g.reduce((t, l) => t + filas(l).length, 0) * MIEMBRO.alto + PAD, 0);
  return { ancho: Math.ceil(ancho), alto: Math.ceil(alto), altoCab, est, grupos, pastillas };
}

const limpiar = (l) => l.replace(/\{(static|abstract)\}\s*/g, "");

// ---------- tipos de relacion ----------
const TIPOS = {
  "<|..": { familia: "herencia", trazo: "5,4", inicio: "triangulo", fin: null, verbo: true },
  "<|--": { familia: "herencia", trazo: null, inicio: "triangulo", fin: null, verbo: true },
  "o-->": { familia: "estructura", trazo: null, inicio: "rombo-vacio", fin: "punta" },
  "*-->": { familia: "estructura", trazo: null, inicio: "rombo-lleno", fin: "punta" },
  "*--": { familia: "estructura", trazo: null, inicio: "rombo-lleno", fin: null },
  "-->": { familia: "estructura", trazo: null, inicio: null, fin: "punta" },
  "..>": { familia: "uso", trazo: "4,3", inicio: null, fin: "punta" },
};

// ---------- layout ----------
async function disponer(modelo, medidas) {
  const etiqueta = (texto, lugar) => ({
    text: texto,
    width: Math.ceil(texto.length * ETIQUETA.ancho) + 8,
    height: ETIQUETA.alto,
    layoutOptions: { "elk.edgeLabels.placement": lugar },
  });
  const grafo = {
    id: "raiz",
    layoutOptions: {
      "elk.algorithm": "layered",
      "elk.direction": "DOWN",
      "elk.edgeRouting": "ORTHOGONAL",
      "elk.hierarchyHandling": "INCLUDE_CHILDREN",
      "elk.spacing.nodeNode": "48",
      "elk.layered.spacing.nodeNodeBetweenLayers": "64",
      "elk.spacing.edgeNode": "22",
      "elk.spacing.edgeEdge": "14",
      "elk.layered.spacing.edgeNodeBetweenLayers": "22",
      "elk.layered.spacing.edgeEdgeBetweenLayers": "14",
      "elk.spacing.edgeLabel": "4",
      "elk.layered.nodePlacement.strategy": "NETWORK_SIMPLEX",
      "elk.layered.crossingMinimization.strategy": "LAYER_SWEEP",
      "elk.layered.thoroughness": "40",
      "elk.layered.mergeEdges": "false",
    },
    children: modelo.paquetes.map((p) => ({
      id: "pkg:" + p.id,
      layoutOptions: { "elk.padding": "[top=46,left=24,bottom=24,right=24]" },
      children: p.clases.map((id) => ({ id, width: medidas.get(id).ancho, height: medidas.get(id).alto })),
    })),
    edges: modelo.relaciones.map((r, i) => {
      const labels = [etiqueta(r.verbo, "CENTER")];
      if (r.multA) labels.push(etiqueta(r.multA, "TAIL"));
      if (r.multB) labels.push(etiqueta(r.multB, "HEAD"));
      return { id: "e" + i, sources: [r.a], targets: [r.b], labels };
    }),
  };
  return new ELK().layout(grafo);
}

// ---------- dibujo ----------
const esc = (t) => t.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

function caja(c, m, x, y) {
  const col = colorPaquete(c.paquete);
  const s = [`<g class="clase" data-id="${c.id}" data-nombre="${esc(c.nombre)}">`];
  s.push(`<rect x="${x}" y="${y}" width="${m.ancho}" height="${m.alto}" rx="6" fill="${C.box}" stroke="${col}" stroke-width="1.2"/>`);
  s.push(`<path d="M${x + 0.6},${y + m.altoCab} V${y + 6} Q${x + 0.6},${y + 0.6} ${x + 6},${y + 0.6} H${x + m.ancho - 6} Q${x + m.ancho - 0.6},${y + 0.6} ${x + m.ancho - 0.6},${y + 6} V${y + m.altoCab} Z" fill="${col}" fill-opacity="0.32"/>`);
  let cy = y + PAD;
  const cx = x + m.ancho / 2;
  if (m.pastillas.length) {
    let px = x + PAD;
    for (const p of m.pastillas) {
      const w = anchoPastilla(p.texto);
      s.push(`<rect x="${px}" y="${y + 7}" width="${w}" height="${PASTILLA.alto}" rx="${PASTILLA.alto / 2}" fill="${p.color}"/>`);
      s.push(`<text x="${px + w / 2}" y="${y + 19}" text-anchor="middle" font-family="${MONO}" font-size="${PASTILLA.size}" font-weight="600" fill="#ffffff" letter-spacing="0.3">${esc(p.texto)}</text>`);
      px += w + 4;
    }
    cy += PASTILLA.alto + 4;
  }
  if (m.est) {
    cy += ESTEREO.alto - 3;
    s.push(`<text x="${cx}" y="${cy}" text-anchor="middle" font-family="${MONO}" font-size="${ESTEREO.size}" fill="${C.soft}" letter-spacing="0.4">${esc(m.est)}</text>`);
    cy += 3;
  }
  cy += NOMBRE.alto - 4;
  const italica = c.tipo === "abstract class" || c.tipo === "interface" ? ` font-style="italic"` : "";
  s.push(`<text x="${cx}" y="${cy}" text-anchor="middle" font-family="${SANS}" font-size="${NOMBRE.size}" font-weight="600" fill="${C.ink}"${italica}>${esc(c.nombre)}</text>`);
  cy = y + m.altoCab;
  for (const g of m.grupos) {
    s.push(`<line x1="${x}" y1="${cy}" x2="${x + m.ancho}" y2="${cy}" stroke="${C.ink}" stroke-width="0.6" stroke-opacity="0.45"/>`);
    cy += PAD / 2;
    for (const l of g) {
      cy += MIEMBRO.alto;
      const deco = [];
      if (l.includes("{static}")) deco.push(`text-decoration="underline"`);
      if (l.includes("{abstract}")) deco.push(`font-style="italic"`);
      const nombre = (l.match(/(\w+)\(/) || [])[1];
      const dm = nombre ? ` data-m="${nombre}"` : "";
      const fs = filas(l);
      if (nombre) s.push(`<rect class="resalte"${dm} x="${x + 2}" y="${cy - MIEMBRO.alto + 1}" width="${m.ancho - 4}" height="${fs.length * MIEMBRO.alto - 1}" rx="2" style="fill:var(--patron)" opacity="0"/>`);
      fs.forEach((f, i) => {
        const sangria = (f.length - f.trimStart().length) * MIEMBRO.ancho;
        s.push(`<text class="miembro"${dm} x="${x + PAD + sangria}" y="${cy - 4 + i * MIEMBRO.alto}" font-family="${MONO}" font-size="${MIEMBRO.size}" fill="${C.ink}" ${deco.join(" ")}>${esc(f.trimStart())}</text>`);
      });
      cy += (fs.length - 1) * MIEMBRO.alto;
    }
    cy += PAD / 2;
  }
  s.push("</g>");
  return s.join("\n");
}

// Polilinea ortogonal con esquinas redondeadas
function trazo(p, r = 7) {
  let d = `M${p[0].x},${p[0].y}`;
  for (let i = 1; i < p.length - 1; i++) {
    const a = p[i - 1], b = p[i], c = p[i + 1];
    const d1 = Math.hypot(b.x - a.x, b.y - a.y), d2 = Math.hypot(c.x - b.x, c.y - b.y);
    const k = Math.min(r, d1 / 2, d2 / 2);
    const p1 = { x: b.x + ((a.x - b.x) / d1) * k, y: b.y + ((a.y - b.y) / d1) * k };
    const p2 = { x: b.x + ((c.x - b.x) / d2) * k, y: b.y + ((c.y - b.y) / d2) * k };
    d += ` L${p1.x},${p1.y} Q${b.x},${b.y} ${p2.x},${p2.y}`;
  }
  const u = p[p.length - 1];
  return d + ` L${u.x},${u.y}`;
}

// Marca en el extremo `punta`, apuntando desde `previo` hacia `punta`
function marca(tipo, punta, previo, color) {
  const L = Math.hypot(punta.x - previo.x, punta.y - previo.y) || 1;
  const ux = (punta.x - previo.x) / L, uy = (punta.y - previo.y) / L;
  const nx = -uy, ny = ux;
  const pt = (a, b) => `${punta.x - ux * a + nx * b},${punta.y - uy * a + ny * b}`;
  if (tipo === "triangulo") {
    return `<polygon points="${pt(0, 0)} ${pt(13, 7)} ${pt(13, -7)}" fill="${C.box}" stroke="${color}" stroke-width="1.2" stroke-linejoin="miter"/>`;
  }
  if (tipo === "rombo-lleno" || tipo === "rombo-vacio") {
    const relleno = tipo === "rombo-lleno" ? color : C.box;
    return `<polygon points="${pt(0, 0)} ${pt(8, 5)} ${pt(16, 0)} ${pt(8, -5)}" fill="${relleno}" stroke="${color}" stroke-width="1.2"/>`;
  }
  if (tipo === "punta") {
    return `<polyline points="${pt(10, 5)} ${pt(0, 0)} ${pt(10, -5)}" fill="none" stroke="${color}" stroke-width="1.3" stroke-linejoin="miter"/>`;
  }
  return "";
}

const MARGEN = 32, TITULO = 244;

// Una vista: paquetes, relaciones y cajas de una combinacion de capas, en un <g> propio
function dibujarVista(modelo, medidas, g, clave, w, h) {
  const abs = new Map();
  const recorrer = (n, ox, oy) => {
    const x = ox + (n.x || 0), y = oy + (n.y || 0);
    abs.set(n.id, { x, y, w: n.width, h: n.height });
    (n.children || []).forEach((h) => recorrer(h, x, y));
  };
  recorrer(g, 0, 0);

  const out = [`<g class="vista" data-vista="${clave}" data-w="${w}" data-h="${h}">`];

  // paquetes
  for (const p of g.children) {
    const a = abs.get(p.id), col = colorPaquete(p.id.slice(4));
    out.push(`<rect x="${a.x}" y="${a.y}" width="${a.w}" height="${a.h}" rx="8" fill="${col}" fill-opacity="0.11" stroke="${col}" stroke-opacity="0.55" stroke-width="1.2"/>`);
    out.push(`<text x="${a.x + 16}" y="${a.y + 27}" font-family="${MONO}" font-size="13" font-weight="600" fill="${col}" letter-spacing="1.2">${esc(p.id.slice(4).toUpperCase())}</text>`);
  }

  // relaciones (antes que las cajas)
  const marcas = [], etiquetas = [];
  g.edges.forEach((e, i) => {
    const r = modelo.relaciones[i], t = TIPOS[r.flecha];
    const o = abs.get(e.container || "raiz") || { x: 0, y: 0 };
    const sec = e.sections[0];
    const pts = [sec.startPoint, ...(sec.bendPoints || []), sec.endPoint].map((q) => ({ x: q.x + o.x, y: q.y + o.y }));
    const col = FAMILIA[t.familia].color;
    const guion = t.trazo ? ` stroke-dasharray="${t.trazo}"` : "";
    const sujeto = r.flecha.startsWith("<|") ? r.b : r.a;
    const attrs = `data-a="${r.a}" data-b="${r.b}" data-sujeto="${sujeto}" data-frase="${esc(frase(r, modelo))}"`;
    // data-inv: implementa y extiende apuntan al origen del trazo (el pulso del hover viaja al reves)
    const inverso = r.flecha.startsWith("<|") ? ` data-inv="1"` : "";
    out.push(`<g class="rel" ${attrs}${inverso}><path d="${trazo(pts)}" fill="none" stroke="${col}" stroke-width="${t.familia === "uso" ? 1.2 : 1.5}"${guion}/></g>`);
    marcas.push(`<g class="rel" data-a="${r.a}" data-b="${r.b}">` +
      (t.inicio ? marca(t.inicio, pts[0], pts[1], col) : "") +
      (t.fin ? marca(t.fin, pts[pts.length - 1], pts[pts.length - 2], col) : "") + `</g>`);
    etiquetas.push(`<g class="rel" data-a="${r.a}" data-b="${r.b}">`);
    for (const l of e.labels || []) {
      const x = l.x + o.x, y = l.y + o.y;
      const esMult = l.text !== r.verbo;
      etiquetas.push(
        `<rect x="${x}" y="${y}" width="${l.width}" height="${l.height}" rx="2" fill="${C.paper}" fill-opacity="0.92"/>` +
        `<text x="${x + l.width / 2}" y="${y + l.height - 4}" text-anchor="middle" font-family="${MONO}" font-size="${ETIQUETA.size}" fill="${esMult ? C.ink : t.familia === "uso" ? C.soft : col}"${esMult ? ` font-weight="600"` : ` font-style="italic"`}>${esc(l.text)}</text>`,
      );
    }
    etiquetas.push(`</g>`);
  });

  for (const [id, c] of modelo.clases) {
    const a = abs.get(id);
    out.push(caja(c, medidas.get(id), a.x, a.y));
  }
  out.push(...marcas, ...etiquetas, `</g>`);
  return out.join("\n");
}

// "Credito [1] se divide en [1..*] Cuota"; implementa y extiende se leen de la hija al padre
function frase(r, modelo) {
  const m = (x) => (x ? ` [${x}]` : "");
  const n = (id) => modelo.clases.get(id).nombre;
  if (r.flecha.startsWith("<|")) return `${n(r.b)} ${r.verbo} ${n(r.a)}`;
  return `${n(r.a)}${m(r.multA)} ${r.verbo}${m(r.multB)} ${n(r.b)}`;
}


// Vista para una combinacion de capas: { clave, html, w, h }
async function vista(capas) {
  const m = recortar(capas);
  const medidas = new Map([...m.clases.values()].map((c) => [c.id, medir(c, capas)]));
  const g = await disponer(m, medidas);
  const clave = ["dominio", ...capas].join("+");
  const w = Math.max(Math.ceil(g.width) + MARGEN * 2, D.anchoCabecera), h = Math.ceil(g.height) + MARGEN * 2 + TITULO;
  return { clave, w, h, clases: m.clases.size, relaciones: m.relaciones.length, html: dibujarVista(m, medidas, g, clave, w, h) };
}

return { C, FAMILIA, MONO, SANS, SERIF, MARGEN, TITULO, esc, vista };
}

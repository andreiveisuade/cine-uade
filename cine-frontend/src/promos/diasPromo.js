export const DIAS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

// La fecha viaja sin hora: armarla con Date la correría un día por la zona horaria.
export function fecha(iso, conAnio = true) {
  const [anio, mes, dia] = iso.split("-");
  return conAnio ? `${dia}/${mes}/${anio}` : `${dia}/${mes}`;
}

export function hoyEnLaSemana() {
  return DIAS[(new Date().getDay() + 6) % 7];
}

export function hoyIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export const corre = (promo, dia) => promo.diasSemana.length === 0 || promo.diasSemana.includes(dia);

// Solo presentación (el descuento lo decide el cobro): «hoy» es que alguna función de hoy todavía pueda entrar,
// así que con franja ya terminada a esta hora no se anuncia.
export function aplicaHoy(promo) {
  const hoy = hoyIso();
  const ahora = new Date().toTimeString().slice(0, 5);
  const franjaAbierta = !promo.horaHasta || ahora < promo.horaHasta.slice(0, 5);
  return promo.vigenciaDesde <= hoy && hoy <= promo.vigenciaHasta && corre(promo, hoyEnLaSemana()) && franjaAbierta;
}

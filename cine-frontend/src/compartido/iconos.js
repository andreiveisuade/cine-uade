import { IconId, IconMoodKid, IconOld, IconSchool } from "@tabler/icons-react";

// Las tarifas que se acreditan en la puerta llevan ícono: se ve de un vistazo qué butaca pide carnet.
const ICONO_TARIFA = { ESTUDIANTE: IconSchool, JUBILADO: IconOld, MENOR: IconMoodKid };

export const iconoTarifa = (tarifa) => ICONO_TARIFA[tarifa] ?? IconId;

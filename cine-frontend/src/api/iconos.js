import { IconBuildingBank, IconCash, IconCreditCard, IconCreditCardPay, IconId, IconMoodKid, IconOld, IconQrcode, IconSchool,
         IconWallet } from "@tabler/icons-react";

// Como las etiquetas: presentación de las constantes del backend. Un medio nuevo sin ícono cae en la billetera.
const ICONO_MEDIO = {
  EFECTIVO: IconCash,
  DEBITO: IconCreditCard,
  CREDITO: IconCreditCardPay,
  QR: IconQrcode,
  TRANSFERENCIA: IconBuildingBank,
};

export const iconoMedio = (medio) => ICONO_MEDIO[medio] ?? IconWallet;

// Las tarifas que se acreditan en la puerta llevan ícono: se ve de un vistazo qué butaca pide carnet.
const ICONO_TARIFA = { ESTUDIANTE: IconSchool, JUBILADO: IconOld, MENOR: IconMoodKid };

export const iconoTarifa = (tarifa) => ICONO_TARIFA[tarifa] ?? IconId;

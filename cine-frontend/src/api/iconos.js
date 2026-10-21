import { IconBuildingBank, IconCash, IconCreditCard, IconCreditCardPay, IconQrcode, IconWallet } from "@tabler/icons-react";

// Como las etiquetas: presentación de las constantes del backend. Un medio nuevo sin ícono cae en la billetera.
const ICONO_MEDIO = {
  EFECTIVO: IconCash,
  DEBITO: IconCreditCard,
  CREDITO: IconCreditCardPay,
  QR: IconQrcode,
  TRANSFERENCIA: IconBuildingBank,
};

export const iconoMedio = (medio) => ICONO_MEDIO[medio] ?? IconWallet;

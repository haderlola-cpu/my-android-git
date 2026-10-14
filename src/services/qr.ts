import QRCode from 'qrcode';

export interface GenerateQrOptions {
  color?: {
    dark?: string;
    light?: string;
  };
  width?: number;
  margin?: number;
}

export async function generateQrDataUrl(
  text: string,
  options: GenerateQrOptions = {}
): Promise<string> {
  const defaultOpts: QRCode.QRCodeToDataURLOptions = {
    errorCorrectionLevel: 'M',
    margin: options.margin !== undefined ? options.margin : 1,
    width: options.width || 300,
    color: {
      dark: options.color?.dark || '#000000',
      light: options.color?.light || '#FFFFFF',
    },
  };

  try {
    return await QRCode.toDataURL(text, defaultOpts);
  } catch (err) {
    console.error('QR code generation failed:', err);
    return '';
  }
}

export function formatGuestQrPayload(eventId: number, guestCode: string): string {
  return `eb:${eventId}:${guestCode}`;
}

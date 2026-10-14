import JSZip from 'jszip';
import { saveAs } from 'file-saver';
import { EventEntity, GuestEntity } from '../types';
import { PRESET_BACKGROUNDS, FONT_CHOICES } from './catalog';
import { generateQrDataUrl, formatGuestQrPayload } from './qr';

export interface RenderCardOptions {
  width?: number;
  height?: number;
  isOriginalQuality?: boolean;
}

export async function renderInvitationCardToCanvas(
  event: EventEntity,
  guest: GuestEntity,
  options: RenderCardOptions = {}
): Promise<HTMLCanvasElement> {
  const isHighRes = options.isOriginalQuality || false;
  const width = options.width || (isHighRes ? 2400 : 1200);
  const height = options.height || Math.round(width / (event.backgroundAspectRatio || 0.714));

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  if (!ctx) return canvas;

  // 1. Draw Background
  const preset = PRESET_BACKGROUNDS[event.backgroundPresetId] || PRESET_BACKGROUNDS[0];

  if (event.backgroundType === 'IMAGE' && event.backgroundUri) {
    try {
      const img = await loadImage(event.backgroundUri);
      ctx.drawImage(img, 0, 0, width, height);
    } catch {
      drawPresetGradient(ctx, width, height, preset);
    }
  } else if (preset.imageFallback) {
    try {
      const img = await loadImage(preset.imageFallback);
      ctx.drawImage(img, 0, 0, width, height);
      // Dark overlay for text contrast
      ctx.fillStyle = 'rgba(5, 32, 24, 0.45)';
      ctx.fillRect(0, 0, width, height);
    } catch {
      drawPresetGradient(ctx, width, height, preset);
    }
  } else {
    drawPresetGradient(ctx, width, height, preset);
  }

  // 2. Draw Luxury Ornate Border
  drawLuxuryBorder(ctx, width, height, preset.accentColor);

  // 3. Setup Font
  const fontChoice = FONT_CHOICES[event.fontIndex] || FONT_CHOICES[0];
  const primaryFontFamily = fontChoice.fontFamily;
  const scale = width / 1000;

  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';

  // 4. Header Verse / Basmala
  if (event.headerVerse) {
    ctx.fillStyle = preset.accentColor;
    ctx.font = `italic ${Math.round(20 * scale)}px 'Amiri', serif`;
    ctx.fillText(event.headerVerse, width * 0.5, height * 0.08);
  }

  // 5. Groom & Bride Names
  if (event.groomName || event.brideName) {
    const coupleText = event.groomName && event.brideName
      ? `${event.groomName}  ❤️  ${event.brideName}`
      : (event.groomName || event.brideName);
    ctx.fillStyle = '#FFF6D8';
    ctx.font = `bold ${Math.round(36 * scale)}px '${primaryFontFamily}', sans-serif`;
    ctx.fillText(coupleText, width * 0.5, height * 0.14);
  }

  // 6. Hosts Line
  if (event.hostsText) {
    ctx.fillStyle = '#F3DC8A';
    ctx.font = `500 ${Math.round(18 * scale)}px '${primaryFontFamily}', sans-serif`;
    ctx.fillText(event.hostsText, width * 0.5, height * 0.19);
  }

  // 7. Event Title
  ctx.fillStyle = event.nameColorHex || '#FFF6D8';
  ctx.font = `800 ${Math.round(32 * scale)}px '${primaryFontFamily}', sans-serif`;
  ctx.fillText(event.name, width * 0.5, height * 0.25);

  // 8. Main QR Code for Guest Entry
  const qrPayload = formatGuestQrPayload(event.id, guest.code);
  const qrDataUrl = await generateQrDataUrl(qrPayload, {
    width: Math.round(width * (event.qrSizeRatio || 0.28)),
    color: {
      dark: event.qrColorHex || '#121826',
      light: event.qrBgColorHex || '#FFFFFF',
    },
  });

  if (qrDataUrl) {
    const qrImg = await loadImage(qrDataUrl);
    const qrPx = width * (event.qrSizeRatio || 0.28);
    const qrX = width * (event.qrPosX ?? 0.50) - qrPx / 2;
    const qrY = height * (event.qrPosY ?? 0.46) - qrPx / 2;

    // QR Card Background Frame with rounded corner and gold border
    ctx.save();
    ctx.fillStyle = event.qrBgColorHex || '#FFFFFF';
    ctx.shadowColor = 'rgba(0, 0, 0, 0.4)';
    ctx.shadowBlur = 20 * scale;
    roundRect(ctx, qrX - 8 * scale, qrY - 8 * scale, qrPx + 16 * scale, qrPx + 16 * scale, 16 * scale);
    ctx.fill();
    ctx.strokeStyle = preset.accentColor;
    ctx.lineWidth = 3 * scale;
    ctx.stroke();
    ctx.restore();

    ctx.drawImage(qrImg, qrX, qrY, qrPx, qrPx);

    // Sequence Code Badge below QR
    ctx.fillStyle = preset.accentColor;
    ctx.font = `bold ${Math.round(14 * scale)}px monospace`;
    ctx.fillText(`#${guest.sequenceNumber}  ·  ${guest.code}`, width * (event.qrPosX ?? 0.50), qrY + qrPx + 24 * scale);
  }

  // 9. Guest Name
  if (event.showGuestName) {
    const nameY = height * (event.namePosY ?? 0.68);
    ctx.fillStyle = event.nameColorHex || '#FFF6D8';
    const nameSize = (event.nameSizeSp || 24) * scale;
    ctx.font = `bold ${Math.round(nameSize)}px '${primaryFontFamily}', sans-serif`;

    // Subtle dark pill background for readability
    const nameText = guest.name;
    const textWidth = ctx.measureText(nameText).width;
    ctx.save();
    ctx.fillStyle = 'rgba(5, 32, 24, 0.75)';
    ctx.strokeStyle = 'rgba(212, 175, 55, 0.6)';
    ctx.lineWidth = 1.5 * scale;
    roundRect(ctx, width * 0.5 - textWidth / 2 - 24 * scale, nameY - nameSize * 0.7, textWidth + 48 * scale, nameSize * 1.4, 20 * scale);
    ctx.fill();
    ctx.stroke();
    ctx.restore();

    ctx.fillText(nameText, width * 0.5, nameY);
  }

  // 10. Date, Time & Venue
  const dateY = height * (event.datePosY ?? 0.78);
  if (event.showDate || event.showTime) {
    const dayDate = [event.dayName, event.showDate ? event.date : ''].filter(Boolean).join('  ·  ');
    const timeText = event.showTime ? `${event.timeFrom} - ${event.timeTo}` : '';
    const fullDateTime = [dayDate, timeText].filter(Boolean).join('  |  ');

    ctx.fillStyle = '#F3DC8A';
    ctx.font = `600 ${Math.round(18 * scale)}px '${primaryFontFamily}', sans-serif`;
    ctx.fillText(fullDateTime, width * 0.5, dateY);
  }

  if (event.venueName) {
    ctx.fillStyle = '#FFF6D8';
    ctx.font = `bold ${Math.round(20 * scale)}px '${primaryFontFamily}', sans-serif`;
    ctx.fillText(`📍 ${event.venueName}`, width * 0.5, dateY + 30 * scale);
    if (event.venueAddress) {
      ctx.fillStyle = '#9DB8AD';
      ctx.font = `normal ${Math.round(14 * scale)}px '${primaryFontFamily}', sans-serif`;
      ctx.fillText(event.venueAddress, width * 0.5, dateY + 54 * scale);
    }
  }

  // 11. Location QR Code (Bottom Left or Right if URL is present)
  if (event.showLocationQr && event.locationUrl) {
    const locQrUrl = await generateQrDataUrl(event.locationUrl, { width: Math.round(100 * scale) });
    if (locQrUrl) {
      const locImg = await loadImage(locQrUrl);
      const locSize = 75 * scale;
      const locX = width * 0.82;
      const locY = height * 0.88;

      ctx.save();
      ctx.fillStyle = '#FFFFFF';
      roundRect(ctx, locX - 4 * scale, locY - 4 * scale, locSize + 8 * scale, locSize + 8 * scale, 8 * scale);
      ctx.fill();
      ctx.restore();

      ctx.drawImage(locImg, locX, locY, locSize, locSize);
      ctx.fillStyle = '#F3DC8A';
      ctx.font = `bold ${Math.round(10 * scale)}px sans-serif`;
      ctx.fillText('موقع القاعة', locX + locSize / 2, locY + locSize + 14 * scale);
    }
  }

  // 12. Notes & Closing Text
  if (event.notesText) {
    ctx.fillStyle = '#F3DC8A';
    ctx.font = `italic ${Math.round(13 * scale)}px '${primaryFontFamily}', sans-serif`;
    ctx.fillText(event.notesText, width * 0.5, height * 0.93);
  }
  if (event.closingText) {
    ctx.fillStyle = '#FFF6D8';
    ctx.font = `600 ${Math.round(15 * scale)}px '${primaryFontFamily}', sans-serif`;
    ctx.fillText(event.closingText, width * 0.5, height * 0.96);
  }

  return canvas;
}

function drawPresetGradient(
  ctx: CanvasRenderingContext2D,
  w: number,
  h: number,
  preset: { topColor: string; centerColor: string; bottomColor: string }
) {
  const grad = ctx.createLinearGradient(0, 0, 0, h);
  grad.addColorStop(0, preset.topColor);
  grad.addColorStop(0.5, preset.centerColor);
  grad.addColorStop(1, preset.bottomColor);
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, w, h);
}

function drawLuxuryBorder(
  ctx: CanvasRenderingContext2D,
  w: number,
  h: number,
  accentColor: string
) {
  ctx.save();
  const m1 = w * 0.035;
  const m2 = w * 0.045;

  // Outer border
  ctx.strokeStyle = accentColor;
  ctx.lineWidth = 2.5;
  ctx.strokeRect(m1, m1, w - m1 * 2, h - m1 * 2);

  // Inner border
  ctx.strokeStyle = 'rgba(212, 175, 55, 0.45)';
  ctx.lineWidth = 1;
  ctx.strokeRect(m2, m2, w - m2 * 2, h - m2 * 2);

  // Diamond corner ornaments
  const cornerSize = w * 0.02;
  const drawCornerDiamond = (x: number, y: number) => {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(Math.PI / 4);
    ctx.fillStyle = accentColor;
    ctx.fillRect(-cornerSize / 2, -cornerSize / 2, cornerSize, cornerSize);
    ctx.restore();
  };

  drawCornerDiamond(m1, m1);
  drawCornerDiamond(w - m1, m1);
  drawCornerDiamond(m1, h - m1);
  drawCornerDiamond(w - m1, h - m1);

  ctx.restore();
}

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = e => reject(e);
    img.src = src;
  });
}

export async function downloadGuestCardImage(event: EventEntity, guest: GuestEntity, isHighRes: boolean = false) {
  const canvas = await renderInvitationCardToCanvas(event, guest, { isOriginalQuality: isHighRes });
  canvas.toBlob(blob => {
    if (blob) {
      const fileName = `${event.name}_${guest.sequenceNumber}_${guest.name.replace(/\s+/g, '_')}.png`;
      saveAs(blob, fileName);
    }
  }, 'image/png');
}

export async function exportAllGuestsZip(
  event: EventEntity,
  guests: GuestEntity[],
  onProgress?: (current: number, total: number) => void
): Promise<void> {
  const zip = new JSZip();
  const folder = zip.folder(`${event.name}_دعوات_الضيوف`);

  for (let i = 0; i < guests.length; i++) {
    const guest = guests[i];
    const canvas = await renderInvitationCardToCanvas(event, guest, { isOriginalQuality: false });
    const dataUrl = canvas.toDataURL('image/png');
    const base64Data = dataUrl.replace(/^data:image\/png;base64,/, '');

    const safeName = `${guest.sequenceNumber.toString().padStart(3, '0')}_${guest.name.replace(/[/\\?%*:|"<>]/g, '_')}`;
    folder?.file(`${safeName}.png`, base64Data, { base64: true });

    if (onProgress) {
      onProgress(i + 1, guests.length);
    }
  }

  const content = await zip.generateAsync({ type: 'blob' });
  saveAs(content, `${event.name}_all_invitations.zip`);
}

export function exportAttendanceCsv(event: EventEntity, guests: GuestEntity[]) {
  const header = ['الرقم التسلسلي', 'كود الدعوة', 'اسم الضيف', 'رقم الهاتف', 'حالة الحضور', 'تاريخ ووقت الحضور', 'تم إرسال واتساب'];
  const rows = guests.map(g => [
    g.sequenceNumber,
    g.code,
    `"${g.name.replace(/"/g, '""')}"`,
    `"${g.phone}"`,
    g.attended ? 'حضر ✅' : 'لم يحضر ⏳',
    g.attendedAt ? new Date(g.attendedAt).toLocaleString('ar-EG') : '—',
    g.whatsappSent ? 'نعم' : 'لا',
  ]);

  const csvContent = '\uFEFF' + [header.join(','), ...rows.map(r => r.join(','))].join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  saveAs(blob, `${event.name}_attendance_sheet.csv`);
}

import React, { useEffect, useRef, useState, useCallback } from 'react';
import { EventEntity, GuestEntity } from '../types';
import { PRESET_BACKGROUNDS, FONT_CHOICES } from '../services/catalog';
import { generateQrDataUrl, formatGuestQrPayload } from '../services/qr';

interface CardPreviewProps {
  event: EventEntity;
  guest?: GuestEntity;
  onPositionChange?: (updates: {
    qrPosX?: number;
    qrPosY?: number;
    namePosX?: number;
    namePosY?: number;
    datePosX?: number;
    datePosY?: number;
  }) => void;
  interactive?: boolean;
}

export const CardPreview: React.FC<CardPreviewProps> = ({
  event,
  guest,
  onPositionChange,
  interactive = true,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const [locationQrDataUrl, setLocationQrDataUrl] = useState<string>('');
  const [activeDrag, setActiveDrag] = useState<'qr' | 'name' | 'date' | null>(null);

  const sampleGuest: GuestEntity = guest || {
    id: 0,
    eventId: event.id,
    name: 'السيد أحمد محمد الشاذلي وعائلته الكريمة',
    normalizedName: 'السيد احمد محمد الشاذلي وعائلته الكريمه',
    phone: '01012345678',
    normalizedPhone: '01012345678',
    code: '48291037',
    sequenceNumber: 1,
    attended: false,
    attendedAt: null,
    whatsappSent: false,
    createdAt: Date.now(),
    cloudKey: 'sample',
    updatedAt: Date.now(),
  };

  // Generate QR code data URLs
  useEffect(() => {
    let cancelled = false;
    const payload = formatGuestQrPayload(event.id, sampleGuest.code);
    generateQrDataUrl(payload, {
      color: {
        dark: event.qrColorHex || '#121826',
        light: event.qrBgColorHex || '#FFFFFF',
      },
    }).then(url => {
      if (!cancelled) setQrDataUrl(url);
    });

    if (event.showLocationQr && event.locationUrl) {
      generateQrDataUrl(event.locationUrl).then(url => {
        if (!cancelled) setLocationQrDataUrl(url);
      });
    } else {
      setLocationQrDataUrl('');
    }

    return () => {
      cancelled = true;
    };
  }, [
    event.id,
    sampleGuest.code,
    event.qrColorHex,
    event.qrBgColorHex,
    event.showLocationQr,
    event.locationUrl,
  ]);

  const preset = PRESET_BACKGROUNDS[event.backgroundPresetId] || PRESET_BACKGROUNDS[0];
  const fontChoice = FONT_CHOICES[event.fontIndex] || FONT_CHOICES[0];

  // Drag handling
  const handlePointerDown = (type: 'qr' | 'name' | 'date', e: React.PointerEvent) => {
    if (!interactive || !onPositionChange) return;
    e.stopPropagation();
    setActiveDrag(type);
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    if (!activeDrag || !containerRef.current || !onPositionChange) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = Math.min(Math.max(0.1, (e.clientX - rect.left) / rect.width), 0.9);
    const y = Math.min(Math.max(0.1, (e.clientY - rect.top) / rect.height), 0.9);

    if (activeDrag === 'qr') {
      onPositionChange({ qrPosX: Number(x.toFixed(2)), qrPosY: Number(y.toFixed(2)) });
    } else if (activeDrag === 'name') {
      onPositionChange({ namePosX: Number(x.toFixed(2)), namePosY: Number(y.toFixed(2)) });
    } else if (activeDrag === 'date') {
      onPositionChange({ datePosX: Number(x.toFixed(2)), datePosY: Number(y.toFixed(2)) });
    }
  }, [activeDrag, onPositionChange]);

  const handlePointerUp = (e: React.PointerEvent) => {
    if (activeDrag) {
      try {
        (e.target as HTMLElement).releasePointerCapture(e.pointerId);
      } catch {}
      setActiveDrag(null);
    }
  };

  const bgStyle: React.CSSProperties = {
    fontFamily: fontChoice.cssFamily,
  };

  if (event.backgroundType === 'IMAGE' && event.backgroundUri) {
    bgStyle.backgroundImage = `url(${event.backgroundUri})`;
    bgStyle.backgroundSize = 'cover';
    bgStyle.backgroundPosition = 'center';
  } else if (preset.imageFallback) {
    bgStyle.backgroundImage = `linear-gradient(rgba(5, 32, 24, 0.45), rgba(5, 32, 24, 0.45)), url(${preset.imageFallback})`;
    bgStyle.backgroundSize = 'cover';
    bgStyle.backgroundPosition = 'center';
  } else {
    bgStyle.background = `linear-gradient(180deg, ${preset.topColor} 0%, ${preset.centerColor} 50%, ${preset.bottomColor} 100%)`;
  }

  return (
    <div className="flex flex-col items-center">
      {/* 5:7 Portrait luxury card */}
      <div
        ref={containerRef}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        className="relative w-full max-w-[390px] aspect-[5/7] rounded-3xl shadow-2xl overflow-hidden border border-[#D4AF37]/70 select-none cursor-default transition-all duration-150"
        style={bgStyle}
      >
        {/* Ornate Luxury Inner Frame */}
        <div className="absolute inset-3 border-2 border-[#D4AF37] pointer-events-none rounded-2xl opacity-80" />
        <div className="absolute inset-4.5 border border-[#F3DC8A]/40 pointer-events-none rounded-xl" />

        {/* Diamond Corner Accents */}
        <div className="absolute top-3.5 left-3.5 w-2.5 h-2.5 bg-[#D4AF37] rotate-45 pointer-events-none" />
        <div className="absolute top-3.5 right-3.5 w-2.5 h-2.5 bg-[#D4AF37] rotate-45 pointer-events-none" />
        <div className="absolute bottom-3.5 left-3.5 w-2.5 h-2.5 bg-[#D4AF37] rotate-45 pointer-events-none" />
        <div className="absolute bottom-3.5 right-3.5 w-2.5 h-2.5 bg-[#D4AF37] rotate-45 pointer-events-none" />

        {/* 1. Header Verse */}
        {event.headerVerse && (
          <div className="absolute top-7 left-6 right-6 text-center text-xs md:text-sm italic font-['Amiri',serif] text-[#F3DC8A] leading-relaxed drop-shadow-sm pointer-events-none">
            {event.headerVerse}
          </div>
        )}

        {/* 2. Groom & Bride Names */}
        {(event.groomName || event.brideName) && (
          <div className="absolute top-14 left-6 right-6 text-center text-lg md:text-xl font-black text-[#FFF6D8] drop-shadow-md pointer-events-none">
            {event.groomName && event.brideName
              ? `${event.groomName}  ❤️  ${event.brideName}`
              : (event.groomName || event.brideName)}
          </div>
        )}

        {/* 3. Hosts Invitation Text */}
        {event.hostsText && (
          <div className="absolute top-22 left-6 right-6 text-center text-xs text-[#F3DC8A]/90 drop-shadow-sm pointer-events-none">
            {event.hostsText}
          </div>
        )}

        {/* 4. Event Title */}
        <div className="absolute top-28 left-6 right-6 text-center text-base md:text-lg font-extrabold text-[#FFF6D8] drop-shadow-md pointer-events-none">
          {event.name}
        </div>

        {/* 5. Draggable QR Code */}
        <div
          onPointerDown={e => handlePointerDown('qr', e)}
          style={{
            left: `${(event.qrPosX ?? 0.50) * 100}%`,
            top: `${(event.qrPosY ?? 0.46) * 100}%`,
            transform: 'translate(-50%, -50%)',
          }}
          className={`absolute flex flex-col items-center z-20 ${
            interactive ? 'cursor-move touch-none' : ''
          }`}
        >
          <div
            className="p-2 rounded-2xl bg-white shadow-2xl border-2 border-[#D4AF37] transition-transform hover:scale-105"
            style={{ backgroundColor: event.qrBgColorHex || '#FFFFFF' }}
          >
            {qrDataUrl ? (
              <img
                src={qrDataUrl}
                alt="QR Code"
                className="w-24 h-24 md:w-28 md:h-28 object-contain pointer-events-none"
              />
            ) : (
              <div className="w-24 h-24 md:w-28 md:h-28 bg-gray-200 animate-pulse rounded" />
            )}
          </div>
          <span className="text-[10px] font-mono font-bold text-[#F3DC8A] mt-1 bg-[#052018]/80 px-2 py-0.5 rounded-full border border-[#D4AF37]/40 shadow">
            #{sampleGuest.sequenceNumber} · {sampleGuest.code}
          </span>
        </div>

        {/* 6. Draggable Guest Name */}
        {event.showGuestName && (
          <div
            onPointerDown={e => handlePointerDown('name', e)}
            style={{
              left: `${(event.namePosX ?? 0.50) * 100}%`,
              top: `${(event.namePosY ?? 0.70) * 100}%`,
              transform: 'translate(-50%, -50%)',
            }}
            className={`absolute z-20 max-w-[90%] text-center ${
              interactive ? 'cursor-move touch-none' : ''
            }`}
          >
            <div className="px-4 py-1.5 rounded-full bg-[#052018]/85 border border-[#D4AF37]/70 shadow-lg backdrop-blur-xs">
              <span
                style={{
                  color: event.nameColorHex || '#FFF6D8',
                  fontSize: `${event.nameSizeSp ? event.nameSizeSp * 0.75 : 16}px`,
                }}
                className="font-bold whitespace-nowrap block"
              >
                {sampleGuest.name}
              </span>
            </div>
          </div>
        )}

        {/* 7. Draggable Date, Time & Venue */}
        <div
          onPointerDown={e => handlePointerDown('date', e)}
          style={{
            left: `${(event.datePosX ?? 0.50) * 100}%`,
            top: `${(event.datePosY ?? 0.81) * 100}%`,
            transform: 'translate(-50%, -50%)',
          }}
          className={`absolute z-20 w-[92%] text-center flex flex-col items-center gap-0.5 ${
            interactive ? 'cursor-move touch-none' : ''
          }`}
        >
          {(event.showDate || event.showTime) && (
            <div className="text-xs font-semibold text-[#F3DC8A] drop-shadow-sm flex items-center justify-center gap-2">
              {event.dayName && <span>{event.dayName}</span>}
              {event.showDate && <span>{event.date}</span>}
              {event.showTime && <span>· {event.timeFrom} - {event.timeTo}</span>}
            </div>
          )}
          {event.venueName && (
            <div className="text-xs font-bold text-[#FFF6D8] drop-shadow-sm">
              📍 {event.venueName}
            </div>
          )}
        </div>

        {/* 8. Location QR Code */}
        {event.showLocationQr && locationQrDataUrl && (
          <div className="absolute bottom-6 left-6 flex flex-col items-center z-10 pointer-events-none">
            <div className="p-1 bg-white rounded-lg shadow-md border border-[#D4AF37]">
              <img src={locationQrDataUrl} alt="Location" className="w-10 h-10 object-contain" />
            </div>
            <span className="text-[8px] text-[#F3DC8A] font-bold mt-0.5">موقع القاعة</span>
          </div>
        )}

        {/* 9. Notes & Closing Text */}
        <div className="absolute bottom-3 left-6 right-6 text-center pointer-events-none flex flex-col gap-0.5">
          {event.notesText && (
            <p className="text-[10px] text-[#F3DC8A]/90 italic leading-tight">
              {event.notesText}
            </p>
          )}
          {event.closingText && (
            <p className="text-[11px] text-[#FFF6D8] font-bold leading-tight">
              {event.closingText}
            </p>
          )}
        </div>
      </div>

      {interactive && (
        <p className="text-xs text-[#9DB8AD] text-center mt-2.5">
          💡 يمكنك سحب الباركود أو اسم المدعو أو التاريخ لتغيير مكانه مباشرة.
        </p>
      )}
    </div>
  );
};

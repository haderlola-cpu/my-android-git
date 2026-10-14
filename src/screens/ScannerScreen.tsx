import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Camera,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  RotateCcw,
  Volume2,
  Calendar,
  Sparkles,
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { EventWithCounts, ScanOutcome, AppPreferences } from '../types';
import { storageService } from '../services/storage';
import { soundEffects } from '../services/sound';
import { getAppStrings } from '../services/catalog';

interface ScannerScreenProps {
  events: EventWithCounts[];
  preferences: AppPreferences;
  initialEventId?: number;
}

export const ScannerScreen: React.FC<ScannerScreenProps> = ({
  events,
  preferences,
  initialEventId,
}) => {
  const strings = getAppStrings(preferences.languageCode);
  const [selectedEventId, setSelectedEventId] = useState<number>(() => {
    if (initialEventId && events.some(e => e.event.id === initialEventId)) {
      return initialEventId;
    }
    return events[0]?.event.id || 1;
  });

  const videoRef = useRef<HTMLVideoElement>(null);
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [manualCode, setManualCode] = useState('');
  const [currentOutcome, setCurrentOutcome] = useState<ScanOutcome | null>(null);
  const [scanHistory, setScanHistory] = useState<ScanOutcome[]>([]);
  const scanningRef = useRef(false);
  const streamRef = useRef<MediaStream | null>(null);

  const selectedEvent = events.find(e => e.event.id === selectedEventId)?.event;

  const handleProcessBarcode = useCallback((barcode: string) => {
    if (scanningRef.current || !selectedEventId) return;
    scanningRef.current = true;

    const outcome = storageService.verifyBarcode(selectedEventId, barcode);
    setCurrentOutcome(outcome);
    setScanHistory(prev => [outcome, ...prev.slice(0, 9)]);

    if (outcome.status === 'SUCCESS') {
      soundEffects.playSuccess();
      try {
        confetti({
          particleCount: 80,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#D4AF37', '#F3DC8A', '#10B981', '#FFF6D8'],
        });
      } catch {}
    } else if (outcome.status === 'ALREADY_CHECKED_IN') {
      soundEffects.playWarning();
    } else {
      soundEffects.playError();
    }
  }, [selectedEventId]);

  const startCamera = useCallback(async () => {
    setCameraError(null);
    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        setCameraError('الكاميرا غير متوفرة في المتصفح، يمكنك إدخال الكود يدوياً.');
        return;
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
        setCameraActive(true);
      }
    } catch (err: any) {
      console.warn('Camera error:', err);
      setCameraError('تعذر فتح الكاميرا (يرجى السماح بصلاحية الكاميرا أو استخدام الإدخال اليدوي).');
      setCameraActive(false);
    }
  }, []);

  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  }, []);

  useEffect(() => {
    startCamera();

    let scanInterval: any;
    const hasDetector = typeof window !== 'undefined' && 'BarcodeDetector' in window;

    if (hasDetector) {
      try {
        const detector = new (window as any).BarcodeDetector({ formats: ['qr_code'] });
        scanInterval = setInterval(async () => {
          if (!videoRef.current || !cameraActive || scanningRef.current) return;
          try {
            const barcodes = await detector.detect(videoRef.current);
            if (barcodes.length > 0 && barcodes[0].rawValue) {
              handleProcessBarcode(barcodes[0].rawValue);
            }
          } catch {}
        }, 300);
      } catch {}
    }

    return () => {
      clearInterval(scanInterval);
      stopCamera();
    };
  }, [cameraActive, startCamera, stopCamera, handleProcessBarcode]);

  const handleNextScan = () => {
    setCurrentOutcome(null);
    scanningRef.current = false;
  };

  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!manualCode.trim()) return;
    handleProcessBarcode(manualCode.trim());
    setManualCode('');
  };

  const currentEventGuests = selectedEvent ? storageService.getGuestsForEvent(selectedEvent.id) : [];
  const unattended = currentEventGuests.find(g => !g.attended);

  return (
    <div className="space-y-4 pb-28 max-w-2xl mx-auto">
      {/* Event Selector Header */}
      <div className="bg-[#082d22] border border-[#D4AF37]/50 rounded-2xl p-4 shadow-lg flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="p-2 bg-[#D4AF37]/20 rounded-xl text-[#F3DC8A]">
            <Camera size={20} />
          </div>
          <div>
            <h2 className="text-base font-black text-[#FFF6D8]">{strings.scannerTitle}</h2>
            <p className="text-xs text-[#9DB8AD]">فحص وتسجيل حضور المدعوين عند البوابة</p>
          </div>
        </div>

        {/* Dropdown */}
        <div className="flex items-center gap-1.5 bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-1.5">
          <Calendar size={14} className="text-[#D4AF37]" />
          <select
            value={selectedEventId}
            onChange={e => {
              setSelectedEventId(parseInt(e.target.value, 10));
              handleNextScan();
            }}
            className="bg-transparent text-[#FFF6D8] text-xs font-bold focus:outline-none cursor-pointer"
          >
            {events.map(e => (
              <option key={e.event.id} value={e.event.id} className="bg-[#052018]">
                {e.event.name} ({e.attendedGuests}/{e.totalGuests})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Camera Live Feed Viewport */}
      <div className="relative aspect-video w-full rounded-3xl overflow-hidden bg-black border-2 border-[#D4AF37] shadow-2xl flex items-center justify-center">
        <video
          ref={videoRef}
          playsInline
          muted
          className={`w-full h-full object-cover ${cameraActive ? 'block' : 'hidden'}`}
        />

        {cameraActive && !currentOutcome && (
          <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
            <div className="w-48 h-48 md:w-56 md:h-56 border-2 border-emerald-400 rounded-2xl relative shadow-[0_0_30px_rgba(16,185,129,0.3)]">
              <div className="absolute -top-1 -left-1 w-6 h-6 border-t-4 border-l-4 border-[#F3DC8A] rounded-tl-lg" />
              <div className="absolute -top-1 -right-1 w-6 h-6 border-t-4 border-r-4 border-[#F3DC8A] rounded-tr-lg" />
              <div className="absolute -bottom-1 -left-1 w-6 h-6 border-b-4 border-l-4 border-[#F3DC8A] rounded-bl-lg" />
              <div className="absolute -bottom-1 -right-1 w-6 h-6 border-b-4 border-r-4 border-[#F3DC8A] rounded-br-lg" />
              <div className="absolute left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-[#F3DC8A] to-transparent animate-bounce opacity-80" />
            </div>
          </div>
        )}

        {cameraError && (
          <div className="p-4 text-center text-xs text-amber-200 bg-amber-950/70 rounded-2xl max-w-sm m-4 border border-amber-500/50">
            <p className="font-bold mb-1">تنبيه الكاميرا:</p>
            <p>{cameraError}</p>
          </div>
        )}
      </div>

      {/* Scan Outcome Alert Box */}
      {currentOutcome && (
        <div
          className={`p-4 md:p-5 rounded-2xl border-2 transition-all shadow-xl ${
            currentOutcome.status === 'SUCCESS'
              ? 'bg-emerald-950/90 border-emerald-400 text-emerald-100'
              : currentOutcome.status === 'ALREADY_CHECKED_IN'
              ? 'bg-amber-950/90 border-amber-400 text-amber-100'
              : 'bg-rose-950/90 border-rose-500 text-rose-100'
          }`}
        >
          <div className="flex items-start gap-3">
            <div className="mt-1">
              {currentOutcome.status === 'SUCCESS' && (
                <CheckCircle2 size={36} className="text-emerald-400 animate-pulse" />
              )}
              {currentOutcome.status === 'ALREADY_CHECKED_IN' && (
                <AlertTriangle size={36} className="text-amber-400" />
              )}
              {(currentOutcome.status === 'WRONG_EVENT' || currentOutcome.status === 'UNKNOWN') && (
                <XCircle size={36} className="text-rose-400" />
              )}
            </div>

            <div className="flex-1 space-y-1">
              <h3 className="font-black text-base md:text-lg">
                {currentOutcome.status === 'SUCCESS' && strings.scanSuccessTitle}
                {currentOutcome.status === 'ALREADY_CHECKED_IN' && strings.scanDuplicateTitle}
                {currentOutcome.status === 'WRONG_EVENT' && strings.scanWrongEventTitle}
                {currentOutcome.status === 'UNKNOWN' && strings.scanUnknownTitle}
              </h3>

              {currentOutcome.guest && (
                <div className="text-sm font-bold text-[#FFF6D8]">
                  <span className="block text-base">{currentOutcome.guest.name}</span>
                  <span className="text-xs text-[#F3DC8A] font-mono">
                    #{currentOutcome.guest.sequenceNumber} · كود {currentOutcome.guest.code}
                  </span>
                </div>
              )}

              {currentOutcome.firstCheckInTimeMillis && (
                <p className="text-xs text-amber-200">
                  {strings.firstEntryTimeLabel}{' '}
                  <span className="font-bold underline">
                    {new Date(currentOutcome.firstCheckInTimeMillis).toLocaleTimeString('ar-EG', {
                      hour: '2-digit',
                      minute: '2-digit',
                      second: '2-digit',
                    })}
                  </span>
                </p>
              )}

              {currentOutcome.wrongEventName && (
                <p className="text-xs text-rose-200">
                  الدعوة تخص: <strong className="underline">{currentOutcome.wrongEventName}</strong>
                </p>
              )}

              <div className="pt-2">
                <button
                  onClick={handleNextScan}
                  className="py-2 px-4 rounded-xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-xs shadow hover:brightness-105 cursor-pointer flex items-center gap-1.5"
                >
                  <RotateCcw size={14} />
                  <span>{strings.scanNextBtn}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Manual Code Input Form */}
      <form onSubmit={handleManualSubmit} className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-4 shadow-md space-y-2">
        <label className="block text-xs font-bold text-[#F3DC8A]">
          {strings.enterCodeManuallyLabel}
        </label>
        <div className="flex gap-2">
          <input
            type="text"
            value={manualCode}
            onChange={e => setManualCode(e.target.value)}
            placeholder={`eb:${selectedEventId}:12345678`}
            className="flex-1 bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] font-mono focus:outline-none focus:border-[#D4AF37]"
          />
          <button
            type="submit"
            className="px-4 py-2 bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black rounded-xl text-xs shadow hover:brightness-105 cursor-pointer"
          >
            {strings.verifyCodeBtn}
          </button>
        </div>
      </form>

      {/* Test simulation button */}
      {unattended && (
        <div className="p-3 bg-[#082d22] border border-[#D4AF37]/30 rounded-2xl flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-[#9DB8AD]">
            <Sparkles size={14} className="text-[#D4AF37]" />
            <span>تجربة مسح ضيف سريع: <strong className="text-[#FFF6D8]">{unattended.name}</strong></span>
          </div>
          <button
            type="button"
            onClick={() => handleProcessBarcode(`eb:${selectedEventId}:${unattended.code}`)}
            className="px-3 py-1 bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/50 rounded-xl font-bold hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer"
          >
            مسح الآن
          </button>
        </div>
      )}

      {/* Recent scan log */}
      {scanHistory.length > 0 && (
        <div className="bg-[#082d22] border border-[#D4AF37]/30 rounded-2xl p-4 shadow-md space-y-2">
          <h4 className="text-xs font-bold text-[#F3DC8A] flex items-center gap-1.5">
            <Volume2 size={13} />
            <span>سجل آخر عمليات المسح اللحظية:</span>
          </h4>
          <div className="space-y-1.5 max-h-36 overflow-y-auto">
            {scanHistory.map((item, idx) => (
              <div
                key={idx}
                className="text-xs p-2 rounded-xl bg-[#052018] flex items-center justify-between border border-[#D4AF37]/20"
              >
                <div className="flex items-center gap-2 truncate">
                  <span className="font-bold text-[#FFF6D8]">{item.guest?.name || item.rawBarcode}</span>
                  {item.guest && <span className="text-[10px] text-[#F3DC8A] font-mono">#{item.guest.sequenceNumber}</span>}
                </div>
                <span className={`text-[11px] font-bold ${item.status === 'SUCCESS' ? 'text-emerald-400' : item.status === 'ALREADY_CHECKED_IN' ? 'text-amber-400' : 'text-rose-400'}`}>
                  {item.status === 'SUCCESS' ? 'حضر ✅' : item.status === 'ALREADY_CHECKED_IN' ? 'مكرر ⛔' : 'خطأ ❌'}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useEffect, useRef, useState, useCallback } from 'react';
import {
  X,
  Camera,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  RotateCcw,
  Volume2,
  Sparkles,
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { EventEntity, ScanOutcome } from '../types';
import { storageService } from '../services/storage';
import { soundEffects } from '../services/sound';

interface ScannerModalProps {
  event: EventEntity;
  isOpen: boolean;
  onClose: () => void;
  onScanOutcome?: (outcome: ScanOutcome) => void;
}

export const ScannerModal: React.FC<ScannerModalProps> = ({
  event,
  isOpen,
  onClose,
  onScanOutcome,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [manualCode, setManualCode] = useState('');
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [continuousScan, setContinuousScan] = useState(true);
  const [currentOutcome, setCurrentOutcome] = useState<ScanOutcome | null>(null);
  const scanningRef = useRef(false);
  const streamRef = useRef<MediaStream | null>(null);

  const handleProcessBarcode = useCallback((barcode: string) => {
    if (scanningRef.current) return;
    scanningRef.current = true;

    const outcome = storageService.verifyBarcode(event.id, barcode);
    setCurrentOutcome(outcome);

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

    if (onScanOutcome) {
      onScanOutcome(outcome);
    }
  }, [event.id, onScanOutcome]);

  // Start Camera
  const startCamera = useCallback(async () => {
    setCameraError(null);
    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        setCameraError('الكاميرا غير مدعومة في هذا المتصفح. يمكنك إدخال الكود يدوياً.');
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
      console.warn('Camera access denied or failed:', err);
      setCameraError('تعذر فتح الكاميرا (يرجى السماح بصلاحية الكاميرا أو استخدام الإدخال اليدوي).');
      setCameraActive(false);
    }
  }, []);

  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    setCameraActive(false);
  }, []);

  // Frame scanning loop using BarcodeDetector if available
  useEffect(() => {
    if (!isOpen) {
      stopCamera();
      return;
    }

    startCamera();

    let scanInterval: any;
    const hasBarcodeDetector = typeof window !== 'undefined' && 'BarcodeDetector' in window;

    if (hasBarcodeDetector) {
      try {
        const detector = new (window as any).BarcodeDetector({ formats: ['qr_code'] });
        scanInterval = setInterval(async () => {
          if (!videoRef.current || !cameraActive || scanningRef.current) return;
          try {
            const barcodes = await detector.detect(videoRef.current);
            if (barcodes.length > 0) {
              const raw = barcodes[0].rawValue;
              if (raw) {
                handleProcessBarcode(raw);
              }
            }
          } catch {}
        }, 300);
      } catch {}
    }

    return () => {
      clearInterval(scanInterval);
      stopCamera();
    };
  }, [isOpen, cameraActive, startCamera, stopCamera, handleProcessBarcode]);

  if (!isOpen) return null;

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

  const guests = storageService.getGuestsForEvent(event.id);
  const sampleUnattendedGuest = guests.find(g => !g.attended);

  return (
    <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-3 md:p-6 overflow-y-auto">
      <div className="bg-[#082d22] border-2 border-[#D4AF37] rounded-3xl max-w-xl w-full p-4 md:p-6 shadow-2xl relative flex flex-col gap-4">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[#D4AF37]/30 pb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-[#D4AF37]/20 rounded-xl text-[#F3DC8A]">
              <Camera size={22} />
            </div>
            <div>
              <h2 className="text-base md:text-lg font-black text-[#FFF6D8]">
                مسح الباركود عند الباب
              </h2>
              <p className="text-xs text-[#9DB8AD]">{event.name}</p>
            </div>
          </div>
          <button
            onClick={() => {
              stopCamera();
              onClose();
            }}
            className="p-1.5 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30"
          >
            <X size={18} />
          </button>
        </div>

        {/* Video / Camera Box */}
        <div className="relative aspect-video w-full rounded-2xl overflow-hidden bg-black border-2 border-[#D4AF37]/50 shadow-inner flex items-center justify-center">
          <video
            ref={videoRef}
            playsInline
            muted
            className={`w-full h-full object-cover ${cameraActive ? 'block' : 'hidden'}`}
          />

          {/* Scanner Overlay Frame */}
          {cameraActive && !currentOutcome && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-48 h-48 md:w-56 md:h-56 border-2 border-emerald-400 rounded-2xl relative shadow-[0_0_30px_rgba(16,185,129,0.3)]">
                {/* Gold corner brackets */}
                <div className="absolute -top-1 -left-1 w-6 h-6 border-t-4 border-l-4 border-[#F3DC8A] rounded-tl-lg" />
                <div className="absolute -top-1 -right-1 w-6 h-6 border-t-4 border-r-4 border-[#F3DC8A] rounded-tr-lg" />
                <div className="absolute -bottom-1 -left-1 w-6 h-6 border-b-4 border-l-4 border-[#F3DC8A] rounded-bl-lg" />
                <div className="absolute -bottom-1 -right-1 w-6 h-6 border-b-4 border-r-4 border-[#F3DC8A] rounded-br-lg" />

                {/* Laser scan line animation */}
                <div className="absolute left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-[#F3DC8A] to-transparent animate-bounce opacity-80" />
              </div>
            </div>
          )}

          {cameraError && (
            <div className="p-4 text-center text-xs text-amber-200 bg-amber-950/60 rounded-xl max-w-xs m-4">
              <p className="font-bold mb-1">تنبيه الكاميرا:</p>
              <p>{cameraError}</p>
            </div>
          )}
        </div>

        {/* Scan Result Feedback Modal/Card */}
        {currentOutcome && (
          <div
            className={`p-4 rounded-2xl border-2 transition-all shadow-xl ${
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
                  <CheckCircle2 size={32} className="text-emerald-400 animate-pulse" />
                )}
                {currentOutcome.status === 'ALREADY_CHECKED_IN' && (
                  <AlertTriangle size={32} className="text-amber-400" />
                )}
                {(currentOutcome.status === 'WRONG_EVENT' || currentOutcome.status === 'UNKNOWN') && (
                  <XCircle size={32} className="text-rose-400" />
                )}
              </div>

              <div className="flex-1 space-y-1">
                <h3 className="font-black text-base md:text-lg">
                  {currentOutcome.status === 'SUCCESS' && '✅ حضر الزائر — مسموح بالدخول'}
                  {currentOutcome.status === 'ALREADY_CHECKED_IN' && '⛔ تم تسجيل الحضور مسبقاً! (تكرار)'}
                  {currentOutcome.status === 'WRONG_EVENT' && '⚠️ دعوة لمناسبة أخرى!'}
                  {currentOutcome.status === 'UNKNOWN' && '❌ دعوة غير معروفة أو كود تالف!'}
                </h3>

                {currentOutcome.guest && (
                  <div className="text-sm font-bold mt-1 text-[#FFF6D8]">
                    <span className="block text-base">{currentOutcome.guest.name}</span>
                    <span className="text-xs text-[#F3DC8A] font-mono">
                      دعوة رقم #{currentOutcome.guest.sequenceNumber} · كود {currentOutcome.guest.code}
                    </span>
                  </div>
                )}

                {currentOutcome.firstCheckInTimeMillis && (
                  <p className="text-xs text-amber-200 mt-1">
                    وقت الدخول الأول:{' '}
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
                  <p className="text-xs text-rose-200 mt-1">
                    هذه الدعوة تخص: <span className="font-bold">{currentOutcome.wrongEventName}</span>
                  </p>
                )}

                <div className="pt-2 flex gap-2">
                  <button
                    onClick={handleNextScan}
                    className="py-2 px-4 rounded-xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-xs shadow hover:brightness-105 cursor-pointer flex items-center gap-1.5"
                  >
                    <RotateCcw size={14} />
                    <span>مسح الدعوة التالية</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Manual Code Entry Form */}
        <form onSubmit={handleManualSubmit} className="space-y-2">
          <label className="block text-xs font-bold text-[#F3DC8A]">
            إدخال كود الدعوة يدوياً (مثال: eb:{event.id}:12345678 أو كود 8 أرقام):
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={manualCode}
              onChange={e => setManualCode(e.target.value)}
              placeholder={`eb:${event.id}:12345678`}
              className="flex-1 bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] font-mono focus:outline-none focus:border-[#D4AF37]"
            />
            <button
              type="submit"
              className="px-4 py-2 bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black rounded-xl text-xs shadow hover:brightness-105 cursor-pointer"
            >
              فحص وتسجيل
            </button>
          </div>
        </form>

        {/* Quick Simulation Button for Testing */}
        {sampleUnattendedGuest && (
          <div className="p-2.5 rounded-xl bg-[#052018] border border-[#D4AF37]/30 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 text-[#9DB8AD]">
              <Sparkles size={14} className="text-[#D4AF37]" />
              <span>تجربة مسح ضيف سريع: <strong className="text-[#FFF6D8]">{sampleUnattendedGuest.name}</strong></span>
            </div>
            <button
              type="button"
              onClick={() => handleProcessBarcode(`eb:${event.id}:${sampleUnattendedGuest.code}`)}
              className="px-2.5 py-1 bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/50 rounded-lg font-bold hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer text-[11px]"
            >
              محاكاة المسح
            </button>
          </div>
        )}

        {/* Footer info */}
        <div className="flex items-center justify-between text-xs text-[#9DB8AD] border-t border-[#D4AF37]/20 pt-2">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={continuousScan}
              onChange={e => setContinuousScan(e.target.checked)}
              className="accent-[#D4AF37] rounded"
            />
            <span>المسح المتتابع المستمر</span>
          </label>
          <span className="flex items-center gap-1 text-[#F3DC8A]">
            <Volume2 size={13} />
            <span>صوت الترحيب مفعل</span>
          </span>
        </div>
      </div>
    </div>
  );
};

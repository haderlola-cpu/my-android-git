import React, { useState } from 'react';
import {
  ShieldCheck,
  QrCode,
  Search,
  CheckCircle2,
  Calendar,
  LogOut,
  Clock,
  Sparkles,
} from 'lucide-react';
import { AppPreferences, GuestEntity } from '../types';
import { storageService } from '../services/storage';
import { getAppStrings } from '../services/catalog';
import { ScannerModal } from '../components/ScannerModal';

interface ReceptionHostScreenProps {
  preferences: AppPreferences;
  onExitReception: () => void;
}

export const ReceptionHostScreen: React.FC<ReceptionHostScreenProps> = ({
  preferences,
  onExitReception,
}) => {
  const strings = getAppStrings(preferences.languageCode);
  const events = storageService.getEventsWithCounts();

  const [selectedEventId, setSelectedEventId] = useState<number>(() => {
    return preferences.receptionEventId || events[0]?.event.id || 1;
  });

  const [searchTerm, setSearchTerm] = useState('');
  const [isScannerOpen, setIsScannerOpen] = useState(false);

  const currentEventWithCount = events.find(e => e.event.id === selectedEventId);
  const currentEvent = currentEventWithCount?.event;
  const guests = currentEvent ? storageService.getGuestsForEvent(currentEvent.id) : [];

  const filteredGuests = guests.filter(g =>
    g.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    g.code.includes(searchTerm) ||
    (g.phone && g.phone.includes(searchTerm))
  );

  return (
    <div className="space-y-4 pb-28 max-w-2xl mx-auto">
      {/* Reception Gatekeeper Banner */}
      <div className="bg-gradient-to-r from-emerald-950 via-[#0B3D2E] to-emerald-950 border-2 border-emerald-500/60 rounded-3xl p-5 shadow-xl flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-2xl bg-emerald-500 text-black flex items-center justify-center font-black shadow-lg">
            <ShieldCheck size={26} />
          </div>
          <div>
            <h2 className="text-lg font-black text-emerald-100 flex items-center gap-2">
              <span>{strings.receptionTitle}</span>
              <span className="text-[10px] bg-emerald-500/30 text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-400/40">
                وضع البوابة
              </span>
            </h2>
            <p className="text-xs text-emerald-300/80">
              واجهة مخصصة لمسؤولي الاستقبال لفحص تذاكر الحضور وتسجيل الدخول السريع
            </p>
          </div>
        </div>

        <button
          onClick={onExitReception}
          className="p-2.5 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-emerald-500/40 text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 shrink-0"
        >
          <LogOut size={15} />
          <span className="hidden sm:inline">خروج للإدارة</span>
        </button>
      </div>

      {/* Event Selection & Live Counter */}
      <div className="bg-[#082d22] border border-[#D4AF37]/50 rounded-2xl p-4 shadow-lg space-y-3">
        <label className="block text-xs font-bold text-[#F3DC8A]">
          المناسبة الحالية المسؤولة عن استقبالها:
        </label>
        <div className="flex items-center gap-2 bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2">
          <Calendar size={16} className="text-[#D4AF37]" />
          <select
            value={selectedEventId}
            onChange={e => setSelectedEventId(parseInt(e.target.value, 10))}
            className="w-full bg-transparent text-[#FFF6D8] font-bold text-sm focus:outline-none cursor-pointer"
          >
            {events.map(ev => (
              <option key={ev.event.id} value={ev.event.id} className="bg-[#052018]">
                {ev.event.name} ({ev.attendedGuests} / {ev.totalGuests} حضروا)
              </option>
            ))}
          </select>
        </div>

        {/* Big Door Scanner Launch Button */}
        {currentEvent && (
          <button
            onClick={() => setIsScannerOpen(true)}
            className="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-base shadow-xl hover:brightness-105 active:scale-98 transition-all flex items-center justify-center gap-3 cursor-pointer"
          >
            <QrCode size={24} />
            <span>فتح قارئ باركود الكاميرا الآن</span>
          </button>
        )}
      </div>

      {/* Guest Fast Lookup and Manual Check-in */}
      <div className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-4 shadow-lg space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-[#FFF6D8]">
            البحث والتسجيل اليدوي عند تعذر قراءة الباركود:
          </h3>
          {currentEventWithCount && (
            <span className="text-xs font-bold text-emerald-400">
              {currentEventWithCount.attendedGuests} / {currentEventWithCount.totalGuests} حضروا
            </span>
          )}
        </div>

        <div className="relative">
          <Search size={16} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9DB8AD] pointer-events-none rtl:right-3.5 rtl:left-auto ltr:left-3.5 ltr:right-auto" />
          <input
            type="text"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder="ابحث باسم الضيف أو كود الدعوة..."
            className="w-full bg-[#052018] border border-[#D4AF37]/30 rounded-xl py-2 px-9 text-xs text-[#FFF6D8] placeholder-[#9DB8AD]/60 focus:outline-none focus:border-[#D4AF37]"
          />
        </div>

        {/* Filtered Guests in Reception mode */}
        <div className="space-y-2 max-h-80 overflow-y-auto">
          {filteredGuests.slice(0, 15).map(guest => (
            <div
              key={guest.id}
              className={`p-3 rounded-xl border flex items-center justify-between gap-3 transition-all ${
                guest.attended
                  ? 'bg-emerald-950/40 border-emerald-500/40'
                  : 'bg-[#052018] border-[#D4AF37]/20 hover:border-[#D4AF37]/50'
              }`}
            >
              <div className="w-7 h-7 rounded-lg bg-[#082d22] border border-[#D4AF37]/30 flex items-center justify-center font-mono font-bold text-xs text-[#F3DC8A] shrink-0">
                #{guest.sequenceNumber}
              </div>

              <div className="flex-1 min-w-0">
                <h4 className="text-xs font-bold text-[#FFF6D8] truncate">{guest.name}</h4>
                <p className="text-[10px] text-[#9DB8AD] font-mono">
                  كود: <strong className="text-[#F3DC8A]">{guest.code}</strong>
                  {guest.attended && guest.attendedAt && (
                    <span className="text-emerald-400 font-bold ms-2">
                      (وصل {new Date(guest.attendedAt).toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' })})
                    </span>
                  )}
                </p>
              </div>

              <button
                onClick={() => storageService.toggleAttendance(guest.id)}
                className={`py-1.5 px-3 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1 ${
                  guest.attended
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/50'
                    : 'bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/50 hover:bg-[#D4AF37] hover:text-[#052018]'
                }`}
              >
                <CheckCircle2 size={13} className={guest.attended ? 'text-emerald-400' : 'text-inherit'} />
                <span>{guest.attended ? 'تم الحضور' : 'تسجيل دخول'}</span>
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Scanner Modal */}
      {currentEvent && (
        <ScannerModal
          event={currentEvent}
          isOpen={isScannerOpen}
          onClose={() => setIsScannerOpen(false)}
        />
      )}
    </div>
  );
};

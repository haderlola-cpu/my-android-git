import React, { useState } from 'react';
import {
  Users,
  Search,
  CheckCircle2,
  Calendar,
  MessageSquare,
  Eye,
  Filter,
} from 'lucide-react';
import { AppPreferences, GuestEntity } from '../types';
import { storageService } from '../services/storage';
import { getAppStrings } from '../services/catalog';
import { getWhatsAppShareUrl } from '../services/whatsapp';
import { CardPreview } from '../components/CardPreview';
import { downloadGuestCardImage } from '../services/export';

interface GuestsAllScreenProps {
  preferences: AppPreferences;
  onOpenEvent: (eventId: number) => void;
}

export const GuestsAllScreen: React.FC<GuestsAllScreenProps> = ({
  preferences,
  onOpenEvent,
}) => {
  const strings = getAppStrings(preferences.languageCode);
  const events = storageService.getEventsWithCounts();
  const allGuests = storageService.getAllGuests();

  const [searchTerm, setSearchTerm] = useState('');
  const [selectedEventId, setSelectedEventId] = useState<number | 'all'>('all');
  const [statusFilter, setStatusFilter] = useState<'all' | 'attended' | 'notArrived'>('all');
  const [previewGuest, setPreviewGuest] = useState<{ guest: GuestEntity; eventId: number } | null>(null);

  const filteredGuests = allGuests.filter(g => {
    if (selectedEventId !== 'all' && g.eventId !== selectedEventId) return false;
    if (statusFilter === 'attended' && !g.attended) return false;
    if (statusFilter === 'notArrived' && g.attended) return false;

    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      return (
        g.name.toLowerCase().includes(term) ||
        g.code.includes(term) ||
        (g.phone && g.phone.includes(term))
      );
    }
    return true;
  });

  const targetEventForPreview = previewGuest
    ? storageService.getEvent(previewGuest.eventId)
    : null;

  return (
    <div className="space-y-4 pb-28">
      {/* Top Header */}
      <div className="bg-[#082d22] border border-[#D4AF37]/50 rounded-2xl p-4 shadow-lg space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-[#D4AF37]/20 rounded-xl text-[#F3DC8A]">
              <Users size={20} />
            </div>
            <div>
              <h2 className="text-base font-black text-[#FFF6D8]">{strings.guestsTitle}</h2>
              <p className="text-xs text-[#9DB8AD]">
                إجمالي {allGuests.length} مدعو عبر جميع المناسبات
              </p>
            </div>
          </div>
        </div>

        {/* Search input */}
        <div className="relative">
          <Search size={16} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9DB8AD] pointer-events-none rtl:right-3.5 rtl:left-auto ltr:left-3.5 ltr:right-auto" />
          <input
            type="text"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder="بحث باسم الضيف، كود الدعوة، أو رقم الهاتف..."
            className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl py-2 px-9 text-xs text-[#FFF6D8] placeholder-[#9DB8AD]/60 focus:outline-none focus:border-[#D4AF37]"
          />
        </div>

        {/* Filters Row */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2 text-xs">
          {/* Event Selector */}
          <div className="flex items-center gap-1.5 flex-1 bg-[#052018] border border-[#D4AF37]/30 rounded-xl px-2.5 py-1.5">
            <Filter size={14} className="text-[#D4AF37] shrink-0" />
            <select
              value={selectedEventId}
              onChange={e => setSelectedEventId(e.target.value === 'all' ? 'all' : parseInt(e.target.value, 10))}
              className="w-full bg-transparent text-[#FFF6D8] focus:outline-none text-xs cursor-pointer font-bold"
            >
              <option value="all" className="bg-[#052018]">جميع المناسبات ({allGuests.length})</option>
              {events.map(ev => (
                <option key={ev.event.id} value={ev.event.id} className="bg-[#052018]">
                  {ev.event.name} ({ev.totalGuests})
                </option>
              ))}
            </select>
          </div>

          {/* Status chips */}
          <div className="flex items-center gap-1.5">
            <button
              onClick={() => setStatusFilter('all')}
              className={`py-1.5 px-3 rounded-xl font-bold transition-all ${
                statusFilter === 'all'
                  ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018]'
                  : 'bg-[#052018] text-[#9DB8AD] border border-[#D4AF37]/30'
              }`}
            >
              الكل
            </button>
            <button
              onClick={() => setStatusFilter('attended')}
              className={`py-1.5 px-3 rounded-xl font-bold transition-all ${
                statusFilter === 'attended'
                  ? 'bg-emerald-500 text-[#052018]'
                  : 'bg-[#052018] text-emerald-400 border border-emerald-500/30'
              }`}
            >
              حضر
            </button>
            <button
              onClick={() => setStatusFilter('notArrived')}
              className={`py-1.5 px-3 rounded-xl font-bold transition-all ${
                statusFilter === 'notArrived'
                  ? 'bg-amber-400 text-[#052018]'
                  : 'bg-[#052018] text-amber-300 border border-amber-400/30'
              }`}
            >
              لم يصل
            </button>
          </div>
        </div>
      </div>

      {/* Guest Directory List */}
      <div className="space-y-2.5">
        {filteredGuests.length === 0 ? (
          <div className="bg-[#082d22]/80 border border-[#D4AF37]/30 rounded-3xl p-8 text-center space-y-2">
            <Users size={32} className="mx-auto text-[#D4AF37]" />
            <h4 className="text-sm font-bold text-[#FFF6D8]">لا يوجد ضيوف مطابقون</h4>
          </div>
        ) : (
          filteredGuests.map(guest => {
            const ev = events.find(e => e.event.id === guest.eventId)?.event;

            return (
              <div
                key={guest.id}
                className="bg-[#0A382A]/90 hover:bg-[#0A382A] border border-[#D4AF37]/30 rounded-2xl p-3 flex items-center justify-between gap-3 shadow-md transition-all"
              >
                <div className="w-8 h-8 rounded-xl bg-[#052018] border border-[#D4AF37]/40 flex items-center justify-center font-mono font-bold text-xs text-[#F3DC8A] shrink-0">
                  #{guest.sequenceNumber}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <h4 className="text-sm font-bold text-[#FFF6D8] truncate">{guest.name}</h4>
                    {guest.attended && (
                      <span className="text-[10px] text-emerald-400 font-bold bg-emerald-950/60 border border-emerald-500/40 px-1.5 py-0.2 rounded-full">
                        حضر ✓
                      </span>
                    )}
                  </div>

                  <p className="text-[11px] text-[#9DB8AD] flex items-center gap-2 flex-wrap">
                    {ev && (
                      <span
                        onClick={() => onOpenEvent(ev.id)}
                        className="text-[#F3DC8A] hover:underline cursor-pointer font-semibold"
                      >
                        {ev.name}
                      </span>
                    )}
                    <span>· كود: <strong className="font-mono text-[#FFF6D8]">{guest.code}</strong></span>
                    {guest.phone && <span>· 📱 {guest.phone}</span>}
                  </p>
                </div>

                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    onClick={() => storageService.toggleAttendance(guest.id)}
                    className={`py-1.5 px-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1 shadow-xs ${
                      guest.attended
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/50'
                        : 'bg-[#052018] text-[#9DB8AD] border border-[#D4AF37]/30 hover:border-[#D4AF37] hover:text-[#FFF6D8]'
                    }`}
                  >
                    <CheckCircle2 size={13} className={guest.attended ? 'text-emerald-400' : 'text-inherit'} />
                    <span>{guest.attended ? 'حضر' : 'تسجيل'}</span>
                  </button>

                  {ev && (
                    <a
                      href={getWhatsAppShareUrl(ev, guest)}
                      target="_blank"
                      rel="noreferrer"
                      onClick={() => storageService.setWhatsappSent(guest.id, true)}
                      className="p-2 rounded-xl bg-[#052018] text-[#25D366] border border-[#25D366]/40 hover:bg-[#25D366]/20 transition-all cursor-pointer"
                    >
                      <MessageSquare size={14} />
                    </a>
                  )}

                  <button
                    onClick={() => setPreviewGuest({ guest, eventId: guest.eventId })}
                    className="p-2 rounded-xl bg-[#052018] text-[#F3DC8A] border border-[#D4AF37]/40 hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer"
                  >
                    <Eye size={14} />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Preview Card Modal */}
      {previewGuest && targetEventForPreview && (
        <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-[#082d22] border-2 border-[#D4AF37] rounded-3xl max-w-md w-full p-5 shadow-2xl relative flex flex-col items-center gap-4">
            <div className="w-full flex items-center justify-between border-b border-[#D4AF37]/30 pb-2">
              <h3 className="text-base font-black text-[#FFF6D8]">معاينة بطاقة الدعوة</h3>
              <button
                onClick={() => setPreviewGuest(null)}
                className="p-1 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30"
              >
                ✕
              </button>
            </div>

            <CardPreview event={targetEventForPreview} guest={previewGuest.guest} interactive={false} />

            <div className="w-full flex gap-2 pt-2">
              <button
                onClick={() => downloadGuestCardImage(targetEventForPreview, previewGuest.guest, false)}
                className="flex-1 py-2.5 px-4 rounded-xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-xs shadow hover:brightness-105 cursor-pointer"
              >
                تحميل الصورة
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

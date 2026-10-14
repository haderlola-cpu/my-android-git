import React, { useState } from 'react';
import {
  Calendar,
  Search,
  Plus,
  QrCode,
  Edit,
  Trash2,
  Users,
  CheckCircle2,
  Clock,
  MapPin,
} from 'lucide-react';
import { EventWithCounts, AppPreferences } from '../types';
import { getAppStrings } from '../services/catalog';

interface HomeScreenProps {
  events: EventWithCounts[];
  preferences: AppPreferences;
  onOpenEvent: (eventId: number) => void;
  onCreateNewEvent: () => void;
  onEditDesign: (eventId: number) => void;
  onQuickScan: (eventId: number) => void;
  onDeleteEvent: (eventId: number) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  events,
  preferences,
  onOpenEvent,
  onCreateNewEvent,
  onEditDesign,
  onQuickScan,
  onDeleteEvent,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [deleteCandidateId, setDeleteCandidateId] = useState<number | null>(null);
  const strings = getAppStrings(preferences.languageCode);

  const filteredEvents = events.filter(e =>
    e.event.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (e.event.venueName && e.event.venueName.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="space-y-4 pb-24">
      {/* Search & New Event Header */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="relative flex-1">
          <Search size={18} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9DB8AD] pointer-events-none rtl:right-3.5 rtl:left-auto ltr:left-3.5 ltr:right-auto" />
          <input
            type="text"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            placeholder={strings.searchEventsPlaceholder}
            className="w-full bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl py-2.5 px-10 text-sm text-[#FFF6D8] placeholder-[#9DB8AD]/60 focus:outline-none focus:border-[#D4AF37] focus:ring-1 focus:ring-[#D4AF37] shadow-inner"
          />
        </div>

        <button
          onClick={onCreateNewEvent}
          className="py-2.5 px-4 bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black rounded-2xl text-sm shadow-md hover:brightness-105 active:scale-98 transition-all flex items-center justify-center gap-2 cursor-pointer shrink-0"
        >
          <Plus size={18} strokeWidth={3} />
          <span>{strings.newEventButton}</span>
        </button>
      </div>

      {/* Events List */}
      {filteredEvents.length === 0 ? (
        <div className="bg-[#082d22]/80 border border-[#D4AF37]/30 rounded-3xl p-8 text-center space-y-3">
          <Calendar size={44} className="mx-auto text-[#D4AF37]" />
          <h3 className="text-base font-bold text-[#FFF6D8]">{strings.noEventsTitle}</h3>
          <p className="text-xs text-[#9DB8AD]">{strings.noEventsSubtitle}</p>
        </div>
      ) : (
        <div className="space-y-3.5">
          {filteredEvents.map(({ event, totalGuests, attendedGuests }) => {
            const pct = totalGuests > 0 ? Math.round((attendedGuests * 100) / totalGuests) : 0;

            return (
              <div
                key={event.id}
                className="bg-[#0A382A]/90 hover:bg-[#0A382A] border border-[#D4AF37]/40 hover:border-[#D4AF37] rounded-3xl p-4 md:p-5 shadow-lg transition-all"
              >
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  {/* Left: Event Details */}
                  <div
                    onClick={() => onOpenEvent(event.id)}
                    className="flex-1 cursor-pointer space-y-1.5"
                  >
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-[#052018] border border-[#D4AF37]/40 text-[#F3DC8A]">
                        {event.dayName || 'المناسبة'} · {event.date}
                      </span>
                      <span className="text-xs text-[#9DB8AD] flex items-center gap-1">
                        <Clock size={12} className="text-[#D4AF37]" />
                        <span>{event.timeFrom} - {event.timeTo}</span>
                      </span>
                    </div>

                    <h3 className="text-lg font-black text-[#FFF6D8] hover:text-[#F3DC8A] transition-colors leading-snug">
                      {event.name}
                    </h3>

                    {event.venueName && (
                      <p className="text-xs text-[#9DB8AD] flex items-center gap-1">
                        <MapPin size={13} className="text-[#D4AF37]" />
                        <span className="truncate">{event.venueName}</span>
                      </p>
                    )}

                    {/* Attendance Mini Bar */}
                    <div className="pt-1.5 flex items-center gap-3">
                      <div className="flex-1 max-w-xs h-2 bg-[#052018] rounded-full overflow-hidden border border-[#D4AF37]/20">
                        <div
                          className="h-full bg-gradient-to-r from-[#D4AF37] to-emerald-400 rounded-full transition-all duration-500"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <span className="text-xs font-bold text-emerald-400 whitespace-nowrap">
                        {attendedGuests} / {totalGuests} ({pct}%)
                      </span>
                    </div>
                  </div>

                  {/* Right: Quick Action Buttons */}
                  <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                    <button
                      onClick={() => onQuickScan(event.id)}
                      className="p-2.5 rounded-xl bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/50 hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer shadow-sm flex items-center gap-1.5 text-xs font-bold"
                      title={strings.scanDoorBtn}
                    >
                      <QrCode size={16} />
                      <span className="hidden md:inline">{strings.quickScanBtn}</span>
                    </button>

                    <button
                      onClick={() => onEditDesign(event.id)}
                      className="p-2.5 rounded-xl bg-[#0B3D2E] text-[#FFF6D8] border border-[#D4AF37]/30 hover:border-[#D4AF37] transition-all cursor-pointer shadow-sm"
                      title={strings.editDesignTitle}
                    >
                      <Edit size={16} />
                    </button>

                    <button
                      onClick={() => setDeleteCandidateId(event.id)}
                      className="p-2.5 rounded-xl bg-[#0B3D2E] text-rose-400 border border-rose-900/40 hover:bg-rose-950/70 transition-all cursor-pointer shadow-sm"
                      title={strings.delete}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteCandidateId !== null && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#082d22] border-2 border-rose-500/80 rounded-3xl max-w-sm w-full p-5 shadow-2xl space-y-4">
            <h3 className="text-base font-black text-[#FFF6D8]">حذف المناسبة</h3>
            <p className="text-xs text-[#9DB8AD] leading-relaxed">
              هل أنت متأكد من حذف هذه المناسبة وجميع دعوات الضيوف المرتبطة بها؟
            </p>
            <div className="flex gap-2.5 pt-2">
              <button
                onClick={() => {
                  onDeleteEvent(deleteCandidateId);
                  setDeleteCandidateId(null);
                }}
                className="flex-1 py-2.5 px-4 rounded-xl bg-rose-600 text-white font-bold text-xs shadow hover:bg-rose-700 cursor-pointer"
              >
                {strings.delete}
              </button>
              <button
                onClick={() => setDeleteCandidateId(null)}
                className="py-2.5 px-4 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 text-xs font-bold cursor-pointer"
              >
                {strings.cancel}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

import React from 'react';
import {
  Calendar,
  Users,
  CheckCircle2,
  Plus,
  QrCode,
  ChevronLeft,
  ChevronRight,
  Clock,
  Sparkles,
} from 'lucide-react';
import { EventWithCounts, AppPreferences } from '../types';
import { getAppStrings } from '../services/catalog';

interface DashboardScreenProps {
  events: EventWithCounts[];
  preferences: AppPreferences;
  onOpenEvent: (eventId: number) => void;
  onCreateNewEvent: () => void;
  onQuickScanEvent: (eventId: number) => void;
  onViewAllEvents: () => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  events,
  preferences,
  onOpenEvent,
  onCreateNewEvent,
  onQuickScanEvent,
  onViewAllEvents,
}) => {
  const isAr = preferences.languageCode === 'ar';
  const strings = getAppStrings(preferences.languageCode);

  const totalGuests = events.reduce((sum, e) => sum + e.totalGuests, 0);
  const totalAttended = events.reduce((sum, e) => sum + e.attendedGuests, 0);

  // Pick next upcoming event
  const now = new Date();
  now.setHours(0, 0, 0, 0);

  const upcomingEvents = events
    .map(e => {
      const parts = e.event.date.split('-');
      const d = parts.length === 3 ? new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2])) : new Date(e.event.date);
      const diffTime = d.getTime() - now.getTime();
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      return { eventWithCounts: e, diffDays };
    })
    .filter(item => !isNaN(item.diffDays));

  upcomingEvents.sort((a, b) => a.diffDays - b.diffDays);
  const nextEventItem = upcomingEvents.find(item => item.diffDays >= 0) || upcomingEvents[0];
  const next = nextEventItem ? nextEventItem.eventWithCounts : events[0];
  const daysUntilNext = nextEventItem ? nextEventItem.diffDays : null;

  const nextPct = next && next.totalGuests > 0 ? Math.round((next.attendedGuests * 100) / next.totalGuests) : 0;

  const monthsAr = ['يناير', 'فبراير', 'مارس', 'أبريل', 'مايو', 'يونيو', 'يوليو', 'أغسطس', 'سبتمبر', 'أكتوبر', 'نوفمبر', 'ديسمبر'];
  const monthsEn = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  const formatEventDate = (dateStr: string) => {
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      const monthIdx = parseInt(parts[1], 10) - 1;
      const day = parts[2];
      const monthName = isAr ? monthsAr[monthIdx] || '' : monthsEn[monthIdx] || '';
      return { day, monthName };
    }
    return { day: '—', monthName: '' };
  };

  return (
    <div className="space-y-5 pb-24">
      {/* 1. Next Upcoming Event Hero Card with Circular Attendance Ring */}
      <div className="bg-gradient-to-br from-[#0B3D2E] to-[#052018] border-2 border-[#D4AF37]/60 rounded-3xl p-5 md:p-6 shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 -mr-10 -mt-10 w-40 h-40 bg-[#D4AF37]/10 rounded-full blur-2xl pointer-events-none" />

        {next ? (
          <div
            onClick={() => onOpenEvent(next.event.id)}
            className="flex flex-col md:flex-row items-center gap-5 cursor-pointer group"
          >
            {/* Circular Progress Ring */}
            <div className="relative w-24 h-24 shrink-0 flex items-center justify-center">
              <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                <circle
                  cx="50"
                  cy="50"
                  r="42"
                  fill="transparent"
                  stroke="#082d22"
                  strokeWidth="8"
                />
                <circle
                  cx="50"
                  cy="50"
                  r="42"
                  fill="transparent"
                  stroke="url(#goldGradient)"
                  strokeWidth="8"
                  strokeDasharray="264"
                  strokeDashoffset={264 - (264 * nextPct) / 100}
                  strokeLinecap="round"
                  className="transition-all duration-1000 ease-out"
                />
                <defs>
                  <linearGradient id="goldGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#F3DC8A" />
                    <stop offset="100%" stopColor="#D4AF37" />
                  </linearGradient>
                </defs>
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                <span className="text-xl font-black text-[#F3DC8A]">{nextPct}%</span>
                <span className="text-[10px] text-[#9DB8AD] leading-none">{strings.attendedLabel}</span>
              </div>
            </div>

            {/* Event Info */}
            <div className="flex-1 text-center md:text-start space-y-1">
              <div className="inline-flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-[#052018] border border-[#D4AF37]/40 text-[#F3DC8A] text-xs font-bold">
                <Sparkles size={12} />
                <span>
                  {daysUntilNext === null
                    ? strings.nextEventTitle
                    : daysUntilNext === 0
                    ? strings.eventTodayTag
                    : daysUntilNext < 0
                    ? strings.pastEventTag
                    : `${strings.nextEventTitle} · ${strings.daysRemainingFormat(daysUntilNext)}`}
                </span>
              </div>

              <h2 className="text-xl md:text-2xl font-black text-[#FFF6D8] group-hover:text-[#F3DC8A] transition-colors leading-snug">
                {next.event.name}
              </h2>

              <p className="text-xs md:text-sm text-[#9DB8AD] flex items-center justify-center md:justify-start gap-3">
                <span className="flex items-center gap-1">
                  <Calendar size={13} className="text-[#D4AF37]" />
                  <span>{next.event.date}</span>
                </span>
                <span className="flex items-center gap-1">
                  <Clock size={13} className="text-[#D4AF37]" />
                  <span>{next.event.timeFrom}</span>
                </span>
              </p>

              <p className="text-xs text-[#FFF6D8]/80 pt-1">
                {strings.attendedLabel}: <strong className="text-emerald-400 font-bold">{next.attendedGuests}</strong> / {next.totalGuests} مدعو
              </p>
            </div>

            {/* Quick scan icon button */}
            <div className="hidden md:flex flex-col items-center gap-2">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onQuickScanEvent(next.event.id);
                }}
                className="p-3 bg-gradient-to-br from-[#F3DC8A] to-[#D4AF37] text-[#052018] rounded-2xl shadow-lg hover:scale-105 active:scale-95 transition-all"
                title={strings.quickScanBtn}
              >
                <QrCode size={24} />
              </button>
              <span className="text-[10px] text-[#F3DC8A] font-bold">{strings.quickScanBtn}</span>
            </div>
          </div>
        ) : (
          <div className="py-8 text-center space-y-3">
            <Calendar size={40} className="mx-auto text-[#D4AF37]" />
            <h3 className="text-lg font-bold text-[#FFF6D8]">{strings.noEventsTitle}</h3>
            <p className="text-xs text-[#9DB8AD] max-w-sm mx-auto">{strings.noEventsSubtitle}</p>
          </div>
        )}
      </div>

      {/* 2. Key Performance Indicators (KPIs) */}
      <div className="grid grid-cols-3 gap-3">
        <div className="bg-[#0A382A]/90 border border-[#D4AF37]/40 rounded-2xl p-3.5 text-center shadow-md">
          <Calendar size={20} className="mx-auto text-[#D4AF37] mb-1" />
          <div className="text-2xl font-black text-[#FFF6D8]">{events.length}</div>
          <div className="text-[11px] text-[#9DB8AD]">{strings.eventsTitle}</div>
        </div>

        <div className="bg-[#0A382A]/90 border border-[#D4AF37]/40 rounded-2xl p-3.5 text-center shadow-md">
          <Users size={20} className="mx-auto text-[#D4AF37] mb-1" />
          <div className="text-2xl font-black text-[#FFF6D8]">{totalGuests}</div>
          <div className="text-[11px] text-[#9DB8AD]">{strings.guestsLabel}</div>
        </div>

        <div className="bg-[#0A382A]/90 border border-emerald-500/40 rounded-2xl p-3.5 text-center shadow-md">
          <CheckCircle2 size={20} className="mx-auto text-emerald-400 mb-1" />
          <div className="text-2xl font-black text-emerald-400">{totalAttended}</div>
          <div className="text-[11px] text-[#9DB8AD]">{strings.attendedLabel}</div>
        </div>
      </div>

      {/* 3. Quick Action Buttons */}
      <div className="grid grid-cols-2 gap-3">
        <button
          onClick={onCreateNewEvent}
          className="py-3 px-4 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-sm shadow-lg hover:brightness-105 active:scale-98 transition-all flex items-center justify-center gap-2 cursor-pointer"
        >
          <Plus size={18} strokeWidth={3} />
          <span>{strings.newEventButton}</span>
        </button>

        <button
          onClick={() => next && onQuickScanEvent(next.event.id)}
          disabled={!next}
          className="py-3 px-4 rounded-2xl bg-[#082d22] text-[#F3DC8A] border border-[#D4AF37]/60 font-bold text-sm shadow hover:bg-[#0B3D2E] active:scale-98 transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
        >
          <QrCode size={18} className="text-[#D4AF37]" />
          <span>{strings.quickScanBtn}</span>
        </button>
      </div>

      {/* 4. Recent Events List */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-extrabold text-[#FFF6D8] flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-[#D4AF37]" />
            <span>{strings.eventsTitle}</span>
          </h3>
          <button
            onClick={onViewAllEvents}
            className="text-xs text-[#F3DC8A] hover:underline font-bold cursor-pointer"
          >
            {strings.viewAllBtn} ({events.length})
          </button>
        </div>

        <div className="space-y-2.5">
          {events.slice(0, 4).map(e => {
            const { day, monthName } = formatEventDate(e.event.date);
            const pct = e.totalGuests > 0 ? Math.round((e.attendedGuests * 100) / e.totalGuests) : 0;

            return (
              <div
                key={e.event.id}
                onClick={() => onOpenEvent(e.event.id)}
                className="bg-[#0A382A]/80 hover:bg-[#0A382A] border border-[#D4AF37]/30 hover:border-[#D4AF37] rounded-2xl p-3 flex items-center justify-between gap-3 shadow-md transition-all cursor-pointer group"
              >
                {/* Date Badge */}
                <div className="w-14 text-center shrink-0 border-e border-[#D4AF37]/30 pe-3">
                  <div className="text-xl font-black text-[#F3DC8A] leading-none">{day}</div>
                  <div className="text-[10px] text-[#9DB8AD] mt-0.5">{monthName}</div>
                </div>

                {/* Event Name & Counts */}
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm font-bold text-[#FFF6D8] truncate group-hover:text-[#F3DC8A] transition-colors">
                    {e.event.name}
                  </h4>
                  <p className="text-xs text-[#9DB8AD] flex items-center gap-2 mt-0.5">
                    <span>{e.totalGuests} {strings.guestsLabel}</span>
                    <span>·</span>
                    <span className="text-emerald-400 font-semibold">{pct}% {strings.attendedLabel}</span>
                  </p>
                </div>

                {/* Arrow */}
                <div className="text-[#D4AF37] group-hover:translate-x-1 rtl:group-hover:-translate-x-1 transition-transform">
                  {isAr ? <ChevronLeft size={20} /> : <ChevronRight size={20} />}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

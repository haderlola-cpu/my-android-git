import React, { useState } from 'react';
import {
  Users,
  CheckCircle2,
  Clock,
  Plus,
  QrCode,
  Share2,
  Download,
  FileSpreadsheet,
  Edit,
  MessageSquare,
  Search,
  CheckSquare,
  Square,
  Trash2,
  ExternalLink,
  ChevronLeft,
  Eye,
  Send,
  Sparkles,
} from 'lucide-react';
import { EventEntity, GuestEntity, AppPreferences } from '../types';
import { storageService } from '../services/storage';
import { getAppStrings } from '../services/catalog';
import { AddGuestsModal } from '../components/AddGuestsModal';
import { ScannerModal } from '../components/ScannerModal';
import { WhatsAppTemplateModal } from '../components/WhatsAppTemplateModal';
import { CardPreview } from '../components/CardPreview';
import { getWhatsAppShareUrl } from '../services/whatsapp';
import { downloadGuestCardImage, exportAllGuestsZip, exportAttendanceCsv } from '../services/export';

interface EventDetailScreenProps {
  eventId: number;
  preferences: AppPreferences;
  onEditDesign: () => void;
  onBack: () => void;
}

export const EventDetailScreen: React.FC<EventDetailScreenProps> = ({
  eventId,
  preferences,
  onEditDesign,
  onBack,
}) => {
  const strings = getAppStrings(preferences.languageCode);
  const event = storageService.getEvent(eventId);
  const guests = storageService.getGuestsForEvent(eventId);

  const [searchTerm, setSearchTerm] = useState('');
  const [filter, setFilter] = useState<'all' | 'attended' | 'notArrived' | 'whatsappSent'>('all');
  const [isAddGuestsOpen, setIsAddGuestsOpen] = useState(false);
  const [isScannerOpen, setIsScannerOpen] = useState(false);
  const [isTemplateOpen, setIsTemplateOpen] = useState(false);
  const [previewGuest, setPreviewGuest] = useState<GuestEntity | null>(null);

  // Selection mode
  const [selectMode, setSelectMode] = useState(false);
  const [selectedGuestIds, setSelectedGuestIds] = useState<Set<number>>(new Set());

  // ZIP progress
  const [zipProgress, setZipProgress] = useState<{ current: number; total: number } | null>(null);

  // Sequential WhatsApp queue modal
  const [sequentialQueue, setSequentialQueue] = useState<GuestEntity[] | null>(null);
  const [currentSeqIndex, setCurrentSeqIndex] = useState(0);

  if (!event) {
    return (
      <div className="p-8 text-center space-y-4">
        <p className="text-sm text-rose-300">لم يتم العثور على المناسبة.</p>
        <button
          onClick={onBack}
          className="px-4 py-2 bg-[#D4AF37] text-[#052018] rounded-xl font-bold"
        >
          {strings.back}
        </button>
      </div>
    );
  }

  const totalGuests = guests.length;
  const attendedCount = guests.filter(g => g.attended).length;
  const notArrivedCount = totalGuests - attendedCount;
  const attendancePct = totalGuests > 0 ? Math.round((attendedCount * 100) / totalGuests) : 0;

  // Filter guests
  const filteredGuests = guests.filter(g => {
    const matchesSearch =
      g.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      g.code.includes(searchTerm) ||
      (g.phone && g.phone.includes(searchTerm));

    if (!matchesSearch) return false;

    if (filter === 'attended') return g.attended;
    if (filter === 'notArrived') return !g.attended;
    if (filter === 'whatsappSent') return g.whatsappSent;
    return true;
  });

  const handleToggleSelect = (id: number) => {
    const next = new Set(selectedGuestIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelectedGuestIds(next);
  };

  const handleSelectAll = () => {
    if (selectedGuestIds.size === filteredGuests.length) {
      setSelectedGuestIds(new Set());
    } else {
      setSelectedGuestIds(new Set(filteredGuests.map(g => g.id)));
    }
  };

  const handleDeleteSelected = () => {
    if (confirm(`هل أنت متأكد من حذف ${selectedGuestIds.size} مدعو؟`)) {
      selectedGuestIds.forEach(id => storageService.deleteGuest(id));
      setSelectedGuestIds(new Set());
      setSelectMode(false);
    }
  };

  const handleDownloadAllZip = async () => {
    setZipProgress({ current: 0, total: guests.length });
    try {
      await exportAllGuestsZip(event, guests, (curr, tot) => {
        setZipProgress({ current: curr, total: tot });
      });
    } finally {
      setZipProgress(null);
    }
  };

  const startSequentialWhatsApp = () => {
    const pendingGuests = guests.filter(g => !g.whatsappSent && g.phone);
    if (pendingGuests.length === 0) {
      alert('تم إرسال واتساب لجميع الضيوف الذين يملكون أرقام هواتف مسجلة! ✨');
      return;
    }
    setSequentialQueue(pendingGuests);
    setCurrentSeqIndex(0);
  };

  const handleSendNextSequential = () => {
    if (!sequentialQueue) return;
    const currentGuest = sequentialQueue[currentSeqIndex];
    if (currentGuest) {
      storageService.setWhatsappSent(currentGuest.id, true);
      const url = getWhatsAppShareUrl(event, currentGuest);
      window.open(url, '_blank');

      if (currentSeqIndex + 1 < sequentialQueue.length) {
        setCurrentSeqIndex(prev => prev + 1);
      } else {
        alert(strings.allGuestsSentMsg);
        setSequentialQueue(null);
      }
    }
  };

  return (
    <div className="space-y-5 pb-28">
      {/* 1. Header Overview Card */}
      <div className="bg-gradient-to-br from-[#0B3D2E] to-[#052018] border-2 border-[#D4AF37]/60 rounded-3xl p-5 md:p-6 shadow-xl relative overflow-hidden">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-[#052018] border border-[#D4AF37]/50 text-[#F3DC8A]">
                {event.dayName} · {event.date}
              </span>
              <span className="text-xs text-[#9DB8AD] flex items-center gap-1">
                <Clock size={12} className="text-[#D4AF37]" />
                <span>{event.timeFrom} - {event.timeTo}</span>
              </span>
            </div>

            <h1 className="text-2xl md:text-3xl font-black text-[#FFF6D8] leading-tight">
              {event.name}
            </h1>

            {event.venueName && (
              <p className="text-xs md:text-sm text-[#9DB8AD]">
                📍 {event.venueName} {event.venueAddress ? `· ${event.venueAddress}` : ''}
              </p>
            )}
          </div>

          {/* Quick attendance circle */}
          <div className="flex items-center gap-4 self-center md:self-auto bg-[#052018]/80 p-3 rounded-2xl border border-[#D4AF37]/30">
            <div className="text-center">
              <div className="text-2xl font-black text-emerald-400">{attendedCount}</div>
              <div className="text-[10px] text-[#9DB8AD]">{strings.attendedStat}</div>
            </div>
            <div className="h-8 w-px bg-[#D4AF37]/30" />
            <div className="text-center">
              <div className="text-2xl font-black text-amber-300">{notArrivedCount}</div>
              <div className="text-[10px] text-[#9DB8AD]">{strings.notArrivedStat}</div>
            </div>
            <div className="h-8 w-px bg-[#D4AF37]/30" />
            <div className="text-center">
              <div className="text-2xl font-black text-[#F3DC8A]">{attendancePct}%</div>
              <div className="text-[10px] text-[#9DB8AD]">النسبة</div>
            </div>
          </div>
        </div>

        {/* Attendance Progress bar */}
        <div className="mt-4 pt-3 border-t border-[#D4AF37]/20 flex items-center gap-3">
          <div className="flex-1 h-2.5 bg-[#052018] rounded-full overflow-hidden border border-[#D4AF37]/30">
            <div
              className="h-full bg-gradient-to-r from-[#D4AF37] to-emerald-400 transition-all duration-500 rounded-full"
              style={{ width: `${attendancePct}%` }}
            />
          </div>
          <span className="text-xs font-bold text-[#F3DC8A] whitespace-nowrap">
            {attendedCount} / {totalGuests} {strings.guestsLabel}
          </span>
        </div>
      </div>

      {/* 2. Primary Actions Toolbar */}
      <div className="flex flex-wrap items-center gap-2">
        <button
          onClick={() => setIsAddGuestsOpen(true)}
          className="py-2.5 px-4 bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black rounded-2xl text-xs md:text-sm shadow-md hover:brightness-105 active:scale-98 transition-all flex items-center gap-1.5 cursor-pointer"
        >
          <Plus size={16} strokeWidth={3} />
          <span>{strings.addGuestsBtn}</span>
        </button>

        <button
          onClick={() => setIsScannerOpen(true)}
          className="py-2.5 px-4 bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/60 font-bold rounded-2xl text-xs md:text-sm shadow hover:bg-[#0E4B39] active:scale-98 transition-all flex items-center gap-1.5 cursor-pointer"
        >
          <QrCode size={16} className="text-[#D4AF37]" />
          <span>{strings.scanDoorBtn}</span>
        </button>

        <button
          onClick={startSequentialWhatsApp}
          className="py-2.5 px-3 bg-[#0B3D2E] text-[#25D366] border border-[#25D366]/40 font-bold rounded-2xl text-xs shadow hover:bg-[#0E4B39] active:scale-98 transition-all flex items-center gap-1.5 cursor-pointer"
          title="إرسال متتابع على واتساب لجميع الضيوف"
        >
          <Send size={15} />
          <span>{strings.sequentialWhatsappBtn}</span>
        </button>

        <button
          onClick={handleDownloadAllZip}
          disabled={zipProgress !== null || guests.length === 0}
          className="py-2.5 px-3 bg-[#0B3D2E] text-[#FFF6D8] border border-[#D4AF37]/40 font-bold rounded-2xl text-xs shadow hover:bg-[#0E4B39] active:scale-98 transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          title="تحميل كل بطاقات الدعوة بصيغة ZIP"
        >
          <Download size={15} />
          <span>{zipProgress ? `جاري (${zipProgress.current}/${zipProgress.total})` : strings.downloadAllZipBtn}</span>
        </button>

        <button
          onClick={() => exportAttendanceCsv(event, guests)}
          className="py-2.5 px-3 bg-[#0B3D2E] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 font-bold rounded-2xl text-xs shadow hover:bg-[#0E4B39] transition-all flex items-center gap-1.5 cursor-pointer"
          title="تصدير كشف الحضور إلى ملف CSV"
        >
          <FileSpreadsheet size={15} />
          <span className="hidden sm:inline">CSV</span>
        </button>

        <button
          onClick={onEditDesign}
          className="py-2.5 px-3 bg-[#0B3D2E] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 font-bold rounded-2xl text-xs shadow hover:bg-[#0E4B39] transition-all flex items-center gap-1.5 cursor-pointer"
          title="تعديل تصميم الدعوة"
        >
          <Edit size={15} />
          <span className="hidden sm:inline">{strings.editDesignMenu}</span>
        </button>

        <button
          onClick={() => setIsTemplateOpen(true)}
          className="py-2.5 px-3 bg-[#0B3D2E] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 font-bold rounded-2xl text-xs shadow hover:bg-[#0E4B39] transition-all flex items-center gap-1.5 cursor-pointer"
          title="تعديل قالب رسالة واتساب"
        >
          <MessageSquare size={15} />
          <span className="hidden sm:inline">قالب واتساب</span>
        </button>
      </div>

      {/* 3. Search & Filter Bar */}
      <div className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-3 shadow-md space-y-3">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          <div className="relative flex-1">
            <Search size={16} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9DB8AD] pointer-events-none rtl:right-3.5 rtl:left-auto ltr:left-3.5 ltr:right-auto" />
            <input
              type="text"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              placeholder={strings.searchGuestPlaceholder}
              className="w-full bg-[#052018] border border-[#D4AF37]/30 rounded-xl py-2 px-9 text-xs text-[#FFF6D8] placeholder-[#9DB8AD]/60 focus:outline-none focus:border-[#D4AF37]"
            />
          </div>

          <div className="flex items-center gap-2 self-end sm:self-auto">
            <button
              onClick={() => {
                setSelectMode(!selectMode);
                setSelectedGuestIds(new Set());
              }}
              className={`py-1.5 px-3 rounded-xl border text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
                selectMode
                  ? 'bg-[#D4AF37] text-[#052018] border-[#F3DC8A]'
                  : 'bg-[#052018] text-[#9DB8AD] border-[#D4AF37]/30 hover:text-[#FFF6D8]'
              }`}
            >
              <CheckSquare size={14} />
              <span>{strings.selectModeBtn}</span>
            </button>
          </div>
        </div>

        {/* Filter Chips */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs font-bold">
          <button
            onClick={() => setFilter('all')}
            className={`py-1 px-3 rounded-xl border transition-all cursor-pointer whitespace-nowrap ${
              filter === 'all'
                ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] border-transparent shadow'
                : 'bg-[#052018] text-[#9DB8AD] border-[#D4AF37]/30 hover:text-[#FFF6D8]'
            }`}
          >
            {strings.filterAll} ({guests.length})
          </button>

          <button
            onClick={() => setFilter('attended')}
            className={`py-1 px-3 rounded-xl border transition-all cursor-pointer whitespace-nowrap ${
              filter === 'attended'
                ? 'bg-emerald-500 text-[#052018] border-emerald-400 font-extrabold shadow'
                : 'bg-[#052018] text-emerald-400 border-emerald-500/30 hover:border-emerald-500/60'
            }`}
          >
            {strings.filterAttended} ({attendedCount})
          </button>

          <button
            onClick={() => setFilter('notArrived')}
            className={`py-1 px-3 rounded-xl border transition-all cursor-pointer whitespace-nowrap ${
              filter === 'notArrived'
                ? 'bg-amber-400 text-[#052018] border-amber-300 font-extrabold shadow'
                : 'bg-[#052018] text-amber-300 border-amber-400/30 hover:border-amber-400/60'
            }`}
          >
            {strings.filterNotArrived} ({notArrivedCount})
          </button>

          <button
            onClick={() => setFilter('whatsappSent')}
            className={`py-1 px-3 rounded-xl border transition-all cursor-pointer whitespace-nowrap ${
              filter === 'whatsappSent'
                ? 'bg-[#25D366] text-black border-[#25D366] font-extrabold shadow'
                : 'bg-[#052018] text-[#25D366] border-[#25D366]/30 hover:border-[#25D366]/60'
            }`}
          >
            {strings.filterWhatsappSent} ({guests.filter(g => g.whatsappSent).length})
          </button>
        </div>

        {/* Multi-selection Actions Bar */}
        {selectMode && (
          <div className="pt-2 border-t border-[#D4AF37]/20 flex items-center justify-between gap-2 text-xs">
            <div className="flex items-center gap-2">
              <button
                onClick={handleSelectAll}
                className="py-1 px-2.5 rounded-lg bg-[#052018] border border-[#D4AF37]/40 text-[#F3DC8A] font-bold"
              >
                {selectedGuestIds.size === filteredGuests.length ? 'إلغاء التحديد' : 'تحديد الكل'}
              </button>
              <span className="text-[#9DB8AD]">
                تم تحديد: <strong className="text-[#FFF6D8]">{selectedGuestIds.size}</strong>
              </span>
            </div>

            {selectedGuestIds.size > 0 && (
              <button
                onClick={handleDeleteSelected}
                className="py-1 px-3 rounded-lg bg-rose-600/90 text-white font-bold flex items-center gap-1 shadow hover:bg-rose-700"
              >
                <Trash2 size={13} />
                <span>حذف المحدد</span>
              </button>
            )}
          </div>
        )}
      </div>

      {/* 4. Guest List Cards */}
      <div className="space-y-2.5">
        {filteredGuests.length === 0 ? (
          <div className="bg-[#082d22]/80 border border-[#D4AF37]/30 rounded-3xl p-8 text-center space-y-3">
            <Users size={36} className="mx-auto text-[#D4AF37]" />
            <h4 className="text-sm font-bold text-[#FFF6D8]">لا توجد دعوات مطابقة</h4>
            <p className="text-xs text-[#9DB8AD]">
              اضغط على إضافة ضيوف لتوليد دعوات وأكواد QR جديدة لهذه المناسبة.
            </p>
          </div>
        ) : (
          filteredGuests.map(guest => {
            const isSelected = selectedGuestIds.has(guest.id);

            return (
              <div
                key={guest.id}
                className={`bg-[#0A382A]/90 hover:bg-[#0A382A] border rounded-2xl p-3 md:p-3.5 flex items-center justify-between gap-3 shadow-md transition-all ${
                  isSelected ? 'border-[#D4AF37] ring-1 ring-[#D4AF37]' : 'border-[#D4AF37]/30'
                }`}
              >
                {/* Select Checkbox (if in select mode) */}
                {selectMode && (
                  <button
                    onClick={() => handleToggleSelect(guest.id)}
                    className="p-1 text-[#D4AF37] cursor-pointer"
                  >
                    {isSelected ? <CheckSquare size={18} /> : <Square size={18} />}
                  </button>
                )}

                {/* Sequence badge */}
                <div className="w-9 h-9 rounded-xl bg-[#052018] border border-[#D4AF37]/40 flex items-center justify-center font-mono font-bold text-xs text-[#F3DC8A] shrink-0">
                  #{guest.sequenceNumber}
                </div>

                {/* Guest info */}
                <div className="flex-1 min-w-0 space-y-0.5">
                  <div className="flex items-center gap-2">
                    <h4 className="text-sm font-bold text-[#FFF6D8] truncate">{guest.name}</h4>
                    {guest.whatsappSent && (
                      <span className="text-[10px] text-[#25D366] font-bold bg-[#25D366]/10 px-1.5 py-0.2 rounded">
                        واتساب ✓
                      </span>
                    )}
                  </div>

                  <p className="text-[11px] text-[#9DB8AD] flex items-center gap-2 flex-wrap">
                    <span className="font-mono text-[#F3DC8A]">كود: {guest.code}</span>
                    {guest.phone && <span>· 📱 {guest.phone}</span>}
                    {guest.attended && guest.attendedAt && (
                      <span className="text-emerald-400 font-bold">
                        · وصل {new Date(guest.attendedAt).toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    )}
                  </p>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-1.5 shrink-0">
                  {/* Attendance toggle button */}
                  <button
                    onClick={() => storageService.toggleAttendance(guest.id)}
                    className={`py-1.5 px-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1 shadow-xs ${
                      guest.attended
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/50 hover:bg-emerald-500/30'
                        : 'bg-[#052018] text-[#9DB8AD] border border-[#D4AF37]/30 hover:border-[#D4AF37] hover:text-[#FFF6D8]'
                    }`}
                    title={guest.attended ? strings.undoCheckInBtn : strings.checkInBtn}
                  >
                    <CheckCircle2 size={14} className={guest.attended ? 'text-emerald-400' : 'text-inherit'} />
                    <span>{guest.attended ? strings.attendedLabel : strings.checkInBtn}</span>
                  </button>

                  {/* WhatsApp button */}
                  <a
                    href={getWhatsAppShareUrl(event, guest)}
                    target="_blank"
                    rel="noreferrer"
                    onClick={() => storageService.setWhatsappSent(guest.id, true)}
                    className="p-2 rounded-xl bg-[#052018] text-[#25D366] border border-[#25D366]/40 hover:bg-[#25D366]/20 transition-all cursor-pointer"
                    title={strings.sendWhatsappBtn}
                  >
                    <MessageSquare size={15} />
                  </a>

                  {/* Preview / Download Card */}
                  <button
                    onClick={() => setPreviewGuest(guest)}
                    className="p-2 rounded-xl bg-[#052018] text-[#F3DC8A] border border-[#D4AF37]/40 hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer"
                    title={strings.viewCardBtn}
                  >
                    <Eye size={15} />
                  </button>

                  {/* Delete single guest */}
                  <button
                    onClick={() => {
                      if (confirm(`حذف دعوة ${guest.name}؟`)) {
                        storageService.deleteGuest(guest.id);
                      }
                    }}
                    className="p-2 rounded-xl bg-[#052018] text-rose-400/80 hover:text-rose-400 hover:bg-rose-950/40 border border-transparent transition-all cursor-pointer"
                    title={strings.delete}
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* MODAL: Single Guest Card Preview & Download */}
      {previewGuest && (
        <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-[#082d22] border-2 border-[#D4AF37] rounded-3xl max-w-md w-full p-5 shadow-2xl relative flex flex-col items-center gap-4">
            <div className="w-full flex items-center justify-between border-b border-[#D4AF37]/30 pb-2">
              <h3 className="text-base font-black text-[#FFF6D8]">بطاقة الدعوة الشخصية</h3>
              <button
                onClick={() => setPreviewGuest(null)}
                className="p-1 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30"
              >
                ✕
              </button>
            </div>

            <CardPreview event={event} guest={previewGuest} interactive={false} />

            <div className="w-full flex gap-2 pt-2">
              <button
                onClick={() => downloadGuestCardImage(event, previewGuest, preferences.originalQuality)}
                className="flex-1 py-2.5 px-4 rounded-xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-xs shadow hover:brightness-105 cursor-pointer flex items-center justify-center gap-1.5"
              >
                <Download size={15} />
                <span>تحميل كصورة PNG</span>
              </button>

              <a
                href={getWhatsAppShareUrl(event, previewGuest)}
                target="_blank"
                rel="noreferrer"
                onClick={() => storageService.setWhatsappSent(previewGuest.id, true)}
                className="py-2.5 px-4 rounded-xl bg-[#25D366] text-black font-black text-xs shadow hover:brightness-105 cursor-pointer flex items-center gap-1.5"
              >
                <MessageSquare size={15} />
                <span>إرسال واتساب</span>
              </a>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Sequential WhatsApp Sender */}
      {sequentialQueue && (
        <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-[#082d22] border-2 border-[#25D366] rounded-3xl max-w-sm w-full p-5 shadow-2xl text-center space-y-4">
            <div className="w-12 h-12 bg-[#25D366]/20 text-[#25D366] rounded-2xl flex items-center justify-center mx-auto border border-[#25D366]/50">
              <Send size={24} />
            </div>

            <div>
              <span className="text-xs text-[#F3DC8A] font-bold">
                ضيف {currentSeqIndex + 1} من {sequentialQueue.length}
              </span>
              <h3 className="text-lg font-black text-[#FFF6D8] mt-1">
                {sequentialQueue[currentSeqIndex]?.name}
              </h3>
              <p className="text-xs text-[#9DB8AD] font-mono mt-0.5">
                {sequentialQueue[currentSeqIndex]?.phone}
              </p>
            </div>

            <div className="flex gap-2 pt-2">
              <button
                onClick={handleSendNextSequential}
                className="flex-1 py-3 px-4 rounded-xl bg-[#25D366] text-black font-black text-xs shadow hover:brightness-105 cursor-pointer flex items-center justify-center gap-1.5"
              >
                <span>إرسال للتالي 💬</span>
              </button>
              <button
                onClick={() => {
                  if (currentSeqIndex + 1 < sequentialQueue.length) {
                    setCurrentSeqIndex(prev => prev + 1);
                  } else {
                    setSequentialQueue(null);
                  }
                }}
                className="py-3 px-3 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 text-xs font-bold"
              >
                تخطي
              </button>
              <button
                onClick={() => setSequentialQueue(null)}
                className="py-3 px-3 rounded-xl bg-[#052018] text-rose-300 border border-rose-900/40 text-xs font-bold"
              >
                إنهاء
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Guests Modal */}
      <AddGuestsModal
        eventId={event.id}
        isOpen={isAddGuestsOpen}
        onClose={() => setIsAddGuestsOpen(false)}
        onSuccess={() => {}}
      />

      {/* Scanner Modal */}
      <ScannerModal
        event={event}
        isOpen={isScannerOpen}
        onClose={() => setIsScannerOpen(false)}
      />

      {/* WhatsApp Template Modal */}
      <WhatsAppTemplateModal
        event={event}
        isOpen={isTemplateOpen}
        onClose={() => setIsTemplateOpen(false)}
        onSaved={() => {}}
      />
    </div>
  );
};

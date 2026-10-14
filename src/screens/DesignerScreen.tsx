import React, { useState } from 'react';
import {
  Save,
  Palette,
  Type,
  Layout,
  Sliders,
  Sparkles,
  Upload,
  Calendar,
  Clock,
  MapPin,
  Heart,
  ChevronDown,
} from 'lucide-react';
import { EventEntity, AppPreferences } from '../types';
import { CardPreview } from '../components/CardPreview';
import { PRESET_BACKGROUNDS, PRESET_COLORS_HEX, FONT_CHOICES, getAppStrings } from '../services/catalog';

interface DesignerScreenProps {
  existingEvent?: EventEntity | null;
  preferences: AppPreferences;
  onSave: (event: EventEntity) => void;
  onBack: () => void;
}

export const DesignerScreen: React.FC<DesignerScreenProps> = ({
  existingEvent,
  preferences,
  onSave,
  onBack,
}) => {
  const strings = getAppStrings(preferences.languageCode);

  const [activeTab, setActiveTab] = useState<'info' | 'background' | 'styling' | 'position'>('info');

  const [formData, setFormData] = useState<EventEntity>(() => {
    if (existingEvent) {
      return { ...existingEvent };
    }
    return {
      id: 0,
      name: 'حفل زفاف مبارك',
      date: new Date(Date.now() + 86400000 * 14).toISOString().split('T')[0],
      timeFrom: '08:00 م',
      timeTo: '12:00 ص',
      backgroundType: 'PRESET',
      backgroundPresetId: 0,
      backgroundAspectRatio: 0.714,
      nameColorHex: '#FFFDF6E2',
      qrColorHex: '#121826',
      qrBgColorHex: '#FFFFFF',
      fontIndex: 0,
      showGuestName: true,
      showDate: true,
      showTime: true,
      qrSizeRatio: 0.28,
      nameSizeSp: 22,
      qrPosX: 0.50,
      qrPosY: 0.46,
      namePosX: 0.50,
      namePosY: 0.70,
      datePosX: 0.50,
      datePosY: 0.81,
      whatsappTemplate: 'أهلًا {name} 🌹\nيسعدنا دعوتك إلى {event}\n{date} - {time}\nمرفق دعوتك الخاصة، برجاء إظهار الباركود عند الدخول. نتشرف بحضورك ✨',
      createdAt: Date.now(),
      groomName: 'العريس',
      brideName: 'العروس',
      headerVerse: 'وَمِنْ آيَاتِهِ أَنْ خَلَقَ لَكُم مِّنْ أَنفُسِكُمْ أَزْوَاجًا لِّتَسْكُنُوا إِلَيْهَا',
      hostsText: 'تتشرف العائلتان الكريمتان بدعوتكم لحضور حفل الزفاف المبارك',
      dayName: 'الخميس',
      venueName: 'قاعة رويال الكبرى',
      venueAddress: 'القاهرة، مصر',
      locationUrl: 'https://maps.google.com',
      showLocationQr: true,
      notesText: 'جنة الأطفال منازلهم',
      closingText: 'حضوركم يكتمل به سرورنا ✨',
      cloudKey: '',
      updatedAt: Date.now(),
    };
  });

  const updateField = (field: keyof EventEntity, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleCustomImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = ev => {
        if (ev.target?.result) {
          setFormData(prev => ({
            ...prev,
            backgroundType: 'IMAGE',
            backgroundUri: ev.target!.result as string,
          }));
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const handlePositionUpdates = (updates: any) => {
    setFormData(prev => ({ ...prev, ...updates }));
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <div className="pb-28">
      {/* Top action bar */}
      <div className="flex items-center justify-between pb-3 mb-4 border-b border-[#D4AF37]/30">
        <div>
          <h2 className="text-lg md:text-xl font-black text-[#FFF6D8]">
            {existingEvent ? strings.editDesignTitle : strings.createInvitationTitle}
          </h2>
          <p className="text-xs text-[#9DB8AD]">
            خصص تفاصيل المناسبة وتصميم الدعوة الفاخرة بالكامل مع معاينة فورية
          </p>
        </div>

        <button
          onClick={handleSave}
          className="py-2.5 px-5 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-sm shadow-xl hover:brightness-105 active:scale-98 transition-all flex items-center gap-2 cursor-pointer"
        >
          <Save size={18} strokeWidth={2.5} />
          <span>{strings.saveEventAndDesignBtn}</span>
        </button>
      </div>

      {/* Main Grid: Left/Top is Live Preview, Right/Bottom is Controls */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left 5 Cols: Live Card Preview */}
        <div className="lg:col-span-5 bg-[#082d22] border-2 border-[#D4AF37]/60 rounded-3xl p-4 md:p-5 shadow-2xl flex flex-col items-center sticky top-20">
          <div className="w-full flex items-center justify-between pb-3 mb-2 border-b border-[#D4AF37]/30 text-xs text-[#F3DC8A] font-bold">
            <span className="flex items-center gap-1.5">
              <Sparkles size={14} />
              <span>معاينة حية للدعوة (5:7)</span>
            </span>
            <span className="text-[10px] text-[#9DB8AD]">اسحب العناصر لتعديل المكان</span>
          </div>

          <CardPreview
            event={formData}
            onPositionChange={handlePositionUpdates}
            interactive={true}
          />
        </div>

        {/* Right 7 Cols: Control Tabs & Inputs */}
        <div className="lg:col-span-7 bg-[#082d22] border border-[#D4AF37]/50 rounded-3xl p-5 shadow-xl space-y-5">
          {/* Section Tabs */}
          <div className="grid grid-cols-4 gap-1.5 p-1 bg-[#052018] rounded-2xl border border-[#D4AF37]/30">
            <button
              type="button"
              onClick={() => setActiveTab('info')}
              className={`py-2 px-1 text-xs font-bold rounded-xl transition-all flex flex-col sm:flex-row items-center justify-center gap-1 ${
                activeTab === 'info'
                  ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] shadow'
                  : 'text-[#9DB8AD] hover:text-[#FFF6D8]'
              }`}
            >
              <Layout size={14} />
              <span>البيانات</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('background')}
              className={`py-2 px-1 text-xs font-bold rounded-xl transition-all flex flex-col sm:flex-row items-center justify-center gap-1 ${
                activeTab === 'background'
                  ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] shadow'
                  : 'text-[#9DB8AD] hover:text-[#FFF6D8]'
              }`}
            >
              <Palette size={14} />
              <span>الخلفية</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('styling')}
              className={`py-2 px-1 text-xs font-bold rounded-xl transition-all flex flex-col sm:flex-row items-center justify-center gap-1 ${
                activeTab === 'styling'
                  ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] shadow'
                  : 'text-[#9DB8AD] hover:text-[#FFF6D8]'
              }`}
            >
              <Type size={14} />
              <span>الألوان والخط</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('position')}
              className={`py-2 px-1 text-xs font-bold rounded-xl transition-all flex flex-col sm:flex-row items-center justify-center gap-1 ${
                activeTab === 'position'
                  ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] shadow'
                  : 'text-[#9DB8AD] hover:text-[#FFF6D8]'
              }`}
            >
              <Sliders size={14} />
              <span>المواضع</span>
            </button>
          </div>

          {/* TAB 1: Event Info */}
          {activeTab === 'info' && (
            <div className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
                  {strings.eventNameLabel} *
                </label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={e => updateField('name', e.target.value)}
                  className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                />
              </div>

              {/* Groom & Bride */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
                    {strings.groomNameLabel}
                  </label>
                  <input
                    type="text"
                    value={formData.groomName}
                    onChange={e => updateField('groomName', e.target.value)}
                    placeholder="اسم العريس"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
                    {strings.brideNameLabel}
                  </label>
                  <input
                    type="text"
                    value={formData.brideName}
                    onChange={e => updateField('brideName', e.target.value)}
                    placeholder="اسم العروس"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
              </div>

              {/* Verse & Hosts */}
              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
                  {strings.headerVerseLabel}
                </label>
                <input
                  type="text"
                  value={formData.headerVerse}
                  onChange={e => updateField('headerVerse', e.target.value)}
                  placeholder="وَمِنْ آيَاتِهِ أَنْ خَلَقَ لَكُم مِّنْ أَنفُسِكُمْ أَزْوَاجًا لِّتَسْكُنُوا إِلَيْهَا"
                  className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
                  {strings.hostsTextLabel}
                </label>
                <input
                  type="text"
                  value={formData.hostsText}
                  onChange={e => updateField('hostsText', e.target.value)}
                  placeholder="تتشرف عائلتا... بدعوتكم لحضور حفل الزفاف"
                  className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                />
              </div>

              {/* Date & Time */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2.5">
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.dayNameLabel}</label>
                  <input
                    type="text"
                    value={formData.dayName}
                    onChange={e => updateField('dayName', e.target.value)}
                    placeholder="الجمعة"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-xs text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.eventDateLabel}</label>
                  <input
                    type="date"
                    value={formData.date}
                    onChange={e => updateField('date', e.target.value)}
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-xs text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.timeFromLabel}</label>
                  <input
                    type="text"
                    value={formData.timeFrom}
                    onChange={e => updateField('timeFrom', e.target.value)}
                    placeholder="08:00 م"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-xs text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.timeToLabel}</label>
                  <input
                    type="text"
                    value={formData.timeTo}
                    onChange={e => updateField('timeTo', e.target.value)}
                    placeholder="12:00 ص"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-xs text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
              </div>

              {/* Venue & Location */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.venueNameLabel}</label>
                  <input
                    type="text"
                    value={formData.venueName}
                    onChange={e => updateField('venueName', e.target.value)}
                    placeholder="قاعة رويال بالاس"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.venueAddressLabel}</label>
                  <input
                    type="text"
                    value={formData.venueAddress}
                    onChange={e => updateField('venueAddress', e.target.value)}
                    placeholder="التجمع الخامس، القاهرة"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.locationUrlLabel}</label>
                <input
                  type="url"
                  value={formData.locationUrl}
                  onChange={e => updateField('locationUrl', e.target.value)}
                  placeholder="https://maps.google.com/?q=..."
                  className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                />
              </div>

              {/* Notes & Closing */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.notesLabel}</label>
                  <input
                    type="text"
                    value={formData.notesText}
                    onChange={e => updateField('notesText', e.target.value)}
                    placeholder="جنة الأطفال منازلهم"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-xs text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">{strings.closingTextLabel}</label>
                  <input
                    type="text"
                    value={formData.closingText}
                    onChange={e => updateField('closingText', e.target.value)}
                    placeholder="حضوركم يكتمل به سرورنا"
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-xs text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: Background Selection */}
          {activeTab === 'background' && (
            <div className="space-y-4">
              <h3 className="text-xs font-bold text-[#F3DC8A]">خلفيات جاهزة راقية مستوحاة من الهوية العربية:</h3>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                {PRESET_BACKGROUNDS.map(bg => {
                  const isSelected = formData.backgroundType === 'PRESET' && formData.backgroundPresetId === bg.id;

                  return (
                    <button
                      key={bg.id}
                      type="button"
                      onClick={() => {
                        setFormData(prev => ({
                          ...prev,
                          backgroundType: 'PRESET',
                          backgroundPresetId: bg.id,
                        }));
                      }}
                      className={`p-2.5 rounded-2xl border-2 text-start transition-all cursor-pointer relative overflow-hidden group ${
                        isSelected
                          ? 'border-[#D4AF37] shadow-lg shadow-[#D4AF37]/30 scale-[1.02]'
                          : 'border-[#D4AF37]/30 hover:border-[#D4AF37]/70'
                      }`}
                    >
                      {/* Swatch visual */}
                      <div
                        className="w-full h-16 rounded-xl mb-2 border border-white/20 shadow-inner"
                        style={{
                          background: bg.imageFallback
                            ? `url(${bg.imageFallback}) center/cover`
                            : `linear-gradient(135deg, ${bg.topColor}, ${bg.centerColor}, ${bg.bottomColor})`,
                        }}
                      />
                      <span className="text-xs font-bold text-[#FFF6D8] block truncate">
                        {bg.titleAr}
                      </span>
                      {isSelected && (
                        <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#D4AF37] ring-4 ring-[#D4AF37]/40" />
                      )}
                    </button>
                  );
                })}
              </div>

              {/* Custom Image Upload */}
              <div className="pt-3 border-t border-[#D4AF37]/30">
                <label className="block text-xs font-bold text-[#F3DC8A] mb-2">
                  أو رفع صورة خلفية مخصصة من جهازك:
                </label>
                <label className="flex items-center justify-center gap-2 p-4 bg-[#052018] border-2 border-dashed border-[#D4AF37]/50 rounded-2xl cursor-pointer hover:bg-[#0B3D2E]/60 transition-all text-xs text-[#FFF6D8] font-bold">
                  <Upload size={18} className="text-[#D4AF37]" />
                  <span>انقر لاختيار صورة (JPG, PNG, WebP)</span>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleCustomImageUpload}
                    className="hidden"
                  />
                </label>
              </div>
            </div>
          )}

          {/* TAB 3: Colors & Fonts */}
          {activeTab === 'styling' && (
            <div className="space-y-4">
              {/* Fonts */}
              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-2">
                  نوع الخط العربي / اللاتيني للدعوة:
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {FONT_CHOICES.map(f => {
                    const isSelected = formData.fontIndex === f.index;
                    return (
                      <button
                        key={f.index}
                        type="button"
                        onClick={() => updateField('fontIndex', f.index)}
                        style={{ fontFamily: f.cssFamily }}
                        className={`p-2.5 rounded-xl border text-sm transition-all text-start cursor-pointer ${
                          isSelected
                            ? 'bg-[#D4AF37] text-[#052018] font-bold border-[#F3DC8A] shadow-md'
                            : 'bg-[#052018] text-[#FFF6D8] border-[#D4AF37]/30 hover:border-[#D4AF37]'
                        }`}
                      >
                        {f.nameAr}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Text Colors */}
              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-2">
                  لون النصوص والاسم:
                </label>
                <div className="flex flex-wrap gap-2">
                  {PRESET_COLORS_HEX.map(color => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => updateField('nameColorHex', color)}
                      style={{ backgroundColor: color }}
                      className={`w-7 h-7 rounded-full border-2 transition-transform cursor-pointer ${
                        formData.nameColorHex === color
                          ? 'border-[#D4AF37] scale-125 ring-2 ring-[#D4AF37]/60'
                          : 'border-white/30 hover:scale-110'
                      }`}
                      title={color}
                    />
                  ))}
                  <input
                    type="color"
                    value={formData.nameColorHex}
                    onChange={e => updateField('nameColorHex', e.target.value)}
                    className="w-7 h-7 rounded-full border-0 cursor-pointer p-0 bg-transparent"
                    title="لون مخصص"
                  />
                </div>
              </div>

              {/* QR Code Foreground Color */}
              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-2">
                  لون الباركود QR:
                </label>
                <div className="flex flex-wrap gap-2">
                  {PRESET_COLORS_HEX.map(color => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => updateField('qrColorHex', color)}
                      style={{ backgroundColor: color }}
                      className={`w-7 h-7 rounded-full border-2 transition-transform cursor-pointer ${
                        formData.qrColorHex === color
                          ? 'border-[#D4AF37] scale-125 ring-2 ring-[#D4AF37]/60'
                          : 'border-white/30 hover:scale-110'
                      }`}
                      title={color}
                    />
                  ))}
                  <input
                    type="color"
                    value={formData.qrColorHex}
                    onChange={e => updateField('qrColorHex', e.target.value)}
                    className="w-7 h-7 rounded-full border-0 cursor-pointer p-0 bg-transparent"
                    title="لون مخصص"
                  />
                </div>
              </div>

              {/* QR Background Color */}
              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-2">
                  خلفية كارت الباركود QR:
                </label>
                <div className="flex flex-wrap gap-2">
                  {['#FFFFFF', '#FFF6D8', '#FBF7EE', '#052018', '#000000'].map(color => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => updateField('qrBgColorHex', color)}
                      style={{ backgroundColor: color }}
                      className={`w-7 h-7 rounded-full border-2 transition-transform cursor-pointer ${
                        formData.qrBgColorHex === color
                          ? 'border-[#D4AF37] scale-125 ring-2 ring-[#D4AF37]/60'
                          : 'border-white/30 hover:scale-110'
                      }`}
                      title={color}
                    />
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: Sizing & Positioning */}
          {activeTab === 'position' && (
            <div className="space-y-4">
              {/* Switches */}
              <div className="grid grid-cols-2 gap-3 p-3 bg-[#052018] rounded-2xl border border-[#D4AF37]/30 text-xs">
                <label className="flex items-center gap-2 cursor-pointer text-[#FFF6D8]">
                  <input
                    type="checkbox"
                    checked={formData.showGuestName}
                    onChange={e => updateField('showGuestName', e.target.checked)}
                    className="accent-[#D4AF37] rounded"
                  />
                  <span>إظهار اسم المدعو</span>
                </label>

                <label className="flex items-center gap-2 cursor-pointer text-[#FFF6D8]">
                  <input
                    type="checkbox"
                    checked={formData.showDate}
                    onChange={e => updateField('showDate', e.target.checked)}
                    className="accent-[#D4AF37] rounded"
                  />
                  <span>إظهار التاريخ</span>
                </label>

                <label className="flex items-center gap-2 cursor-pointer text-[#FFF6D8]">
                  <input
                    type="checkbox"
                    checked={formData.showTime}
                    onChange={e => updateField('showTime', e.target.checked)}
                    className="accent-[#D4AF37] rounded"
                  />
                  <span>إظهار الوقت</span>
                </label>

                <label className="flex items-center gap-2 cursor-pointer text-[#FFF6D8]">
                  <input
                    type="checkbox"
                    checked={formData.showLocationQr}
                    onChange={e => updateField('showLocationQr', e.target.checked)}
                    className="accent-[#D4AF37] rounded"
                  />
                  <span>باركود موقع القاعة</span>
                </label>
              </div>

              {/* Sliders */}
              <div className="space-y-3">
                <div>
                  <div className="flex justify-between text-xs font-bold text-[#F3DC8A] mb-1">
                    <span>حجم الباركود (QR Size):</span>
                    <span>{Math.round(formData.qrSizeRatio * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min={0.18}
                    max={0.42}
                    step={0.01}
                    value={formData.qrSizeRatio}
                    onChange={e => updateField('qrSizeRatio', parseFloat(e.target.value))}
                    className="w-full accent-[#D4AF37]"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs font-bold text-[#F3DC8A] mb-1">
                    <span>حجم اسم المدعو (Name Size):</span>
                    <span>{Math.round(formData.nameSizeSp)} sp</span>
                  </div>
                  <input
                    type="range"
                    min={14}
                    max={36}
                    step={1}
                    value={formData.nameSizeSp}
                    onChange={e => updateField('nameSizeSp', parseFloat(e.target.value))}
                    className="w-full accent-[#D4AF37]"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs font-bold text-[#F3DC8A] mb-1">
                    <span>موقع الباركود الرأسي (Y):</span>
                    <span>{Math.round(formData.qrPosY * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min={0.2}
                    max={0.7}
                    step={0.01}
                    value={formData.qrPosY}
                    onChange={e => updateField('qrPosY', parseFloat(e.target.value))}
                    className="w-full accent-[#D4AF37]"
                  />
                </div>

                <div>
                  <div className="flex justify-between text-xs font-bold text-[#F3DC8A] mb-1">
                    <span>موقع اسم المدعو الرأسي (Y):</span>
                    <span>{Math.round(formData.namePosY * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min={0.4}
                    max={0.88}
                    step={0.01}
                    value={formData.namePosY}
                    onChange={e => updateField('namePosY', parseFloat(e.target.value))}
                    className="w-full accent-[#D4AF37]"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Bottom Save Action */}
          <div className="pt-2 flex gap-3">
            <button
              onClick={handleSave}
              className="flex-1 py-3 px-4 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-sm shadow-xl hover:brightness-105 active:scale-98 transition-all flex items-center justify-center gap-2 cursor-pointer"
            >
              <Save size={18} strokeWidth={2.5} />
              <span>{strings.saveEventAndDesignBtn}</span>
            </button>
            <button
              onClick={onBack}
              className="py-3 px-5 rounded-2xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 text-sm font-bold cursor-pointer"
            >
              {strings.cancel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

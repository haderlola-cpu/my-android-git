import React, { useState } from 'react';
import { X, Users, Hash, Phone, AlertCircle, CheckCircle2 } from 'lucide-react';
import { storageService } from '../services/storage';
import { AddGuestsResult } from '../types';

interface AddGuestsModalProps {
  eventId: number;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (result: AddGuestsResult) => void;
}

export const AddGuestsModal: React.FC<AddGuestsModalProps> = ({
  eventId,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [activeTab, setActiveTab] = useState<'names' | 'sequence' | 'phones'>('names');
  const [namesText, setNamesText] = useState('');
  const [seqFrom, setSeqFrom] = useState(1);
  const [seqTo, setSeqTo] = useState(50);
  const [seqPrefix, setSeqPrefix] = useState('طاولة');
  const [phonesText, setPhonesText] = useState('');
  const [lastResult, setLastResult] = useState<AddGuestsResult | null>(null);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let res: AddGuestsResult;

    if (activeTab === 'names') {
      if (!namesText.trim()) return;
      res = storageService.addGuestsByNames(eventId, namesText);
    } else if (activeTab === 'sequence') {
      res = storageService.addGuestsBySequence(eventId, seqFrom, seqTo, seqPrefix);
    } else {
      if (!phonesText.trim()) return;
      res = storageService.addGuestsByPhones(eventId, phonesText);
    }

    setLastResult(res);
    onSuccess(res);
    if (res.skippedItems.length === 0) {
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-[#082d22] border-2 border-[#D4AF37] rounded-3xl max-w-lg w-full p-6 shadow-2xl relative max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[#D4AF37]/30 pb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-[#D4AF37]/20 rounded-xl text-[#F3DC8A]">
              <Users size={20} />
            </div>
            <h2 className="text-lg font-black text-[#FFF6D8]">إضافة الضيوف والدعوات</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 hover:border-[#D4AF37]"
          >
            <X size={18} />
          </button>
        </div>

        {/* Tab Buttons */}
        <div className="grid grid-cols-3 gap-2 mt-4 p-1 bg-[#052018] rounded-2xl border border-[#D4AF37]/30">
          <button
            type="button"
            onClick={() => setActiveTab('names')}
            className={`py-2 px-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'names'
                ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] shadow'
                : 'text-[#9DB8AD] hover:text-[#FFF6D8]'
            }`}
          >
            <Users size={14} />
            <span>بالأسماء</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('sequence')}
            className={`py-2 px-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'sequence'
                ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] shadow'
                : 'text-[#9DB8AD] hover:text-[#FFF6D8]'
            }`}
          >
            <Hash size={14} />
            <span>تسلسل أرقام</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('phones')}
            className={`py-2 px-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              activeTab === 'phones'
                ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] shadow'
                : 'text-[#9DB8AD] hover:text-[#FFF6D8]'
            }`}
          >
            <Phone size={14} />
            <span>أرقام موبايل</span>
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="mt-4 flex-1 overflow-y-auto space-y-4">
          {activeTab === 'names' && (
            <div>
              <label className="block text-xs font-bold text-[#F3DC8A] mb-1.5">
                اكتب أو الصق اسماً في كل سطر (واختيارياً رقم الهاتف بجانبه):
              </label>
              <textarea
                value={namesText}
                onChange={e => setNamesText(e.target.value)}
                placeholder={'أحمد محمد 01012345678\nمنى السيد\nكريم علي 01123456789\nالأستاذ خالد وعائلته'}
                rows={6}
                className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-2xl p-3 text-sm text-[#FFF6D8] placeholder-[#9DB8AD]/50 focus:outline-none focus:border-[#D4AF37] focus:ring-1 focus:ring-[#D4AF37]"
              />
              <p className="text-[11px] text-[#9DB8AD] mt-1">
                💡 سيتم إنشاء دعوة بكود باركود فريد 8 أرقام لكل اسم تلقائياً.
              </p>
            </div>
          )}

          {activeTab === 'sequence' && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">من رقم:</label>
                  <input
                    type="number"
                    min={1}
                    max={10000}
                    value={seqFrom}
                    onChange={e => setSeqFrom(parseInt(e.target.value, 10) || 1)}
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#F3DC8A] mb-1">إلى رقم:</label>
                  <input
                    type="number"
                    min={seqFrom}
                    max={seqFrom + 5000}
                    value={seqTo}
                    onChange={e => setSeqTo(parseInt(e.target.value, 10) || seqFrom)}
                    className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
                  بادئة قبل الرقم (اختياري مثل: طاولة أو دعوة):
                </label>
                <input
                  type="text"
                  value={seqPrefix}
                  onChange={e => setSeqPrefix(e.target.value)}
                  placeholder="مثال: طاولة، بطاقة"
                  className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl px-3 py-2 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
                />
              </div>

              <div className="p-3 bg-[#052018] rounded-xl border border-[#D4AF37]/30 text-xs text-[#FFF6D8] flex items-center justify-between">
                <span>إجمالي الدعوات التي ستتولد:</span>
                <span className="font-extrabold text-[#F3DC8A] text-sm">
                  {Math.max(0, seqTo - seqFrom + 1)} دعوة
                </span>
              </div>
            </div>
          )}

          {activeTab === 'phones' && (
            <div>
              <label className="block text-xs font-bold text-[#F3DC8A] mb-1.5">
                الصق أرقام الموبايل (رقم في كل سطر):
              </label>
              <textarea
                value={phonesText}
                onChange={e => setPhonesText(e.target.value)}
                placeholder={'01012345678\n01123456789\n01234567890'}
                rows={6}
                className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-2xl p-3 text-sm text-[#FFF6D8] placeholder-[#9DB8AD]/50 focus:outline-none focus:border-[#D4AF37]"
              />
              <p className="text-[11px] text-[#9DB8AD] mt-1">
                💡 سيتم إنشاء اسم تسلسلي لكل رقم موبايل تلقائياً مع كود باركود مستقل.
              </p>
            </div>
          )}

          {/* Feedback & skipped summary */}
          {lastResult && (
            <div className="p-3 rounded-2xl bg-[#052018] border border-[#D4AF37]/50 space-y-2">
              <div className="flex items-center gap-2 text-emerald-400 font-bold text-xs">
                <CheckCircle2 size={16} />
                <span>تمت إضافة {lastResult.addedCount} دعوة بنجاح!</span>
              </div>
              {lastResult.skippedItems.length > 0 && (
                <div className="text-amber-300 text-xs space-y-1">
                  <div className="flex items-center gap-1 font-bold">
                    <AlertCircle size={14} />
                    <span>تم تخطي {lastResult.skippedItems.length} عنصر (مكرر):</span>
                  </div>
                  <ul className="list-disc list-inside text-[11px] text-amber-200/80 max-h-24 overflow-y-auto">
                    {lastResult.skippedItems.map((item, idx) => (
                      <li key={idx}>
                        {item.inputLine} — {item.reasonAr}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Submit Button */}
          <div className="pt-2 flex gap-3">
            <button
              type="submit"
              className="flex-1 py-3 px-4 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-sm shadow-lg hover:brightness-105 active:scale-98 transition-all cursor-pointer"
            >
              إضافة وتوليد الدعوات الآن
            </button>
            <button
              type="button"
              onClick={onClose}
              className="py-3 px-4 rounded-2xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 text-sm font-bold cursor-pointer"
            >
              إغلاق
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

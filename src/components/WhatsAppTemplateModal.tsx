import React, { useState } from 'react';
import { X, MessageSquare, Sparkles } from 'lucide-react';
import { EventEntity } from '../types';
import { storageService } from '../services/storage';

interface WhatsAppTemplateModalProps {
  event: EventEntity;
  isOpen: boolean;
  onClose: () => void;
  onSaved: (updatedEvent: EventEntity) => void;
}

export const WhatsAppTemplateModal: React.FC<WhatsAppTemplateModalProps> = ({
  event,
  isOpen,
  onClose,
  onSaved,
}) => {
  const [template, setTemplate] = useState(
    event.whatsappTemplate ||
      'أهلًا {name} 🌹\nيسعدنا دعوتك إلى {event}\n{date} - {time}\nمرفق دعوتك الخاصة، برجاء إظهار الباركود عند الدخول. نتشرف بحضورك ✨'
  );

  if (!isOpen) return null;

  const placeholders = [
    { tag: '{name}', label: 'اسم الضيف' },
    { tag: '{event}', label: 'اسم المناسبة' },
    { tag: '{date}', label: 'التاريخ' },
    { tag: '{time}', label: 'الوقت' },
    { tag: '{venue}', label: 'اسم القاعة' },
    { tag: '{code}', label: 'كود الدعوة' },
  ];

  const insertPlaceholder = (tag: string) => {
    setTemplate(prev => prev + ' ' + tag);
  };

  const handleSave = () => {
    const updated = storageService.saveEvent({
      ...event,
      whatsappTemplate: template,
    });
    onSaved(updated);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-[#082d22] border-2 border-[#D4AF37] rounded-3xl max-w-lg w-full p-6 shadow-2xl relative">
        <div className="flex items-center justify-between border-b border-[#D4AF37]/30 pb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-[#25D366]/20 rounded-xl text-[#25D366]">
              <MessageSquare size={20} />
            </div>
            <h2 className="text-lg font-black text-[#FFF6D8]">قالب رسالة الترحيب على واتساب</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30"
          >
            <X size={18} />
          </button>
        </div>

        <div className="mt-4 space-y-3">
          <p className="text-xs text-[#9DB8AD]">
            انقر على المتغير لإضافته إلى نص الرسالة ليتم استبداله تلقائياً ببيانات كل ضيف:
          </p>

          <div className="flex flex-wrap gap-1.5">
            {placeholders.map(p => (
              <button
                key={p.tag}
                type="button"
                onClick={() => insertPlaceholder(p.tag)}
                className="px-2.5 py-1 rounded-xl bg-[#052018] border border-[#D4AF37]/40 text-[#F3DC8A] text-xs font-bold hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer flex items-center gap-1 shadow-sm"
              >
                <Sparkles size={11} />
                <span>{p.label} ({p.tag})</span>
              </button>
            ))}
          </div>

          <textarea
            value={template}
            onChange={e => setTemplate(e.target.value)}
            rows={6}
            className="w-full bg-[#052018] border border-[#D4AF37]/50 rounded-2xl p-3.5 text-sm text-[#FFF6D8] leading-relaxed focus:outline-none focus:border-[#D4AF37] focus:ring-1 focus:ring-[#D4AF37]"
          />

          <div className="flex gap-3 pt-2">
            <button
              onClick={handleSave}
              className="flex-1 py-3 px-4 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-sm shadow-lg hover:brightness-105 active:scale-98 transition-all cursor-pointer"
            >
              حفظ القالب
            </button>
            <button
              onClick={onClose}
              className="py-3 px-4 rounded-2xl bg-[#052018] text-[#9DB8AD] hover:text-[#FFF6D8] border border-[#D4AF37]/30 text-sm font-bold cursor-pointer"
            >
              إلغاء
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

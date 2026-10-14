import React, { useRef, useState } from 'react';
import {
  Settings,
  Languages,
  Download,
  Upload,
  Shield,
  FileJson,
  CheckCircle2,
  AlertCircle,
  Share2,
  LogOut,
  Moon,
  Sparkles,
} from 'lucide-react';
import { saveAs } from 'file-saver';
import { AppPreferences } from '../types';
import { storageService } from '../services/storage';
import { getAppStrings } from '../services/catalog';

interface SettingsScreenProps {
  preferences: AppPreferences;
  onUpdatePreferences: (updates: Partial<AppPreferences>) => void;
  onLogout: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  preferences,
  onUpdatePreferences,
  onLogout,
}) => {
  const strings = getAppStrings(preferences.languageCode);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [backupMessage, setBackupMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleExportBackup = () => {
    const json = storageService.exportBackupJson();
    const blob = new Blob([json], { type: 'application/json' });
    const fileName = `elbeit_elaraby_backup_${new Date().toISOString().split('T')[0]}.json`;
    saveAs(blob, fileName);
    setBackupMessage({
      type: 'success',
      text: preferences.languageCode === 'ar' ? 'تم تنزيل ملف النسخة الاحتياطية بنجاح.' : 'Backup JSON file exported successfully.',
    });
  };

  const handleImportBackup = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = ev => {
      const content = ev.target?.result as string;
      if (content) {
        const result = storageService.importBackupJson(content);
        setBackupMessage({
          type: result.success ? 'success' : 'error',
          text: preferences.languageCode === 'ar' ? result.messageAr : result.messageEn,
        });
      }
    };
    reader.readAsText(file);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleShareApp = () => {
    if (navigator.share) {
      navigator.share({
        title: strings.appName,
        text: strings.companySubtitle,
        url: window.location.href,
      }).catch(() => {});
    } else {
      navigator.clipboard.writeText(window.location.href);
      alert(preferences.languageCode === 'ar' ? 'تم نسخ رابط التطبيق للحافظة.' : 'App link copied to clipboard.');
    }
  };

  return (
    <div className="space-y-4 pb-28 max-w-2xl mx-auto">
      {/* Settings Header */}
      <div className="bg-[#082d22] border border-[#D4AF37]/50 rounded-2xl p-4 shadow-lg flex items-center gap-3">
        <div className="p-2 bg-[#D4AF37]/20 rounded-xl text-[#F3DC8A]">
          <Settings size={22} />
        </div>
        <div>
          <h2 className="text-base font-black text-[#FFF6D8]">{strings.settingsTitle}</h2>
          <p className="text-xs text-[#9DB8AD]">تفضيلات النظام، التصدير، والنسخ الاحتياطي</p>
        </div>
      </div>

      {/* Language Section */}
      <div className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-4 shadow-md space-y-3">
        <h3 className="text-xs font-bold text-[#F3DC8A] flex items-center gap-2">
          <Languages size={15} />
          <span>{strings.languageLabel}</span>
        </h3>
        <div className="grid grid-cols-2 gap-3">
          <button
            onClick={() => onUpdatePreferences({ languageCode: 'ar' })}
            className={`py-2.5 px-4 rounded-xl font-bold text-xs border transition-all cursor-pointer ${
              preferences.languageCode === 'ar'
                ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] border-transparent shadow'
                : 'bg-[#052018] text-[#9DB8AD] border-[#D4AF37]/30 hover:text-[#FFF6D8]'
            }`}
          >
            العربية (RTL)
          </button>
          <button
            onClick={() => onUpdatePreferences({ languageCode: 'en' })}
            className={`py-2.5 px-4 rounded-xl font-bold text-xs border transition-all cursor-pointer ${
              preferences.languageCode === 'en'
                ? 'bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] border-transparent shadow'
                : 'bg-[#052018] text-[#9DB8AD] border-[#D4AF37]/30 hover:text-[#FFF6D8]'
            }`}
          >
            English (LTR)
          </button>
        </div>
      </div>

      {/* Export Quality Preference */}
      <div className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-4 shadow-md space-y-2">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-xs font-bold text-[#FFF6D8]">{strings.originalQualityLabel}</h3>
            <p className="text-[11px] text-[#9DB8AD] mt-0.5 max-w-sm">
              {strings.originalQualityDesc}
            </p>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={preferences.originalQuality}
              onChange={e => onUpdatePreferences({ originalQuality: e.target.checked })}
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-[#052018] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#D4AF37]" />
          </label>
        </div>
      </div>

      {/* Backup & Restore Section */}
      <div className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-4 shadow-md space-y-3">
        <h3 className="text-xs font-bold text-[#F3DC8A] flex items-center gap-2">
          <FileJson size={15} />
          <span>{strings.backupSectionTitle}</span>
        </h3>
        <p className="text-xs text-[#9DB8AD] leading-relaxed">
          {strings.backupDesc}
        </p>

        {backupMessage && (
          <div
            className={`p-3 rounded-xl border flex items-center gap-2 text-xs font-bold ${
              backupMessage.type === 'success'
                ? 'bg-emerald-950/60 border-emerald-500/50 text-emerald-300'
                : 'bg-rose-950/60 border-rose-500/50 text-rose-300'
            }`}
          >
            {backupMessage.type === 'success' ? (
              <CheckCircle2 size={16} />
            ) : (
              <AlertCircle size={16} />
            )}
            <span>{backupMessage.text}</span>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 pt-1">
          <button
            onClick={handleExportBackup}
            className="py-2.5 px-3 rounded-xl bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/50 hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer font-bold text-xs flex items-center justify-center gap-1.5 shadow"
          >
            <Download size={14} />
            <span>{strings.exportBackupBtn}</span>
          </button>

          <label className="py-2.5 px-3 rounded-xl bg-[#052018] text-[#FFF6D8] border border-[#D4AF37]/40 hover:border-[#D4AF37] transition-all cursor-pointer font-bold text-xs flex items-center justify-center gap-1.5 shadow">
            <Upload size={14} />
            <span>{strings.restoreBackupBtn}</span>
            <input
              ref={fileInputRef}
              type="file"
              accept=".json"
              onChange={handleImportBackup}
              className="hidden"
            />
          </label>
        </div>
      </div>

      {/* Share & Account */}
      <div className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-4 shadow-md space-y-3">
        <button
          onClick={handleShareApp}
          className="w-full py-2.5 px-4 rounded-xl bg-[#052018] text-[#FFF6D8] border border-[#D4AF37]/30 hover:border-[#D4AF37] transition-all cursor-pointer font-bold text-xs flex items-center justify-center gap-2"
        >
          <Share2 size={15} className="text-[#D4AF37]" />
          <span>{strings.shareAppBtn}</span>
        </button>

        <button
          onClick={onLogout}
          className="w-full py-2.5 px-4 rounded-xl bg-rose-950/40 text-rose-300 border border-rose-900/40 hover:bg-rose-950/80 transition-all cursor-pointer font-bold text-xs flex items-center justify-center gap-2"
        >
          <LogOut size={15} />
          <span>{strings.logoutBtn}</span>
        </button>
      </div>

      {/* Version badge */}
      <div className="text-center text-[11px] text-[#9DB8AD]/70 space-y-0.5">
        <p className="font-bold text-[#D4AF37]">البيت العربي · elbeit elaraby</p>
        <p>نظام إدارة الدعوات والباركود QR وتسجيل الحضور {strings.versionTag}</p>
      </div>
    </div>
  );
};

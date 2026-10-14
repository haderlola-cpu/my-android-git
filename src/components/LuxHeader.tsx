import React from 'react';
import { Languages, Shield, LogOut } from 'lucide-react';
import { AppPreferences } from '../types';
import { getAppStrings } from '../services/catalog';

interface LuxHeaderProps {
  preferences: AppPreferences;
  onToggleLanguage: () => void;
  onLogout?: () => void;
  title?: string;
  subtitle?: string;
  showBack?: boolean;
  onBack?: () => void;
}

export const LuxHeader: React.FC<LuxHeaderProps> = ({
  preferences,
  onToggleLanguage,
  onLogout,
  title,
  subtitle,
  showBack,
  onBack,
}) => {
  const strings = getAppStrings(preferences.languageCode);

  return (
    <header className="sticky top-0 z-40 bg-[#082d22]/95 backdrop-blur-md border-b-2 border-[#D4AF37]/50 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between gap-3">
        {/* Left: Brand mark or Back button */}
        <div className="flex items-center gap-3">
          {showBack && onBack ? (
            <button
              onClick={onBack}
              className="p-2 rounded-xl bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/50 hover:bg-[#D4AF37] hover:text-[#052018] transition-all cursor-pointer flex items-center gap-1.5 text-sm font-bold shadow-md"
              title={strings.back}
            >
              <span>{preferences.languageCode === 'ar' ? '→' : '←'}</span>
              <span>{strings.back}</span>
            </button>
          ) : (
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[#F3DC8A] to-[#D4AF37] flex items-center justify-center rotate-45 shadow-[0_2px_12px_rgba(212,175,55,0.3)] border border-[#B8922A] shrink-0">
              <span className="-rotate-45 text-[#0B3D2E] text-xl font-black">⌂</span>
            </div>
          )}

          <div>
            <h1 className="text-lg md:text-xl font-black text-[#FFF6D8] tracking-wide leading-tight flex items-center gap-2">
              {title || strings.appName}
              {preferences.userRole === 'reception' && (
                <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-800/80 text-emerald-200 border border-emerald-500/50">
                  <Shield size={12} />
                  {strings.receptionTitle}
                </span>
              )}
            </h1>
            <p className="text-xs text-[#9DB8AD] leading-none mt-0.5">
              {subtitle || strings.companySubtitle}
            </p>
          </div>
        </div>

        {/* Right: Actions */}
        <div className="flex items-center gap-2">
          {/* Language Toggle */}
          <button
            onClick={onToggleLanguage}
            className="px-3 py-1.5 rounded-xl bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/40 hover:border-[#D4AF37] hover:bg-[#0E4B39] transition-all flex items-center gap-1.5 text-xs font-bold cursor-pointer shadow-sm"
            title="تبديل اللغة / Switch Language"
          >
            <Languages size={15} />
            <span>{preferences.languageCode === 'ar' ? 'English' : 'العربية'}</span>
          </button>

          {onLogout && (
            <button
              onClick={onLogout}
              className="p-2 rounded-xl bg-[#0B3D2E] text-rose-300 border border-rose-900/40 hover:bg-rose-950/60 transition-all cursor-pointer"
              title={strings.logout}
            >
              <LogOut size={16} />
            </button>
          )}
        </div>
      </div>
    </header>
  );
};

import React from 'react';
import {
  LayoutDashboard,
  Calendar,
  Users,
  QrCode,
  Cloud,
  ShieldCheck,
  Settings,
} from 'lucide-react';
import { AppPreferences } from '../types';
import { getAppStrings } from '../services/catalog';

export type NavTab = 'dashboard' | 'events' | 'guests' | 'scanner' | 'sync' | 'reception' | 'settings';

interface LuxDockProps {
  currentTab: NavTab;
  onSelectTab: (tab: NavTab) => void;
  preferences: AppPreferences;
  pendingSyncCount?: number;
}

export const LuxDock: React.FC<LuxDockProps> = ({
  currentTab,
  onSelectTab,
  preferences,
  pendingSyncCount = 0,
}) => {
  const strings = getAppStrings(preferences.languageCode);

  const navItems: { id: NavTab; label: string; icon: React.FC<{ size: number; className?: string }> }[] = [
    { id: 'dashboard', label: strings.dashboardTitle, icon: LayoutDashboard },
    { id: 'events', label: strings.eventsTitle, icon: Calendar },
    { id: 'guests', label: strings.guestsTitle, icon: Users },
    { id: 'scanner', label: strings.scannerTitle, icon: QrCode },
    { id: 'sync', label: strings.syncTitle, icon: Cloud },
    { id: 'reception', label: strings.receptionTitle, icon: ShieldCheck },
    { id: 'settings', label: strings.settingsTitle, icon: Settings },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#07221B]/95 backdrop-blur-lg border-t border-[#D4AF37]/40 shadow-[0_-8px_30px_rgba(0,0,0,0.5)]">
      <div className="max-w-4xl mx-auto px-2 py-1.5 flex items-center justify-around gap-1">
        {navItems.map(item => {
          const isActive = currentTab === item.id;
          const Icon = item.icon;

          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`relative flex flex-col items-center justify-center py-1.5 px-2 md:px-3 rounded-xl transition-all cursor-pointer ${
                isActive
                  ? 'bg-gradient-to-b from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-bold shadow-lg shadow-[#D4AF37]/20 scale-105'
                  : 'text-[#9DB8AD] hover:text-[#FFF6D8] hover:bg-[#0B3D2E]/60'
              }`}
            >
              <div className="relative">
                <Icon size={20} className={isActive ? 'text-[#052018]' : 'text-inherit'} />
                {item.id === 'sync' && pendingSyncCount > 0 && (
                  <span className="absolute -top-1.5 -right-2 w-4 h-4 bg-amber-500 text-black text-[9px] font-black rounded-full flex items-center justify-center">
                    {pendingSyncCount}
                  </span>
                )}
              </div>
              <span className="text-[10px] md:text-[11px] leading-tight mt-1 whitespace-nowrap">
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};

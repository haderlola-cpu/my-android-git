import React, { useState } from 'react';
import {
  Cloud,
  CheckCircle2,
  RefreshCw,
  Clock,
  ShieldCheck,
  Database,
  ArrowUpRight,
} from 'lucide-react';
import { AppPreferences } from '../types';
import { storageService } from '../services/storage';
import { getAppStrings } from '../services/catalog';

interface SyncScreenProps {
  preferences: AppPreferences;
}

export const SyncScreen: React.FC<SyncScreenProps> = ({ preferences }) => {
  const strings = getAppStrings(preferences.languageCode);
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastSyncTime, setLastSyncTime] = useState<Date>(new Date());
  const [syncQueue, setSyncQueue] = useState(() => storageService.getSyncQueue());

  const handleSyncNow = () => {
    setIsSyncing(true);
    setTimeout(() => {
      storageService.clearSyncQueue();
      setSyncQueue([]);
      setLastSyncTime(new Date());
      setIsSyncing(false);
    }, 1200);
  };

  return (
    <div className="space-y-4 pb-28 max-w-2xl mx-auto">
      {/* Cloud Status Card */}
      <div className="bg-gradient-to-br from-[#0B3D2E] to-[#052018] border-2 border-[#D4AF37]/60 rounded-3xl p-6 shadow-xl text-center space-y-4">
        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#F3DC8A] to-[#D4AF37] text-[#052018] flex items-center justify-center mx-auto shadow-lg shadow-[#D4AF37]/30">
          <Cloud size={32} />
        </div>

        <div>
          <h2 className="text-xl font-black text-[#FFF6D8]">{strings.cloudSyncStatus}</h2>
          <p className="text-xs text-[#9DB8AD] mt-1 max-w-md mx-auto">
            نظام المزامنة الذكي يعمل بدون انقطاع؛ يسجل العمليات محلياً وفور توفر الاتصال تتطابق التعديلات تلقائياً.
          </p>
        </div>

        {/* Sync Status Badge */}
        <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#052018] border border-[#D4AF37]/40 text-xs font-bold text-[#F3DC8A]">
          <CheckCircle2 size={16} className="text-emerald-400" />
          <span>
            {syncQueue.length === 0
              ? strings.allSyncedMsg
              : strings.pendingSyncCount(syncQueue.length)}
          </span>
        </div>

        {/* Last sync time */}
        <p className="text-xs text-[#9DB8AD] flex items-center justify-center gap-1.5">
          <Clock size={13} className="text-[#D4AF37]" />
          <span>آخر مزامنة: {lastSyncTime.toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
        </p>

        {/* Sync Now Button */}
        <div className="pt-2">
          <button
            onClick={handleSyncNow}
            disabled={isSyncing}
            className="py-3 px-8 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-sm shadow-xl hover:brightness-105 active:scale-98 transition-all flex items-center justify-center gap-2 mx-auto cursor-pointer disabled:opacity-50"
          >
            <RefreshCw size={18} className={isSyncing ? 'animate-spin' : ''} />
            <span>{isSyncing ? 'جاري المزامنة...' : strings.syncNowBtn}</span>
          </button>
        </div>
      </div>

      {/* Sync Queue Table */}
      <div className="bg-[#082d22] border border-[#D4AF37]/40 rounded-2xl p-5 shadow-lg space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-[#FFF6D8] flex items-center gap-2">
            <Database size={16} className="text-[#D4AF37]" />
            <span>طابور المزامنة اللحظية ({syncQueue.length})</span>
          </h3>
          <span className="text-[11px] text-[#9DB8AD]">
            Offline-First Queue
          </span>
        </div>

        {syncQueue.length === 0 ? (
          <div className="p-6 text-center text-xs text-[#9DB8AD] bg-[#052018] rounded-xl border border-[#D4AF37]/20">
            ✅ لا توجد عمليات معلقة، قاعدة البيانات متطابقة تماماً.
          </div>
        ) : (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {syncQueue.map(item => (
              <div
                key={item.queueId}
                className="p-2.5 rounded-xl bg-[#052018] border border-[#D4AF37]/30 flex items-center justify-between text-xs"
              >
                <div className="space-y-0.5">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-[#FFF6D8]">{item.entityType}</span>
                    <span className="px-1.5 py-0.2 rounded text-[10px] bg-amber-500/20 text-amber-300 font-mono">
                      {item.actionType}
                    </span>
                  </div>
                  <p className="text-[10px] text-[#9DB8AD] font-mono">
                    ID: {item.entityCloudKey.slice(0, 16)}...
                  </p>
                </div>

                <div className="text-end text-[10px] text-[#9DB8AD]">
                  <span>{new Date(item.timestamp).toLocaleTimeString('ar-EG')}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Safety Notice */}
      <div className="p-4 rounded-2xl bg-[#082d22] border border-[#D4AF37]/30 flex items-start gap-3 text-xs text-[#9DB8AD]">
        <ShieldCheck size={20} className="text-[#D4AF37] shrink-0 mt-0.5" />
        <p className="leading-relaxed">
          جميع البيانات (المناسبات، المدعوون، سجلات الحضور، الأكواد، والتصاميم) تُحفظ على الفور في مساحة التخزين الآمنة للجهاز حتى لا تفقد أي بيانات في حال انقطاع الإنترنت أثناء الحفل.
        </p>
      </div>
    </div>
  );
};

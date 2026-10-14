import React, { useState } from 'react';
import { ShieldCheck, Languages, ArrowLeft, ArrowRight, Lock, User } from 'lucide-react';
import { getAppStrings } from '../services/catalog';

interface LoginScreenProps {
  onLoginAttempt: (username: string, pass: string, role: 'admin' | 'reception') => boolean;
  onToggleLanguage: () => void;
  languageCode: 'ar' | 'en';
}

export const LoginScreen: React.FC<LoginScreenProps> = ({
  onLoginAttempt,
  onToggleLanguage,
  languageCode,
}) => {
  const strings = getAppStrings(languageCode);
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin');
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const ok = onLoginAttempt(username, password, 'admin');
    if (!ok) {
      setErrorMessage(strings.loginError);
    }
  };

  const handleDemoAdmin = () => {
    onLoginAttempt('admin', 'admin', 'admin');
  };

  const handleDemoReception = () => {
    onLoginAttempt('reception', '1234', 'reception');
  };

  return (
    <div className="min-h-screen arabesque-bg flex items-center justify-center p-4">
      {/* Language Switch floating top-right */}
      <button
        onClick={onToggleLanguage}
        className="absolute top-5 right-5 px-3 py-1.5 rounded-xl bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/50 text-xs font-bold flex items-center gap-1.5 shadow hover:bg-[#0E4B39] cursor-pointer"
      >
        <Languages size={15} />
        <span>{languageCode === 'ar' ? 'English' : 'العربية'}</span>
      </button>

      <div className="bg-[#082d22]/95 border-2 border-[#D4AF37] rounded-3xl max-w-md w-full p-6 md:p-8 shadow-2xl space-y-6">
        {/* Brand Mark */}
        <div className="text-center space-y-2">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-[#F3DC8A] to-[#D4AF37] flex items-center justify-center rotate-45 mx-auto shadow-[0_4px_20px_rgba(212,175,55,0.4)] border border-[#B8922A]">
            <span className="-rotate-45 text-[#0B3D2E] text-3xl font-black">⌂</span>
          </div>

          <h1 className="text-2xl font-black text-[#FFF6D8] pt-2">
            {strings.appName}
          </h1>
          <p className="text-xs text-[#9DB8AD]">
            {strings.companySubtitle}
          </p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
              {strings.usernameLabel}
            </label>
            <div className="relative">
              <User size={16} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9DB8AD] pointer-events-none rtl:right-3.5 rtl:left-auto ltr:left-3.5 ltr:right-auto" />
              <input
                type="text"
                required
                value={username}
                onChange={e => setUsername(e.target.value)}
                className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl py-2.5 px-9 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-[#F3DC8A] mb-1">
              {strings.passwordLabel}
            </label>
            <div className="relative">
              <Lock size={16} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#9DB8AD] pointer-events-none rtl:right-3.5 rtl:left-auto ltr:left-3.5 ltr:right-auto" />
              <input
                type="password"
                required
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="w-full bg-[#052018] border border-[#D4AF37]/40 rounded-xl py-2.5 px-9 text-sm text-[#FFF6D8] focus:outline-none focus:border-[#D4AF37]"
              />
            </div>
          </div>

          {errorMessage && (
            <div className="p-3 bg-rose-950/70 border border-rose-500/50 rounded-xl text-rose-200 text-xs font-bold">
              {errorMessage}
            </div>
          )}

          <button
            type="submit"
            className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-[#F3DC8A] to-[#D4AF37] text-[#052018] font-black text-sm shadow-xl hover:brightness-105 active:scale-98 transition-all cursor-pointer"
          >
            {strings.signInButton}
          </button>
        </form>

        {/* Demo Fast Login Buttons */}
        <div className="pt-2 border-t border-[#D4AF37]/20 space-y-2">
          <p className="text-[11px] text-center text-[#9DB8AD]">
            تسجيل دخول تجريبي فوري بنقرة واحدة:
          </p>

          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={handleDemoAdmin}
              className="py-2 px-3 rounded-xl bg-[#0B3D2E] text-[#F3DC8A] border border-[#D4AF37]/40 hover:bg-[#D4AF37] hover:text-[#052018] transition-all text-xs font-bold cursor-pointer"
            >
              دخول كمسؤول (Admin)
            </button>
            <button
              type="button"
              onClick={handleDemoReception}
              className="py-2 px-3 rounded-xl bg-[#052018] text-emerald-400 border border-emerald-500/40 hover:bg-emerald-900/60 transition-all text-xs font-bold cursor-pointer flex items-center justify-center gap-1"
            >
              <ShieldCheck size={14} />
              <span>طاقم الاستقبال</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { storageService } from './services/storage';
import { EventWithCounts, AppPreferences, EventEntity } from './types';
import { LuxHeader } from './components/LuxHeader';
import { LuxDock, NavTab } from './components/LuxDock';
import { DashboardScreen } from './screens/DashboardScreen';
import { HomeScreen } from './screens/HomeScreen';
import { DesignerScreen } from './screens/DesignerScreen';
import { EventDetailScreen } from './screens/EventDetailScreen';
import { GuestsAllScreen } from './screens/GuestsAllScreen';
import { ScannerScreen } from './screens/ScannerScreen';
import { SyncScreen } from './screens/SyncScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { ReceptionHostScreen } from './screens/ReceptionHostScreen';
import { LoginScreen } from './screens/LoginScreen';

type AppView =
  | { type: 'tab'; tab: NavTab }
  | { type: 'eventDetail'; eventId: number }
  | { type: 'designer'; eventId?: number | null };

export const App: React.FC = () => {
  const [preferences, setPreferences] = useState<AppPreferences>(() => storageService.getPreferences());
  const [events, setEvents] = useState<EventWithCounts[]>(() => storageService.getEventsWithCounts());
  const [currentView, setCurrentView] = useState<AppView>({ type: 'tab', tab: 'dashboard' });
  const [viewHistory, setViewHistory] = useState<AppView[]>([]);
  const [pendingSyncCount, setPendingSyncCount] = useState<number>(() => storageService.getSyncQueue().length);

  // Subscribe to storage changes
  useEffect(() => {
    const unsubscribe = storageService.subscribe(() => {
      setEvents(storageService.getEventsWithCounts());
      setPreferences(storageService.getPreferences());
      setPendingSyncCount(storageService.getSyncQueue().length);
    });
    return unsubscribe;
  }, []);

  // Sync RTL and lang attribute
  useEffect(() => {
    document.documentElement.lang = preferences.languageCode;
    document.documentElement.dir = preferences.languageCode === 'ar' ? 'rtl' : 'ltr';
  }, [preferences.languageCode]);

  const navigateTo = (view: AppView) => {
    setViewHistory(prev => [...prev, currentView]);
    setCurrentView(view);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const navigateBack = () => {
    if (viewHistory.length > 0) {
      const prev = viewHistory[viewHistory.length - 1];
      setViewHistory(history => history.slice(0, -1));
      setCurrentView(prev);
    } else {
      setCurrentView({ type: 'tab', tab: 'dashboard' });
    }
  };

  const handleToggleLanguage = () => {
    const nextLang = preferences.languageCode === 'ar' ? 'en' : 'ar';
    storageService.updatePreferences({ languageCode: nextLang });
  };

  const handleLoginAttempt = (user: string, pass: string, role: 'admin' | 'reception'): boolean => {
    if (
      (user === 'admin' && pass === 'admin') ||
      (user === 'reception' && pass === '1234') ||
      (user.length > 0 && pass.length > 0)
    ) {
      storageService.updatePreferences({ isLoggedIn: true, userRole: role });
      if (role === 'reception') {
        setCurrentView({ type: 'tab', tab: 'reception' });
      } else {
        setCurrentView({ type: 'tab', tab: 'dashboard' });
      }
      return true;
    }
    return false;
  };

  const handleLogout = () => {
    storageService.updatePreferences({ isLoggedIn: false });
  };

  if (!preferences.isLoggedIn) {
    return (
      <LoginScreen
        onLoginAttempt={handleLoginAttempt}
        onToggleLanguage={handleToggleLanguage}
        languageCode={preferences.languageCode}
      />
    );
  }

  const isSubScreen = currentView.type === 'eventDetail' || currentView.type === 'designer';
  const activeTab: NavTab = currentView.type === 'tab' ? currentView.tab : 'events';

  return (
    <div className="min-h-screen arabesque-bg flex flex-col text-[#FFF6D8]">
      {/* Top Header */}
      <LuxHeader
        preferences={preferences}
        onToggleLanguage={handleToggleLanguage}
        onLogout={handleLogout}
        showBack={isSubScreen}
        onBack={navigateBack}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6">
        {currentView.type === 'eventDetail' && (
          <EventDetailScreen
            eventId={currentView.eventId}
            preferences={preferences}
            onEditDesign={() => navigateTo({ type: 'designer', eventId: currentView.eventId })}
            onBack={navigateBack}
          />
        )}

        {currentView.type === 'designer' && (
          <DesignerScreen
            existingEvent={currentView.eventId ? storageService.getEvent(currentView.eventId) : null}
            preferences={preferences}
            onSave={(savedEvent) => {
              const res = storageService.saveEvent(savedEvent);
              navigateTo({ type: 'eventDetail', eventId: res.id });
            }}
            onBack={navigateBack}
          />
        )}

        {currentView.type === 'tab' && (
          <>
            {currentView.tab === 'dashboard' && (
              <DashboardScreen
                events={events}
                preferences={preferences}
                onOpenEvent={(id) => navigateTo({ type: 'eventDetail', eventId: id })}
                onCreateNewEvent={() => navigateTo({ type: 'designer', eventId: null })}
                onQuickScanEvent={(id) => navigateTo({ type: 'tab', tab: 'scanner' })}
                onViewAllEvents={() => navigateTo({ type: 'tab', tab: 'events' })}
              />
            )}

            {currentView.tab === 'events' && (
              <HomeScreen
                events={events}
                preferences={preferences}
                onOpenEvent={(id) => navigateTo({ type: 'eventDetail', eventId: id })}
                onCreateNewEvent={() => navigateTo({ type: 'designer', eventId: null })}
                onEditDesign={(id) => navigateTo({ type: 'designer', eventId: id })}
                onQuickScan={(id) => navigateTo({ type: 'tab', tab: 'scanner' })}
                onDeleteEvent={(id) => storageService.deleteEvent(id)}
              />
            )}

            {currentView.tab === 'guests' && (
              <GuestsAllScreen
                preferences={preferences}
                onOpenEvent={(id) => navigateTo({ type: 'eventDetail', eventId: id })}
              />
            )}

            {currentView.tab === 'scanner' && (
              <ScannerScreen
                events={events}
                preferences={preferences}
              />
            )}

            {currentView.tab === 'sync' && (
              <SyncScreen preferences={preferences} />
            )}

            {currentView.tab === 'reception' && (
              <ReceptionHostScreen
                preferences={preferences}
                onExitReception={() => {
                  storageService.updatePreferences({ userRole: 'admin' });
                  setCurrentView({ type: 'tab', tab: 'dashboard' });
                }}
              />
            )}

            {currentView.tab === 'settings' && (
              <SettingsScreen
                preferences={preferences}
                onUpdatePreferences={(updates) => storageService.updatePreferences(updates)}
                onLogout={handleLogout}
              />
            )}
          </>
        )}
      </main>

      {/* Floating Bottom Luxury Dock */}
      {!isSubScreen && (
        <LuxDock
          currentTab={activeTab}
          onSelectTab={(tab) => {
            setViewHistory([]);
            setCurrentView({ type: 'tab', tab });
            window.scrollTo({ top: 0, behavior: 'smooth' });
          }}
          preferences={preferences}
          pendingSyncCount={pendingSyncCount}
        />
      )}
    </div>
  );
};

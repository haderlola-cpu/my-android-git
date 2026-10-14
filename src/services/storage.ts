import {
  EventEntity,
  GuestEntity,
  EventWithCounts,
  AddGuestsResult,
  SkippedGuestItem,
  ScanOutcome,
  SyncQueueItem,
  AppPreferences,
} from '../types';

const STORAGE_KEY_EVENTS = 'elbeit_events_v2';
const STORAGE_KEY_GUESTS = 'elbeit_guests_v2';
const STORAGE_KEY_PREFS = 'elbeit_prefs_v2';
const STORAGE_KEY_SYNC_QUEUE = 'elbeit_sync_queue_v2';

function generateRandomCode(): string {
  return Math.floor(10000000 + Math.random() * 90000000).toString();
}

function normalizeArabicText(text: string): string {
  return text
    .trim()
    .replace(/[أإآ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/\s+/g, ' ')
    .toLowerCase();
}

function normalizePhoneNumber(phone: string): string {
  return phone.replace(/[^0-9+]/g, '').trim();
}

const INITIAL_EVENTS: EventEntity[] = [
  {
    id: 1,
    name: 'حفل زفاف أحمد & فاطمة',
    date: '2026-10-25',
    timeFrom: '07:30 م',
    timeTo: '11:30 م',
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
    qrPosY: 0.48,
    namePosX: 0.50,
    namePosY: 0.76,
    datePosX: 0.50,
    datePosY: 0.88,
    whatsappTemplate: 'أهلًا {name} 🌹\nيسعدنا دعوتك إلى {event}\n{date} - {time}\nمرفق دعوتك الخاصة، برجاء إظهار الباركود عند الدخول. نتشرف بحضورك ✨',
    createdAt: Date.now() - 86400000 * 5,
    groomName: 'أحمد محمود النجار',
    brideName: 'فاطمة عصام الصاوي',
    headerVerse: 'وَمِنْ آيَاتِهِ أَنْ خَلَقَ لَكُم مِّنْ أَنفُسِكُمْ أَزْوَاجًا لِّتَسْكُنُوا إِلَيْهَا',
    hostsText: 'تتشرف عائلتا النجار والصاوي بدعوتكم لحضور حفل الزفاف المبارك',
    dayName: 'السبت',
    venueName: 'قاعة رويال بالاس الكبرى',
    venueAddress: 'شارع التسعين الشمالي، التجمع الخامس، القاهرة',
    locationUrl: 'https://maps.google.com/?q=Royal+Palace+Hall',
    showLocationQr: true,
    notesText: 'جنة الأطفال منازلهم · نرجو الالتزام بالموعد المحدد',
    closingText: 'حضوركم يكتمل به سرورنا وعاقبة لديكم بالمسرات ✨',
    cloudKey: 'ev-cloud-key-001',
    updatedAt: Date.now(),
  },
  {
    id: 2,
    name: 'حفل خطوبة عمر & مريم',
    date: '2026-11-14',
    timeFrom: '06:00 م',
    timeTo: '10:00 م',
    backgroundType: 'PRESET',
    backgroundPresetId: 1,
    backgroundAspectRatio: 0.714,
    nameColorHex: '#F6E3A5',
    qrColorHex: '#07221B',
    qrBgColorHex: '#FFF6D8',
    fontIndex: 1,
    showGuestName: true,
    showDate: true,
    showTime: true,
    qrSizeRatio: 0.28,
    nameSizeSp: 24,
    qrPosX: 0.50,
    qrPosY: 0.46,
    namePosX: 0.50,
    namePosY: 0.74,
    datePosX: 0.50,
    datePosY: 0.86,
    whatsappTemplate: 'أهلًا {name} 🌸\nنتشرف بدعوتك لحفل خطوبة عمر ومريم\n{date}\nيسعدنا تشريفك وحضورك الكريم ✨',
    createdAt: Date.now() - 86400000 * 2,
    groomName: 'عمر الشريف',
    brideName: 'مريم الأحمدي',
    headerVerse: 'بَارَكَ اللَّهُ لَكُمَا وَبَارَكَ عَلَيْكُمَا وَجَمَعَ بَيْنَكُمَا فِي خَيْرٍ',
    hostsText: 'يسر الأستاذ سامي الشريف دعوة سيادتكم لمشاركتنا فرحة الخطوبة',
    dayName: 'الجمعة',
    venueName: 'فندق سميراميس إنتركونتيننتال - قاعة كليوباترا',
    venueAddress: 'كورنيش النيل، جاردن سيتي، القاهرة',
    locationUrl: 'https://maps.google.com/?q=Semiramis+InterContinental+Cairo',
    showLocationQr: true,
    notesText: 'الدعوة شخصية للمدعو فقط',
    closingText: 'دامت دياركم عامرة بالأفراح والمسرات',
    cloudKey: 'ev-cloud-key-002',
    updatedAt: Date.now(),
  },
];

const INITIAL_GUESTS: GuestEntity[] = [
  {
    id: 1,
    eventId: 1,
    name: 'معالي المستشار حسام الخولي وعائلته',
    normalizedName: normalizeArabicText('معالي المستشار حسام الخولي وعائلته'),
    phone: '+201001234567',
    normalizedPhone: '+201001234567',
    code: '48291037',
    sequenceNumber: 1,
    attended: true,
    attendedAt: Date.now() - 3600000 * 2,
    whatsappSent: true,
    createdAt: Date.now() - 86400000 * 4,
    cloudKey: 'gst-001',
    updatedAt: Date.now(),
  },
  {
    id: 2,
    eventId: 1,
    name: 'الدكتور حازم بدوي',
    normalizedName: normalizeArabicText('الدكتور حازم بدوي'),
    phone: '+201112345678',
    normalizedPhone: '+201112345678',
    code: '71930528',
    sequenceNumber: 2,
    attended: true,
    attendedAt: Date.now() - 1800000,
    whatsappSent: true,
    createdAt: Date.now() - 86400000 * 4,
    cloudKey: 'gst-002',
    updatedAt: Date.now(),
  },
  {
    id: 3,
    eventId: 1,
    name: 'المهندس طارق منصور وحرمه',
    normalizedName: normalizeArabicText('المهندس طارق منصور وحرمه'),
    phone: '+201223456789',
    normalizedPhone: '+201223456789',
    code: '63820194',
    sequenceNumber: 3,
    attended: false,
    attendedAt: null,
    whatsappSent: true,
    createdAt: Date.now() - 86400000 * 4,
    cloudKey: 'gst-003',
    updatedAt: Date.now(),
  },
  {
    id: 4,
    eventId: 1,
    name: 'الأستاذة نادية عبد الرحمن',
    normalizedName: normalizeArabicText('الأستاذة نادية عبد الرحمن'),
    phone: '+201098765432',
    normalizedPhone: '+201098765432',
    code: '85019372',
    sequenceNumber: 4,
    attended: false,
    attendedAt: null,
    whatsappSent: false,
    createdAt: Date.now() - 86400000 * 3,
    cloudKey: 'gst-004',
    updatedAt: Date.now(),
  },
  {
    id: 5,
    eventId: 1,
    name: 'السيد أحمد إبراهيم فهمي',
    normalizedName: normalizeArabicText('السيد أحمد إبراهيم فهمي'),
    phone: '+201555123456',
    normalizedPhone: '+201555123456',
    code: '92847105',
    sequenceNumber: 5,
    attended: false,
    attendedAt: null,
    whatsappSent: false,
    createdAt: Date.now() - 86400000 * 3,
    cloudKey: 'gst-005',
    updatedAt: Date.now(),
  },
  {
    id: 6,
    eventId: 2,
    name: 'السيد خالد عبد العزيز',
    normalizedName: normalizeArabicText('السيد خالد عبد العزيز'),
    phone: '+201011122233',
    normalizedPhone: '+201011122233',
    code: '30491827',
    sequenceNumber: 1,
    attended: false,
    attendedAt: null,
    whatsappSent: true,
    createdAt: Date.now() - 86400000,
    cloudKey: 'gst-006',
    updatedAt: Date.now(),
  },
  {
    id: 7,
    eventId: 2,
    name: 'المهندسة سارة مدحت',
    normalizedName: normalizeArabicText('المهندسة سارة مدحت'),
    phone: '+201200033344',
    normalizedPhone: '+201200033344',
    code: '58204916',
    sequenceNumber: 2,
    attended: false,
    attendedAt: null,
    whatsappSent: false,
    createdAt: Date.now() - 86400000,
    cloudKey: 'gst-007',
    updatedAt: Date.now(),
  },
];

const INITIAL_PREFS: AppPreferences = {
  languageCode: 'ar',
  isDarkMode: true,
  continuousScan: true,
  originalQuality: false,
  isLoggedIn: true,
  userRole: 'admin',
};

class StorageService {
  private events: EventEntity[] = [];
  private guests: GuestEntity[] = [];
  private preferences: AppPreferences = { ...INITIAL_PREFS };
  private syncQueue: SyncQueueItem[] = [];
  private listeners: (() => void)[] = [];

  constructor() {
    this.loadFromStorage();
  }

  private loadFromStorage() {
    try {
      const storedEvents = localStorage.getItem(STORAGE_KEY_EVENTS);
      const storedGuests = localStorage.getItem(STORAGE_KEY_GUESTS);
      const storedPrefs = localStorage.getItem(STORAGE_KEY_PREFS);
      const storedSync = localStorage.getItem(STORAGE_KEY_SYNC_QUEUE);

      if (storedEvents) {
        this.events = JSON.parse(storedEvents);
      } else {
        this.events = [...INITIAL_EVENTS];
        this.saveEvents();
      }

      if (storedGuests) {
        this.guests = JSON.parse(storedGuests);
      } else {
        this.guests = [...INITIAL_GUESTS];
        this.saveGuests();
      }

      if (storedPrefs) {
        this.preferences = { ...INITIAL_PREFS, ...JSON.parse(storedPrefs) };
      } else {
        this.preferences = { ...INITIAL_PREFS };
        this.savePrefs();
      }

      if (storedSync) {
        this.syncQueue = JSON.parse(storedSync);
      } else {
        this.syncQueue = [];
      }
    } catch (e) {
      console.error('Failed to load storage, using defaults:', e);
      this.events = [...INITIAL_EVENTS];
      this.guests = [...INITIAL_GUESTS];
      this.preferences = { ...INITIAL_PREFS };
    }
  }

  private saveEvents() {
    localStorage.setItem(STORAGE_KEY_EVENTS, JSON.stringify(this.events));
    this.notify();
  }

  private saveGuests() {
    localStorage.setItem(STORAGE_KEY_GUESTS, JSON.stringify(this.guests));
    this.notify();
  }

  private savePrefs() {
    localStorage.setItem(STORAGE_KEY_PREFS, JSON.stringify(this.preferences));
    this.notify();
  }

  private saveSyncQueue() {
    localStorage.setItem(STORAGE_KEY_SYNC_QUEUE, JSON.stringify(this.syncQueue));
    this.notify();
  }

  public subscribe(listener: () => void): () => void {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private notify() {
    this.listeners.forEach(l => l());
  }

  // --- Preferences ---
  public getPreferences(): AppPreferences {
    return { ...this.preferences };
  }

  public updatePreferences(updates: Partial<AppPreferences>) {
    this.preferences = { ...this.preferences, ...updates };
    this.savePrefs();
  }

  // --- Events ---
  public getEventsWithCounts(): EventWithCounts[] {
    const activeEvents = this.events.filter(e => !e.isDeleted);
    return activeEvents.map(event => {
      const eventGuests = this.guests.filter(g => g.eventId === event.id && !g.isDeleted);
      const totalGuests = eventGuests.length;
      const attendedGuests = eventGuests.filter(g => g.attended).length;
      return { event, totalGuests, attendedGuests };
    });
  }

  public getEvent(id: number): EventEntity | undefined {
    return this.events.find(e => e.id === id && !e.isDeleted);
  }

  public saveEvent(event: EventEntity): EventEntity {
    const isNew = event.id === 0 || !this.events.some(e => e.id === event.id);
    let targetEvent: EventEntity;

    if (isNew) {
      const nextId = this.events.reduce((max, e) => Math.max(max, e.id), 0) + 1;
      targetEvent = {
        ...event,
        id: nextId,
        cloudKey: event.cloudKey || `ev-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        createdAt: event.createdAt || Date.now(),
        updatedAt: Date.now(),
      };
      this.events = [targetEvent, ...this.events];
    } else {
      targetEvent = {
        ...event,
        updatedAt: Date.now(),
      };
      this.events = this.events.map(e => (e.id === targetEvent.id ? targetEvent : e));
    }

    this.saveEvents();
    this.enqueueSync('EVENT', targetEvent.cloudKey, undefined, 'UPSERT', JSON.stringify(targetEvent));
    return targetEvent;
  }

  public deleteEvent(id: number) {
    const ev = this.events.find(e => e.id === id);
    if (!ev) return;
    
    this.events = this.events.map(e => (e.id === id ? { ...e, isDeleted: true, updatedAt: Date.now() } : e));
    // Soft-delete related guests
    this.guests = this.guests.map(g => (g.eventId === id ? { ...g, isDeleted: true, updatedAt: Date.now() } : g));

    this.saveEvents();
    this.saveGuests();
    this.enqueueSync('EVENT', ev.cloudKey, undefined, 'DELETE', JSON.stringify({ id, cloudKey: ev.cloudKey }));
  }

  // --- Guests ---
  public getGuestsForEvent(eventId: number): GuestEntity[] {
    return this.guests
      .filter(g => g.eventId === eventId && !g.isDeleted)
      .sort((a, b) => a.sequenceNumber - b.sequenceNumber);
  }

  public getAllGuests(): GuestEntity[] {
    return this.guests.filter(g => !g.isDeleted);
  }

  public getGuest(id: number): GuestEntity | undefined {
    return this.guests.find(g => g.id === id && !g.isDeleted);
  }

  public saveGuest(guest: GuestEntity): GuestEntity {
    const isNew = guest.id === 0 || !this.guests.some(g => g.id === guest.id);
    let targetGuest: GuestEntity;

    if (isNew) {
      const nextId = this.guests.reduce((max, g) => Math.max(max, g.id), 0) + 1;
      const currentEventGuests = this.guests.filter(g => g.eventId === guest.eventId && !g.isDeleted);
      const nextSeq = guest.sequenceNumber > 0 ? guest.sequenceNumber : (currentEventGuests.length + 1);

      targetGuest = {
        ...guest,
        id: nextId,
        sequenceNumber: nextSeq,
        code: guest.code || generateRandomCode(),
        normalizedName: normalizeArabicText(guest.name),
        normalizedPhone: normalizePhoneNumber(guest.phone),
        cloudKey: guest.cloudKey || `gst-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        createdAt: guest.createdAt || Date.now(),
        updatedAt: Date.now(),
      };
      this.guests = [...this.guests, targetGuest];
    } else {
      targetGuest = {
        ...guest,
        normalizedName: normalizeArabicText(guest.name),
        normalizedPhone: normalizePhoneNumber(guest.phone),
        updatedAt: Date.now(),
      };
      this.guests = this.guests.map(g => (g.id === targetGuest.id ? targetGuest : g));
    }

    this.saveGuests();
    const parentEvent = this.events.find(e => e.id === targetGuest.eventId);
    this.enqueueSync('GUEST', targetGuest.cloudKey, parentEvent?.cloudKey, 'UPSERT', JSON.stringify(targetGuest));
    return targetGuest;
  }

  public deleteGuest(id: number) {
    const gst = this.guests.find(g => g.id === id);
    if (!gst) return;

    this.guests = this.guests.map(g => (g.id === id ? { ...g, isDeleted: true, updatedAt: Date.now() } : g));
    this.saveGuests();
    const parentEvent = this.events.find(e => e.id === gst.eventId);
    this.enqueueSync('GUEST', gst.cloudKey, parentEvent?.cloudKey, 'DELETE', JSON.stringify({ id, cloudKey: gst.cloudKey }));
  }

  public toggleAttendance(guestId: number, attended?: boolean): GuestEntity | null {
    const gst = this.guests.find(g => g.id === guestId);
    if (!gst) return null;

    const newAttended = attended !== undefined ? attended : !gst.attended;
    const updated: GuestEntity = {
      ...gst,
      attended: newAttended,
      attendedAt: newAttended ? (gst.attendedAt || Date.now()) : null,
      updatedAt: Date.now(),
    };

    this.guests = this.guests.map(g => (g.id === guestId ? updated : g));
    this.saveGuests();

    const parentEvent = this.events.find(e => e.id === updated.eventId);
    this.enqueueSync(
      'GUEST',
      updated.cloudKey,
      parentEvent?.cloudKey,
      'UPDATE_ATTENDANCE',
      JSON.stringify({ id: updated.id, attended: newAttended, attendedAt: updated.attendedAt })
    );

    return updated;
  }

  public setWhatsappSent(guestId: number, sent: boolean = true) {
    this.guests = this.guests.map(g => (g.id === guestId ? { ...g, whatsappSent: sent, updatedAt: Date.now() } : g));
    this.saveGuests();
  }

  // --- Batch Add Guests ---
  public addGuestsByNames(eventId: number, rawInput: string): AddGuestsResult {
    const lines = rawInput.split('\n').map(l => l.trim()).filter(Boolean);
    const existingGuests = this.getGuestsForEvent(eventId);
    const existingNormalizedNames = new Set(existingGuests.map(g => g.normalizedName));
    
    let addedCount = 0;
    const skippedItems: SkippedGuestItem[] = [];
    let currentSeq = existingGuests.reduce((max, g) => Math.max(max, g.sequenceNumber), 0);

    const newGuests: GuestEntity[] = [];

    for (const line of lines) {
      // Split name and possible phone (phone typically starts with 0 or + or digits at end)
      const phoneMatch = line.match(/(\+?\d[\d\s-]{7,})/);
      let name = line;
      let phone = '';

      if (phoneMatch) {
        phone = phoneMatch[1].trim();
        name = line.replace(phoneMatch[1], '').trim();
      }

      if (!name) {
        name = phone;
      }

      const normalized = normalizeArabicText(name);
      if (existingNormalizedNames.has(normalized)) {
        skippedItems.push({
          inputLine: line,
          reasonAr: 'الاسم مضاف مسبقاً في هذه المناسبة',
          reasonEn: 'Name already exists in this event',
        });
        continue;
      }

      existingNormalizedNames.add(normalized);
      currentSeq++;
      const nextId = this.guests.reduce((max, g) => Math.max(max, g.id), 0) + 1 + addedCount;

      const gst: GuestEntity = {
        id: nextId,
        eventId,
        name,
        normalizedName: normalized,
        phone,
        normalizedPhone: normalizePhoneNumber(phone),
        code: generateRandomCode(),
        sequenceNumber: currentSeq,
        attended: false,
        attendedAt: null,
        whatsappSent: false,
        createdAt: Date.now(),
        cloudKey: `gst-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        updatedAt: Date.now(),
      };

      newGuests.push(gst);
      addedCount++;
    }

    if (newGuests.length > 0) {
      this.guests = [...this.guests, ...newGuests];
      this.saveGuests();
    }

    return { addedCount, skippedItems };
  }

  public addGuestsBySequence(eventId: number, from: number, to: number, prefix: string): AddGuestsResult {
    const existingGuests = this.getGuestsForEvent(eventId);
    const existingCodes = new Set(existingGuests.map(g => g.sequenceNumber));
    
    const count = Math.min(Math.max(1, to - from + 1), 5000);
    let addedCount = 0;
    const skippedItems: SkippedGuestItem[] = [];
    const newGuests: GuestEntity[] = [];
    let currentSeq = existingGuests.reduce((max, g) => Math.max(max, g.sequenceNumber), 0);

    for (let i = 0; i < count; i++) {
      const num = from + i;
      const name = prefix ? `${prefix} ${num}` : `دعوة رقم ${num}`;
      currentSeq++;
      const nextId = this.guests.reduce((max, g) => Math.max(max, g.id), 0) + 1 + addedCount;

      newGuests.push({
        id: nextId,
        eventId,
        name,
        normalizedName: normalizeArabicText(name),
        phone: '',
        normalizedPhone: '',
        code: generateRandomCode(),
        sequenceNumber: currentSeq,
        attended: false,
        attendedAt: null,
        whatsappSent: false,
        createdAt: Date.now(),
        cloudKey: `gst-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        updatedAt: Date.now(),
      });
      addedCount++;
    }

    if (newGuests.length > 0) {
      this.guests = [...this.guests, ...newGuests];
      this.saveGuests();
    }

    return { addedCount, skippedItems };
  }

  public addGuestsByPhones(eventId: number, rawInput: string): AddGuestsResult {
    const lines = rawInput.split('\n').map(l => l.trim()).filter(Boolean);
    const existingGuests = this.getGuestsForEvent(eventId);
    const existingPhones = new Set(existingGuests.map(g => g.normalizedPhone).filter(Boolean));

    let addedCount = 0;
    const skippedItems: SkippedGuestItem[] = [];
    let currentSeq = existingGuests.reduce((max, g) => Math.max(max, g.sequenceNumber), 0);
    const newGuests: GuestEntity[] = [];

    for (const line of lines) {
      const normalizedPhone = normalizePhoneNumber(line);
      if (!normalizedPhone) continue;

      if (existingPhones.has(normalizedPhone)) {
        skippedItems.push({
          inputLine: line,
          reasonAr: 'رقم الهاتف مضاف مسبقاً في هذه المناسبة',
          reasonEn: 'Phone already exists in this event',
        });
        continue;
      }

      existingPhones.add(normalizedPhone);
      currentSeq++;
      const nextId = this.guests.reduce((max, g) => Math.max(max, g.id), 0) + 1 + addedCount;
      const name = `ضيف ${currentSeq}`;

      newGuests.push({
        id: nextId,
        eventId,
        name,
        normalizedName: normalizeArabicText(name),
        phone: line,
        normalizedPhone,
        code: generateRandomCode(),
        sequenceNumber: currentSeq,
        attended: false,
        attendedAt: null,
        whatsappSent: false,
        createdAt: Date.now(),
        cloudKey: `gst-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        updatedAt: Date.now(),
      });
      addedCount++;
    }

    if (newGuests.length > 0) {
      this.guests = [...this.guests, ...newGuests];
      this.saveGuests();
    }

    return { addedCount, skippedItems };
  }

  // --- QR Code Verification ---
  public verifyBarcode(currentEventId: number, rawPayload: string): ScanOutcome {
    const clean = rawPayload.trim();

    // Standard pattern: "eb:eventId:code" or "eb:code" or URL query or direct 8-digit code
    let parsedEventId: number | null = null;
    let code = clean;

    if (clean.startsWith('eb:')) {
      const parts = clean.split(':');
      if (parts.length === 3) {
        parsedEventId = parseInt(parts[1], 10);
        code = parts[2];
      } else if (parts.length === 2) {
        code = parts[1];
      }
    } else if (clean.includes('code=')) {
      const match = clean.match(/code=([a-zA-Z0-9]+)/);
      if (match) code = match[1];
    }

    // Find guest across all active guests
    const targetGuest = this.guests.find(g => g.code === code && !g.isDeleted);

    if (!targetGuest) {
      return {
        status: 'UNKNOWN',
        rawBarcode: clean,
      };
    }

    // Check wrong event
    if (targetGuest.eventId !== currentEventId) {
      const otherEvent = this.events.find(e => e.id === targetGuest.eventId);
      return {
        status: 'WRONG_EVENT',
        guest: targetGuest,
        wrongEventId: targetGuest.eventId,
        wrongEventName: otherEvent?.name || `مناسبة رقم ${targetGuest.eventId}`,
        rawBarcode: clean,
      };
    }

    // Check if already checked in
    if (targetGuest.attended) {
      return {
        status: 'ALREADY_CHECKED_IN',
        guest: targetGuest,
        firstCheckInTimeMillis: targetGuest.attendedAt || targetGuest.updatedAt || Date.now(),
      };
    }

    // Valid check-in
    const checkInTime = Date.now();
    this.toggleAttendance(targetGuest.id, true);
    const updatedGuest = this.getGuest(targetGuest.id) || targetGuest;

    return {
      status: 'SUCCESS',
      guest: updatedGuest,
      checkInTimeMillis: checkInTime,
    };
  }

  // --- Sync Queue & Offline Simulation ---
  private enqueueSync(
    entityType: 'EVENT' | 'GUEST',
    entityCloudKey: string,
    parentCloudKey: string | undefined,
    actionType: 'UPSERT' | 'DELETE' | 'UPDATE_ATTENDANCE',
    payloadJson: string
  ) {
    const item: SyncQueueItem = {
      queueId: Date.now() + Math.floor(Math.random() * 1000),
      entityType,
      entityCloudKey,
      parentCloudKey,
      actionType,
      payloadJson,
      timestamp: Date.now(),
      retryCount: 0,
    };

    this.syncQueue.push(item);
    this.saveSyncQueue();
  }

  public getSyncQueue(): SyncQueueItem[] {
    return [...this.syncQueue];
  }

  public clearSyncQueue() {
    this.syncQueue = [];
    this.saveSyncQueue();
  }

  // --- Backup & Restore ---
  public exportBackupJson(): string {
    const backup = {
      app: 'elbeit-elaraby',
      version: 2,
      exportedAt: new Date().toISOString(),
      events: this.events.filter(e => !e.isDeleted),
      guests: this.guests.filter(g => !g.isDeleted),
      preferences: this.preferences,
    };
    return JSON.stringify(backup, null, 2);
  }

  public importBackupJson(jsonString: string): { success: boolean; messageAr: string; messageEn: string } {
    try {
      const parsed = JSON.parse(jsonString);
      if (!parsed || (!Array.isArray(parsed.events) && !Array.isArray(parsed.guests))) {
        return {
          success: false,
          messageAr: 'ملف النسخة الاحتياطية غير صالح أو تالف',
          messageEn: 'Invalid or corrupted backup JSON file',
        };
      }

      const importedEvents: EventEntity[] = parsed.events || [];
      const importedGuests: GuestEntity[] = parsed.guests || [];

      // Merge events without wiping
      const eventMap = new Map<number, EventEntity>();
      this.events.forEach(e => eventMap.set(e.id, e));
      importedEvents.forEach(e => eventMap.set(e.id, e));
      this.events = Array.from(eventMap.values());

      // Merge guests without wiping
      const guestMap = new Map<number, GuestEntity>();
      this.guests.forEach(g => guestMap.set(g.id, g));
      importedGuests.forEach(g => guestMap.set(g.id, g));
      this.guests = Array.from(guestMap.values());

      this.saveEvents();
      this.saveGuests();

      return {
        success: true,
        messageAr: `تمت استعادة ${importedEvents.length} مناسبة و ${importedGuests.length} دعوة بنجاح`,
        messageEn: `Successfully restored ${importedEvents.length} events and ${importedGuests.length} guests`,
      };
    } catch (e: any) {
      return {
        success: false,
        messageAr: `فشلت الاستعادة: ${e?.message || 'خطأ غير معروف'}`,
        messageEn: `Restore failed: ${e?.message || 'Unknown error'}`,
      };
    }
  }
}

export const storageService = new StorageService();

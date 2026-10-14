export interface EventEntity {
  id: number;
  name: string;
  date: string;
  timeFrom: string;
  timeTo: string;
  backgroundType: 'PRESET' | 'IMAGE' | 'GIF' | 'VIDEO';
  backgroundPresetId: number; // 0..5
  backgroundUri?: string | null;
  backgroundAspectRatio: number; // Default 0.714 (5:7 ratio)
  nameColorHex: string;
  qrColorHex: string;
  qrBgColorHex: string;
  fontIndex: number; // 0..7
  showGuestName: boolean;
  showDate: boolean;
  showTime: boolean;
  qrSizeRatio: number; // 0.15..0.45
  nameSizeSp: number; // 12..42
  qrPosX: number; // 0..1
  qrPosY: number; // 0..1
  namePosX: number; // 0..1
  namePosY: number; // 0..1
  datePosX: number; // 0..1
  datePosY: number; // 0..1
  whatsappTemplate: string;
  createdAt: number;
  groomName: string;
  brideName: string;
  headerVerse: string;
  hostsText: string;
  dayName: string;
  venueName: string;
  venueAddress: string;
  locationUrl: string;
  showLocationQr: boolean;
  notesText: string;
  closingText: string;
  cloudKey: string;
  updatedAt: number;
  isDeleted?: boolean;
}

export interface GuestEntity {
  id: number;
  eventId: number;
  name: string;
  normalizedName: string;
  phone: string;
  normalizedPhone: string;
  code: string; // 8-digit or alphanumeric unique code
  sequenceNumber: number;
  attended: boolean;
  attendedAt: number | null;
  whatsappSent: boolean;
  createdAt: number;
  cloudKey: string;
  updatedAt: number;
  isDeleted?: boolean;
}

export interface EventWithCounts {
  event: EventEntity;
  totalGuests: number;
  attendedGuests: number;
}

export interface SkippedGuestItem {
  inputLine: string;
  reasonAr: string;
  reasonEn: string;
}

export interface AddGuestsResult {
  addedCount: number;
  skippedItems: SkippedGuestItem[];
}

export type ScanStatus = 'SUCCESS' | 'ALREADY_CHECKED_IN' | 'WRONG_EVENT' | 'UNKNOWN';

export interface ScanOutcome {
  status: ScanStatus;
  guest?: GuestEntity;
  checkInTimeMillis?: number;
  firstCheckInTimeMillis?: number;
  wrongEventId?: number;
  wrongEventName?: string;
  rawBarcode?: string;
}

export interface SyncQueueItem {
  queueId: number;
  entityType: 'EVENT' | 'GUEST';
  entityCloudKey: string;
  parentCloudKey?: string;
  actionType: 'UPSERT' | 'DELETE' | 'UPDATE_ATTENDANCE';
  payloadJson: string;
  timestamp: number;
  retryCount: number;
  lastError?: string;
}

export interface AppPreferences {
  languageCode: 'ar' | 'en';
  isDarkMode: boolean;
  continuousScan: boolean;
  originalQuality: boolean;
  isLoggedIn: boolean;
  userRole: 'admin' | 'reception';
  receptionEventId?: number;
}

export interface PresetBackgroundStyle {
  id: number;
  titleAr: string;
  titleEn: string;
  topColor: string;
  centerColor: string;
  bottomColor: string;
  accentColor: string;
  secondaryAccentColor: string;
  imageFallback?: string;
}

export interface FontChoice {
  index: number;
  nameAr: string;
  nameEn: string;
  fontFamily: string;
  cssFamily: string;
  fontWeight: number | string;
  fontStyle?: string;
}

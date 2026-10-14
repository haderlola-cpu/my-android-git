import { PresetBackgroundStyle, FontChoice } from '../types';

export const PRESET_BACKGROUNDS: PresetBackgroundStyle[] = [
  {
    id: 0,
    titleAr: 'ذهب ملكي وأوبسيديان',
    titleEn: 'Royal Gold & Obsidian',
    topColor: '#0E1525',
    centerColor: '#1A2540',
    bottomColor: '#0A0F1A',
    accentColor: '#D4AF37',
    secondaryAccentColor: '#F7E7B4',
    imageFallback: '/backgrounds/img_wedding_royal_gold.jpg',
  },
  {
    id: 1,
    titleAr: 'زمردي ليلي فاخر',
    titleEn: 'Midnight Emerald Gala',
    topColor: '#07221B',
    centerColor: '#0E3B2F',
    bottomColor: '#051813',
    accentColor: '#E5C158',
    secondaryAccentColor: '#88D4B8',
    imageFallback: '/backgrounds/img_wedding_boho_floral.jpg',
  },
  {
    id: 2,
    titleAr: 'وردي مخملي وشامبانيا',
    titleEn: 'Velvet Rose & Champagne',
    topColor: '#331122',
    centerColor: '#521C36',
    bottomColor: '#240B17',
    accentColor: '#F5D1B0',
    secondaryAccentColor: '#E699B8',
    imageFallback: '/backgrounds/img_wedding_blush_palace.jpg',
  },
  {
    id: 3,
    titleAr: 'ليالي عربية ياقوتية',
    titleEn: 'Arabian Nights Sapphire',
    topColor: '#0C1B3D',
    centerColor: '#17346E',
    bottomColor: '#081229',
    accentColor: '#F3C663',
    secondaryAccentColor: '#93B7F8',
  },
  {
    id: 4,
    titleAr: 'لؤلؤ عاجي وبرونز',
    titleEn: 'Ivory Pearl & Bronze',
    topColor: '#FBF7EE',
    centerColor: '#F3E9D2',
    bottomColor: '#EAE0C8',
    accentColor: '#8C6223',
    secondaryAccentColor: '#C59B4E',
    imageFallback: '/backgrounds/img_wedding_ivory_floral.jpg',
  },
  {
    id: 5,
    titleAr: 'قصر الجمشت الاحتفالي',
    titleEn: 'Festive Amethyst Palace',
    topColor: '#211138',
    centerColor: '#381E5E',
    bottomColor: '#150A26',
    accentColor: '#EAC870',
    secondaryAccentColor: '#C4A1F5',
  },
];

export const PRESET_COLORS_HEX = [
  '#FFFDF6E2', // Warm Ivory
  '#D4AF37',   // Royal Gold
  '#FFFFFF',   // Pure White
  '#121826',   // Deep Obsidian
  '#000000',   // Classic Black
  '#E5C158',   // Champagne Gold
  '#0F5132',   // Royal Emerald
  '#1E3A8A',   // Sapphire Blue
  '#7F1D1D',   // Burgundy Velvet
  '#581C87',   // Imperial Purple
  '#8C6223',   // Antique Bronze
  '#F43F5E',   // Rose Coral
  '#0D9488',   // Turquoise Teal
  '#334155',   // Slate Charcoal
];

export const FONT_CHOICES: FontChoice[] = [
  {
    index: 0,
    nameAr: 'القاهرة الملكي (Cairo)',
    nameEn: 'Cairo Royal Sans',
    fontFamily: 'Cairo',
    cssFamily: "'Cairo', sans-serif",
    fontWeight: '700',
  },
  {
    index: 1,
    nameAr: 'أميري كلاسيك (Amiri)',
    nameEn: 'Amiri Classical Serif',
    fontFamily: 'Amiri',
    cssFamily: "'Amiri', serif",
    fontWeight: '700',
  },
  {
    index: 2,
    nameAr: 'تجول عصري (Tajawal)',
    nameEn: 'Tajawal Modern Light',
    fontFamily: 'Tajawal',
    cssFamily: "'Tajawal', sans-serif",
    fontWeight: '500',
  },
  {
    index: 3,
    nameAr: 'المراعي انسيابي (Almarai)',
    nameEn: 'Almarai Clean',
    fontFamily: 'Almarai',
    cssFamily: "'Almarai', sans-serif",
    fontWeight: '700',
  },
  {
    index: 4,
    nameAr: 'تشانجا هندسي (Changa)',
    nameEn: 'Changa Geometric',
    fontFamily: 'Changa',
    cssFamily: "'Changa', sans-serif",
    fontWeight: '700',
  },
  {
    index: 5,
    nameAr: 'بلايفير فاخر (Playfair)',
    nameEn: 'Playfair Display Serif',
    fontFamily: 'Playfair Display',
    cssFamily: "'Playfair Display', serif",
    fontWeight: '700',
  },
  {
    index: 6,
    nameAr: 'مونتسيرات (Montserrat)',
    nameEn: 'Montserrat Geometric',
    fontFamily: 'Montserrat',
    cssFamily: "'Montserrat', sans-serif",
    fontWeight: '800',
  },
  {
    index: 7,
    nameAr: 'الخط الديواني الاحتفالي (Script)',
    nameEn: 'Dancing Script Cursive',
    fontFamily: 'Dancing Script',
    cssFamily: "'Dancing Script', cursive",
    fontWeight: '700',
    fontStyle: 'italic',
  },
];

export function getAppStrings(lang: 'ar' | 'en') {
  const isAr = lang === 'ar';
  return {
    isArabic: isAr,
    appName: 'elbeit elaraby',
    companySubtitle: isAr ? 'تنظيم الحفلات والمناسبات الفاخرة' : 'Luxury Event & Wedding Planning',
    versionTag: 'v2.0 Web',
    
    // Login
    loginTitle: isAr ? 'تسجيل الدخول' : 'Sign In',
    usernameLabel: isAr ? 'البريد الإلكتروني أو اسم المستخدم' : 'Email or Username',
    passwordLabel: isAr ? 'كلمة المرور' : 'Password',
    signInButton: isAr ? 'دخول إلى النظام' : 'Sign In',
    receptionModeBtn: isAr ? 'دخول طاقم الاستقبال / البوابة' : 'Reception / Gatekeeper Access',
    loginError: isAr ? 'بيانات الدخول غير صحيحة. تأكد من اسم المستخدم وكلمة المرور.' : 'Invalid credentials. Please check your username and password.',
    demoCredsHint: isAr ? 'تجربة سريعة: admin / admin أو reception / 1234' : 'Quick demo: admin / admin or reception / 1234',

    // Dashboard & Navigation
    dashboardTitle: isAr ? 'لوحة التحكم' : 'Dashboard',
    eventsTitle: isAr ? 'المناسبات والحفلات' : 'Events & Parties',
    guestsTitle: isAr ? 'سجل الضيوف الشامل' : 'All Guests Directory',
    scannerTitle: isAr ? 'مسح الباركود عند الباب' : 'Door QR Scanner',
    syncTitle: isAr ? 'المزامنة السحابية' : 'Cloud Synchronization',
    settingsTitle: isAr ? 'الإعدادات والنسخ الاحتياطي' : 'Settings & Backup',
    receptionTitle: isAr ? 'طاقم الاستقبال' : 'Reception Gatekeeper',

    searchEventsPlaceholder: isAr ? 'بحث باسم المناسبة...' : 'Search events by name...',
    newEventButton: isAr ? 'مناسبة جديدة' : 'New Event',
    noEventsTitle: isAr ? 'لا توجد مناسبات حالياً' : 'No Events Yet',
    noEventsSubtitle: isAr ? 'اضغط على زر مناسبة جديدة لإنشاء مناسبة وتصميم الدعوة الخاصة بها' : 'Create your first event and design its luxury invitations',
    quickScanBtn: isAr ? 'مسح سريع' : 'Quick Scan',
    viewAllBtn: isAr ? 'عرض الكل' : 'View All',
    
    // Counters
    guestsLabel: isAr ? 'المدعوون' : 'Guests',
    attendedLabel: isAr ? 'الحاضرون' : 'Attended',
    notArrivedLabel: isAr ? 'لم يصلوا' : 'Not Arrived',
    attendanceRateLabel: isAr ? 'نسبة الحضور' : 'Attendance Rate',
    nextEventTitle: isAr ? 'المناسبة القادمة' : 'Upcoming Event',
    eventTodayTag: isAr ? 'المناسبة اليوم ✦' : 'Event Today ✦',
    pastEventTag: isAr ? 'مناسبة سابقة' : 'Past Event',
    daysRemainingFormat: (d: number) => isAr ? `بعد ${d} يوم` : `in ${d} days`,

    // Designer
    createInvitationTitle: isAr ? 'تصميم الدعوة وإنشاء المناسبة' : 'Invitation Studio & Event Setup',
    editDesignTitle: isAr ? 'تعديل تصميم الدعوة' : 'Edit Invitation Design',
    eventDetailsSection: isAr ? '1. بيانات المناسبة' : '1. Event Details',
    eventNameLabel: isAr ? 'اسم المناسبة' : 'Event Name',
    eventDateLabel: isAr ? 'التاريخ' : 'Date',
    dayNameLabel: isAr ? 'اليوم' : 'Day',
    timeFromLabel: isAr ? 'الوقت من' : 'Time From',
    timeToLabel: isAr ? 'إلى' : 'To',
    groomNameLabel: isAr ? 'اسم العريس' : "Groom's Name",
    brideNameLabel: isAr ? 'اسم العروس' : "Bride's Name",
    headerVerseLabel: isAr ? 'الآية / الترويسة' : 'Header Verse / Basmala',
    hostsTextLabel: isAr ? 'نص الداعين (أولياء الأمور)' : 'Hosts Invitation Text',
    venueNameLabel: isAr ? 'اسم القاعة أو الفندق' : 'Venue / Hall Name',
    venueAddressLabel: isAr ? 'العنوان بالتفصيل' : 'Venue Address',
    locationUrlLabel: isAr ? 'رابط خرائط جوجل (Google Maps)' : 'Google Maps Location Link',
    showLocationQrLabel: isAr ? 'توليد باركود موقع القاعة (QR)' : 'Generate Venue Location QR',
    notesLabel: isAr ? 'ملاحظات وتنبيهات (مثل جنة الأطفال منازلهم)' : 'Special Notes & Courtesy Warnings',
    closingTextLabel: isAr ? 'الخاتمة / عبارة الترحيب' : 'Closing Salutation',

    backgroundSection: isAr ? '2. الخلفية الفاخرة' : '2. Luxury Background',
    presetBackgroundsTab: isAr ? 'خلفيات جاهزة راقية' : 'Curated Luxury Presets',
    uploadPhotoOrGifBtn: isAr ? 'رفع صورة من جهازك' : 'Upload Custom Image',
    colorsSection: isAr ? '3. الألوان والخطوط' : '3. Colors & Typography',
    nameColorLabel: isAr ? 'لون النصوص والأسماء' : 'Text & Name Color',
    qrColorLabel: isAr ? 'لون الباركود' : 'QR Barcode Color',
    qrBgColorLabel: isAr ? 'خلفية الباركود' : 'QR Background Color',
    fontsSection: isAr ? 'نوع الخط العربي / اللاتيني' : 'Arabic & Latin Font',
    visibilitySection: isAr ? '4. عناصر الدعوة' : '4. Display Elements',
    showGuestNameLabel: isAr ? 'إظهار اسم المدعو' : 'Show Guest Name',
    showDateLabel: isAr ? 'إظهار التاريخ' : 'Show Event Date',
    showTimeLabel: isAr ? 'إظهار الوقت' : 'Show Event Time',
    sizeAndPositionSection: isAr ? '5. الحجم ومواقع العناصر' : '5. Sizing & Element Positioning',
    qrSizeLabel: isAr ? 'حجم الباركود' : 'QR Barcode Size',
    nameSizeLabel: isAr ? 'حجم اسم المدعو' : 'Guest Name Size',
    dragHintText: isAr ? 'يمكنك تحريك الباركود والنصوص بالسحب المباشر داخل المعاينة أو باستخدام أشرطة الإحداثيات' : 'Drag QR or text elements directly on the preview or use coordinate sliders',
    saveEventAndDesignBtn: isAr ? 'حفظ المناسبة والتصميم' : 'Save Event & Design',
    sampleGuestName: isAr ? 'السيد أحمد محمد الشاذلي وعائلته الكريمة' : 'Mr. Ahmed Mohamed & Family',

    // Event Detail
    statsDashboardTitle: isAr ? 'لوحة إحصائيات الحضور اللحظية' : 'Live Attendance Analytics',
    totalGuestsStat: isAr ? 'إجمالي المدعوين' : 'Total Guests',
    attendedStat: isAr ? 'الحاضرون بالفعل' : 'Checked In',
    notArrivedStat: isAr ? 'في الانتظار' : 'Pending',
    firstCheckInLabel: isAr ? 'أول حضور:' : 'First Arrival:',
    lastCheckInLabel: isAr ? 'آخر حضور:' : 'Latest Arrival:',
    filterAll: isAr ? 'الكل' : 'All',
    filterAttended: isAr ? 'حضر' : 'Attended',
    filterNotArrived: isAr ? 'لم يصل' : 'Not Arrived',
    filterWhatsappSent: isAr ? 'تم الإرسال واتساب' : 'WhatsApp Sent',
    searchGuestPlaceholder: isAr ? 'بحث بالاسم، الكود، أو رقم الهاتف...' : 'Search by name, code, or phone...',
    addGuestsBtn: isAr ? 'إضافة ضيوف' : 'Add Guests',
    scanDoorBtn: isAr ? 'مسح باركود الباب' : 'Scan Door QR',
    sequentialWhatsappBtn: isAr ? 'الإرسال المتتابع' : 'Sequential WhatsApp',
    downloadAllZipBtn: isAr ? 'تحميل الكل (ZIP)' : 'Download All (ZIP)',
    exportExcelBtn: isAr ? 'تصدير جدول الحضور (CSV)' : 'Export Attendance (CSV)',
    editEventInfoMenu: isAr ? 'تعديل بيانات المناسبة' : 'Edit Event Info',
    whatsappTemplateMenu: isAr ? 'قالب رسالة واتساب' : 'WhatsApp Message Template',
    selectModeBtn: isAr ? 'تحديد متعدد' : 'Select Mode',
    selectAllBtn: isAr ? 'تحديد الكل' : 'Select All',
    downloadSelectedBtn: isAr ? 'تحميل المحدد' : 'Download Selected',
    sendSelectedWhatsappBtn: isAr ? 'إرسال المحدد واتساب' : 'Send WhatsApp to Selected',
    deleteSelectedBtn: isAr ? 'حذف المحدد' : 'Delete Selected',
    checkInBtn: isAr ? 'تسجيل دخول' : 'Check In',
    undoCheckInBtn: isAr ? 'إلغاء الحضور' : 'Undo Check-in',
    sendWhatsappBtn: isAr ? 'إرسال واتساب' : 'Send WhatsApp',
    viewCardBtn: isAr ? 'معاينة البطاقة' : 'View Card',
    downloadCardBtn: isAr ? 'تحميل البطاقة' : 'Download Card',

    // Add Guests Dialog
    addGuestsDialogTitle: isAr ? 'إضافة ضيوف (3 طرق ميسرة)' : 'Add Guests (3 Methods)',
    tabByNames: isAr ? 'بالأسماء وأرقام الهاتف' : 'By Names & Phone',
    tabBySequence: isAr ? 'تسلسل رقمي' : 'Sequential Numbers',
    tabByPhones: isAr ? 'أرقام هواتف مجردة' : 'Phone Numbers List',
    byNamesHint: isAr ? 'اكتب أو الصق اسماً في كل سطر (واختيارياً رقم الهاتف):\nأحمد محمد 01012345678\nمنى السيد\nكريم علي 01123456789' : 'One name per line (optional phone number):\nAhmed Mohamed 01012345678\nMona El-Sayed\nKarim Ali 01123456789',
    seqFromLabel: isAr ? 'من رقم' : 'From #',
    seqToLabel: isAr ? 'إلى رقم' : 'To #',
    seqPrefixLabel: isAr ? 'بادئة (مثل: طاولة، دعوة)' : 'Prefix (e.g. Table, Inv)',
    byPhonesHint: isAr ? 'الصق أرقام الهواتف (سطر لكل رقم):' : 'Paste phone numbers (one per line):',
    addNowBtn: isAr ? 'إضافة وتوليد الأكواد' : 'Generate Invitations',

    // Scanner
    continuousScanLabel: isAr ? 'المسح المستمر (إعادة تفعيل الكاميرا تلقائياً)' : 'Continuous scanning mode',
    scanSuccessTitle: isAr ? 'مرحباً بكم! تم تسجيل الدخول بنجاح ✨' : 'Welcome! Checked In Successfully ✨',
    scanDuplicateTitle: isAr ? 'تنبيه: تم تسجيل حضور هذا الضيف مسبقاً!' : 'Warning: Guest Already Checked In!',
    scanWrongEventTitle: isAr ? 'خطأ: هذه الدعوة تخص مناسبة أخرى!' : 'Wrong Event: Invitation belongs to another event!',
    scanUnknownTitle: isAr ? 'خطأ: كود غير معروف أو تالف!' : 'Invalid: Unknown or corrupted code!',
    firstEntryTimeLabel: isAr ? 'وقت الدخول الأول:' : 'First Check-in Time:',
    enterCodeManuallyLabel: isAr ? 'إدخال الكود يدوياً (مثال: eb:1:48291037 أو الكود فقط)' : 'Enter code manually (e.g. eb:1:48291037 or 8-digit code)',
    verifyCodeBtn: isAr ? 'تحقق وتسجيل' : 'Verify & Check In',
    cameraScanInstruction: isAr ? 'وجّه كاميرا الهاتف نحو باركود الدعوة' : 'Point camera at invitation QR code',

    // Sync
    cloudSyncStatus: isAr ? 'حالة المزامنة السحابية' : 'Cloud Synchronization Status',
    allSyncedMsg: isAr ? 'جميع العمليات متزامنة ومحفوظة محلياً وفي السحابة' : 'All records are synchronized and backed up',
    pendingSyncCount: (n: number) => isAr ? `${n} عملية بانتظار المزامنة` : `${n} pending sync operations`,
    syncNowBtn: isAr ? 'مزامنة الآن' : 'Sync Now',

    // Common
    cancel: isAr ? 'إلغاء' : 'Cancel',
    delete: isAr ? 'حذف' : 'Delete',
    save: isAr ? 'حفظ' : 'Save',
    close: isAr ? 'إغلاق' : 'Close',
    back: isAr ? 'رجوع' : 'Back',
    deleteConfirm: isAr ? 'هل أنت متأكد من الحذف؟ لا يمكن التراجع عن هذا الإجراء.' : 'Are you sure you want to delete? This action cannot be undone.',
    shareApp: isAr ? 'مشاركة التطبيق' : 'Share App',
    logout: isAr ? 'تسجيل الخروج' : 'Sign Out',
    arabicLanguage: 'العربية',
    englishLanguage: 'English',
  };
}

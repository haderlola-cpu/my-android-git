import { EventEntity, GuestEntity } from '../types';

export function formatWhatsAppMessage(event: EventEntity, guest: GuestEntity): string {
  const timeFormatted = `${event.timeFrom} - ${event.timeTo}`;
  let template = event.whatsappTemplate || EventEntityTemplateFallback;

  return template
    .replace(/\{name\}/g, guest.name)
    .replace(/\{event\}/g, event.name)
    .replace(/\{date\}/g, event.date)
    .replace(/\{time\}/g, timeFormatted)
    .replace(/\{code\}/g, guest.code)
    .replace(/\{venue\}/g, event.venueName || '')
    .replace(/\{location\}/g, event.locationUrl || '');
}

const EventEntityTemplateFallback =
  'أهلًا {name} 🌹\nيسعدنا دعوتك إلى {event}\n{date} ({time})\nمرفق كود دعوتك الخاصة: {code}\nبرجاء إظهار الباركود عند الدخول. نتشرف بحضورك ✨';

export function getWhatsAppShareUrl(event: EventEntity, guest: GuestEntity): string {
  const message = formatWhatsAppMessage(event, guest);
  let cleanPhone = (guest.phone || '').replace(/[^0-9]/g, '');

  // Add Egypt or default code if local 01xxxxxxxxx
  if (cleanPhone.startsWith('01') && cleanPhone.length === 11) {
    cleanPhone = '20' + cleanPhone.slice(1);
  }

  if (cleanPhone) {
    return `https://wa.me/${cleanPhone}?text=${encodeURIComponent(message)}`;
  }
  return `https://wa.me/?text=${encodeURIComponent(message)}`;
}

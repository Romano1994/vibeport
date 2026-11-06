const formatSingleTime = (value) => {
  if (value.includes(':')) return value.slice(0, 5);
  const digits = value.replace(/\D/g, '');
  return digits.length >= 4 ? `${digits.slice(0, 2)}:${digits.slice(2, 4)}` : value;
};

/**
 * 공연 시간(HHmm, 여러 회차는 HHmm/HHmm)을 화면 표시용 HH:mm으로 변환합니다.
 * 예: "1900" → "19:00", "1500/1900" → "15:00 / 19:00", "미정" → "미정"
 */
export function formatConcertTime(value) {
  if (!value) return '';
  return String(value)
    .split('/')
    .map((part) => part.trim())
    .filter(Boolean)
    .map(formatSingleTime)
    .join(' / ');
}

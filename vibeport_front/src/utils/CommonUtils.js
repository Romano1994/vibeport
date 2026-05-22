/**
 * 공통 유틸 함수 모음
 * named export + default export 모두 제공 (호환성)
 */

export const isNull = (v) => v === null || v === undefined;

export const isEmpty = (v) => {
  if (isNull(v)) return true;
  if (typeof v === 'string') return v.trim() === '';
  if (Array.isArray(v)) return v.length === 0;
  if (v instanceof Date) return false;
  if (typeof v === 'object') return Object.keys(v).length === 0;
  return false;
};

export const isBlank = (v) => {
  // 문자열이 null/undefined 이거나 공백만 있는지 검사
  if (isNull(v)) return true;
  return typeof v === 'string' && v.trim() === '';
};

export const isValidEmail = (v) => {
  if (typeof v !== 'string') return false;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim());
};

export const trim = (v) => (typeof v === 'string' ? v.trim() : v);
import { memo, useEffect, useMemo, useState } from 'react';
import client from '../api/client';
import { htmlToPlainText, truncateText } from '../utils/TextUtils';

const NEWSLETTER_LAUNCH_DATE = '2026-04-28';
const NEWSLETTER_SEND_TIME = '09:00';

function Eyebrow({ children, accent = 'var(--brick)' }) {
  return (
    <div className="vp-eyebrow" style={{ color: accent }}>
      {children}
    </div>
  );
}

function formatSendDate(date = new Date()) {
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
}

function parseFeatureDate(feature) {
  const rawDate = feature?.sendDateText || feature?.sentDate || feature?.regTime;
  if (!rawDate) return new Date();

  const normalized = String(rawDate).replace(' ', 'T');
  const parsed = new Date(normalized);
  return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
}

function resolveImageSrc(rawImage) {
  if (!rawImage) return null;

  const baseURL = (client.defaults.baseURL || '').replace(/\/$/, '');

  const idx = rawImage.indexOf('/concert_images/');
  if (idx >= 0) {
    return `${baseURL}${rawImage.slice(idx)}`;
  }

  if (/^https?:\/\//i.test(rawImage)) return rawImage;
  const path = rawImage.startsWith('/') ? rawImage : `/${rawImage}`;
  return `${baseURL}${path}`;
}

function calculateVolNumber(date = new Date()) {
  const launch = new Date(`${NEWSLETTER_LAUNCH_DATE}T00:00:00`);
  const target = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const diff = Math.floor((target - launch) / 86400000) + 1;
  return Math.max(diff, 1);
}

function EmailPreview({
  feature,
  className = '',
  isLoading = false,
  loadingMessage = '오늘의 메일 미리보기를 불러오는 중입니다.',
  emptyMessage = '오늘의 메일 미리보기가 없습니다.',
  previewMaxLength,
  bodyClassName = '',
  showReadMore = false,
  onReadMore,
}) {
  const sendDate = useMemo(() => parseFeatureDate(feature), [feature]);
  const sendDateText = useMemo(() => formatSendDate(sendDate), [sendDate]);
  const volNumber = useMemo(() => calculateVolNumber(sendDate), [sendDate]);
  const artistName = feature?.artistNmFor || feature?.artistNmKor || '';
  const plainContent = useMemo(() => {
    if (feature?.plainContent !== undefined) return feature.plainContent;
    return htmlToPlainText(feature?.content);
  }, [feature]);
  const content = useMemo(() => {
    return previewMaxLength ? truncateText(plainContent, previewMaxLength) : plainContent;
  }, [plainContent, previewMaxLength]);
  const rawImage = feature?.artistImageUrl;
  const imageSrc = useMemo(() => resolveImageSrc(rawImage), [rawImage]);
  const [imageFailed, setImageFailed] = useState(false);
  useEffect(() => { setImageFailed(false); }, [imageSrc]);

  return (
    <div className={`vp-archive-mockup ${className}`.trim()}>
      <div className="vp-archive-mail-chrome">
        <span>FROM Vibeport &lt;vibeport@gmail.com&gt;</span>
        <span>{sendDateText} · {NEWSLETTER_SEND_TIME}</span>
      </div>

      {isLoading ? (
        <div className="vp-archive-empty">{loadingMessage}</div>
      ) : feature ? (
        <>
          <div className="vp-ce-masthead">
            <div className="vp-ce-vol">VOL.{volNumber} · {sendDateText}</div>
            <div className="vp-ce-brand">Vibeport</div>
            <div className="vp-ce-subtitle">내한 아티스트를 위한 아침 편지</div>
          </div>

          <div className="vp-ce-artist-header">
            <Eyebrow>오늘의 아티스트</Eyebrow>
            <h1 className="vp-ce-h1">{artistName}</h1>
            {feature.artistNmKor && feature.artistNmFor && (
              <div className="vp-ce-subtitle-small">{feature.artistNmKor}</div>
            )}
          </div>

          {imageSrc && !imageFailed ? (
            <div className="vp-ce-hero-photo">
              <img
                src={imageSrc}
                alt={`${artistName} 사진`}
                className="vp-ce-hero-img"
                loading="eager"
                decoding="async"
                onError={() => setImageFailed(true)}
              />
            </div>
          ) : (
            <div className="skel-light vp-ce-hero" style={{ height: 220, color: 'var(--ink-mute)' }}>
              ARTIST · HERO PHOTO
            </div>
          )}

          <div className="vp-ce-body">
            {content ? (
              <p className={`vp-ce-essay ${bodyClassName}`.trim()}>{content}</p>
            ) : (
              <p className="vp-ce-essay" style={{ color: 'var(--ink-mute)' }}>
                오늘 발송된 메일 본문이 아직 없습니다.
              </p>
            )}
          </div>

          <div className="vp-ce-footer vp-archive-preview-footer">
            <span>{sendDateText}</span>
            {showReadMore && (
              <span onClick={onReadMore} className="vp-archive-feature-more">
                READ MORE
              </span>
            )}
          </div>
        </>
      ) : (
        <div className="vp-archive-empty">{emptyMessage}</div>
      )}
    </div>
  );
}

export default memo(EmailPreview);

import React, { useEffect } from 'react';
import ReactDOM from 'react-dom';
import client from '../api/client';
import { formatConcertTime } from '../utils/TimeUtils';
import '../styles/slide.css';


const resolveImageSrc = (rawImage) => {
  if (!rawImage) return null;

  const baseURL = (client.defaults.baseURL || '').replace(/\/$/, '');

  const idx = rawImage.indexOf('/concert_images/');
  if (idx >= 0) {
    return `${baseURL}${rawImage.slice(idx)}`;
  }

  if (/^https?:\/\//i.test(rawImage)) return rawImage;
  const imagePath = rawImage.startsWith('/') ? rawImage : `/${rawImage}`;
  return `${baseURL}${imagePath}`;
};

function ConcertDetailSlide({ concert, isOpen, onClose }) {
  useEffect(() => {
    const onEsc = (e) => { if (e.key === 'Escape' && isOpen) onClose(); };
    if (isOpen) window.addEventListener('keydown', onEsc);
    return () => window.removeEventListener('keydown', onEsc);
  }, [isOpen, onClose]);


  const formatDateTime = (c) => {
    if (!c) return '-';
    const { concertYear, concertMonth, concertDate, concertTime } = c;
    if (!concertYear || !concertMonth || !concertDate) return '-';
    const mm   = String(concertMonth).padStart(2, '0');
    const dd   = String(concertDate).padStart(2, '0');
    const time = formatConcertTime(concertTime);
    return time ? `${concertYear}.${mm}.${dd} ${time}` : `${concertYear}.${mm}.${dd}`;
  };

  if (!concert || !isOpen) return null;

  const name = concert.artistNmKor || concert.artistNmEng || '아티스트 정보 없음';

  const metaRows = [
    ['장소', concert.venue || '-'],
    ['일시', formatDateTime(concert)],
    ...(concert.tctSite   ? [['티켓', concert.tctSite]]   : []),
    ...(concert.tctOpenAt ? [['예매', concert.tctOpenAt]] : []),
  ];

  const imageSrc = resolveImageSrc(concert.artistImageUrl);

  return ReactDOM.createPortal(
    <>
      <div
        className={`vp-slide-backdrop${isOpen ? ' active' : ''}`}
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        className={`vp-slide-panel${isOpen ? ' active' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="vp-concert-title"
      >
        {/* 브레드크럼 */}
        <div className="vp-slide-crumb">
          <span>CALENDAR / {name.toUpperCase()}</span>
          <button className="vp-slide-back" onClick={onClose} aria-label="뒤로가기">← BACK</button>
        </div>

        <div className="vp-slide-body">
          {/* 포스터 영역 */}
          <div className="vp-detail-poster-col">
            {imageSrc ? (
              <div className="vp-detail-poster" data-fallback="TOUR POSTER">
                <img
                  src={imageSrc}
                  alt={`${name} 포스터`}
                  className="vp-detail-poster-img"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                    e.currentTarget.parentElement.classList.add('is-empty');
                  }}
                />
              </div>
            ) : (
              <div className="skel-light vp-detail-poster">TOUR POSTER</div>
            )}
          </div>

          {/* 정보 영역 */}
          <div className="vp-detail-info-col">
            <div className="vp-detail-eyebrow">Concert · {formatDateTime(concert)}</div>

            <h1 id="vp-concert-title" className="vp-detail-title">
              {name}
            </h1>

            {concert.genre && (
              <div className="vp-detail-tagline">
                {concert.genre} <span style={{ color: 'var(--ink-dim)' }}>in Korea</span>
              </div>
            )}

            {/* 메타 테이블 */}
            {metaRows.length > 0 && (
              <div className="vp-detail-meta-table">
                {metaRows.map(([k, v]) => (
                  <React.Fragment key={k}>
                    <div className="vp-meta-label">{k.toUpperCase()}</div>
                    <div className="vp-meta-value">{v}</div>
                  </React.Fragment>
                ))}
              </div>
            )}

            {/* 큐레이터 노트 / 설명 */}
            {(concert.description || concert.notes) && (
              <>
                <div className="vp-detail-curator-eyebrow">Curator's Note</div>
                <p className="vp-detail-essay">
                  {concert.description || concert.notes}
                </p>
              </>
            )}

          </div>
        </div>
      </div>
    </>,
    document.body,
  );
}

export default ConcertDetailSlide;

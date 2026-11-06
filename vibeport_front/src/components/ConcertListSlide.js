import React, { useEffect } from 'react';
import ReactDOM from 'react-dom';
import { formatConcertTime } from '../utils/TimeUtils';
import '../styles/slide.css';

function ConcertListSlide({ concerts, date, isOpen, onClose, onSelectConcert }) {
  useEffect(() => {
    const onEsc = (e) => { if (e.key === 'Escape' && isOpen) onClose(); };
    if (isOpen) {
      window.addEventListener('keydown', onEsc);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      window.removeEventListener('keydown', onEsc);
      document.body.style.overflow = '';
    };
  }, [isOpen, onClose]);


  if (!isOpen) return null;

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
        aria-labelledby="vp-list-title"
      >
        <div className="vp-slide-crumb">
          <span id="vp-list-title">
            {date ? `${date}일 공연 목록` : '공연 목록'}
            {concerts?.length > 0 && (
              <span className="vp-slide-count"> · {concerts.length}건</span>
            )}
          </span>
          <button className="vp-slide-back" onClick={onClose} aria-label="닫기">× CLOSE</button>
        </div>

        <div className="vp-slide-body vp-list-body">
          {concerts && concerts.length > 0 ? (
            <div className="vp-list">
              {concerts.map((concert, idx) => (
                <div
                  key={concert.concertNo ?? idx}
                  className="vp-list-item"
                  onClick={() => onSelectConcert(concert)}
                  onKeyPress={(e) => { if (e.key === 'Enter') onSelectConcert(concert); }}
                  role="button"
                  tabIndex={0}
                  aria-label={`${concert.artistNmKor} 공연 상세보기`}
                >
                  <span className="vp-list-idx">{String(idx + 1).padStart(2, '0')}</span>
                  <div className="vp-list-info">
                    <div className="vp-list-artist">
                      {concert.artistNmKor || concert.artistNmEng || '아티스트 정보 없음'}
                    </div>
                    <div className="vp-list-meta">
                      {formatConcertTime(concert.concertTime)}{concert.venue ? ` · ${concert.venue}` : ''}
                    </div>
                  </div>
                  <span className="vp-list-arrow">›</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="vp-slide-empty">
              이 날짜에는 등록된 공연이 없습니다.
            </div>
          )}
        </div>
      </div>
    </>,
    document.body,
  );
}

export default ConcertListSlide;

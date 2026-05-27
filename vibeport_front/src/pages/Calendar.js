import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import client from '../api/client';
import ConcertDetailSlide from '../components/ConcertDetailSlide';
import ConcertListSlide from '../components/ConcertListSlide';
import '../styles/calendar.css';

const MONTH_NAMES = [
  'January','February','March','April','May','June',
  'July','August','September','October','November','December',
];
const WEEKDAYS = ['SUN','MON','TUE','WED','THU','FRI','SAT'];

function buildGrid(year, month) {
  const first = new Date(year, month, 1);
  const last  = new Date(year, month + 1, 0);
  const cells = [];
  for (let i = 0; i < first.getDay(); i++) cells.push(null);
  for (let d = 1; d <= last.getDate(); d++) cells.push(d);
  while (cells.length % 7 !== 0) cells.push(null);
  return cells;
}

function accentFor(concert) {
  return concert?.genre === 'pop' ? 'var(--olive)' : 'var(--brick)';
}
function tintFor(concert) {
  return concert?.genre === 'pop'
    ? 'rgba(61,74,42,0.10)'
    : 'rgba(160,58,44,0.09)';
}
function hoverTintFor(concert) {
  return concert?.genre === 'pop'
    ? 'rgba(61,74,42,0.18)'
    : 'rgba(160,58,44,0.16)';
}


function Calendar() {
  const now = new Date();
  const { state: initFeature } = useLocation();
  const [year,  setYear]  = useState(initFeature?.year  ?? now.getFullYear());
  const [month, setMonth] = useState(initFeature?.month ?? now.getMonth());
  const [concertInfos, setConcertInfos] = useState([]);
  const [showSidebar, setShowSidebar]   = useState(false);
  const [selectedDay, setSelectedDay]   = useState(null);
  const [isAnimating, setIsAnimating]   = useState(false);

  const [slideState, setSlideState] = useState({
    list:   { isOpen: false, date: null, concerts: [] },
    detail: { isOpen: false, concert: null },
  });

  const today = now.getDate();
  const isCurrentMonth = year === now.getFullYear() && month === now.getMonth();
  const grid  = useMemo(() => buildGrid(year, month), [year, month]);

  const concertsByDate = useMemo(() => {
    const map = new Map();
    concertInfos.forEach((item) => {
      const day = item?.concertDate;
      if (!day) return;
      if (!map.has(day)) map.set(day, []);
      map.get(day).push(item);
    });
    return map;
  }, [concertInfos]);

  const sortedConcerts = useMemo(
    () => [...concertInfos].sort((a, b) => (a.concertDate || 0) - (b.concertDate || 0)),
    [concertInfos],
  );

  const getConcertInfos = useCallback((y, m) => {
    return client.post('/concert/getConcertInfosByMonth', { year: y, month: m + 1 })
      .then((data) => {
        const payload = data?.data ?? data;
        setConcertInfos(Array.isArray(payload) ? payload : []);
      })
      .catch(() => setConcertInfos([]));
  }, []);

  useEffect(() => {
    getConcertInfos(year, month).then(() => {
      if (initFeature?.concert && initFeature?.date) {
        openDetail(initFeature.concert, initFeature.date);
      }
    });
  }, []);

  const changeMonth = useCallback((ny, nm) => {
    setShowSidebar(false);
    setSelectedDay(null);
    setIsAnimating(true);
    getConcertInfos(ny, nm).then(() => {
      setYear(ny); setMonth(nm);
      setTimeout(() => setIsAnimating(false), 300);
    }).catch(() => {
      setYear(ny); setMonth(nm);
      setTimeout(() => setIsAnimating(false), 300);
    });
  }, [getConcertInfos]);

  const handlePrev = () => {
    if (month === 0) changeMonth(year - 1, 11);
    else changeMonth(year, month - 1);
  };
  const handleNext = () => {
    if (month === 11) changeMonth(year + 1, 0);
    else changeMonth(year, month + 1);
  };
  const handleToday = () => changeMonth(now.getFullYear(), now.getMonth());

  const openDetail  = useCallback((concert, day) => {
    if (day !== undefined) { setShowSidebar(true); setSelectedDay(day); }
    setSlideState(prev => ({ ...prev, detail: { isOpen: true, concert } }));
  }, []);
  const closeDetail = useCallback(() => setSlideState(prev => ({
    ...prev, detail: { isOpen: false, concert: null },
  })), []);
  const openList    = useCallback((date, concerts) => {
    setShowSidebar(true);
    setSelectedDay(date);
    setSlideState({
      list: { isOpen: true, date, concerts },
      detail: { isOpen: false, concert: null },
    });
  }, []);
  const handleCellClick = useCallback((date, concerts) => {
    if (!concerts.length) return;
    if (concerts.length > 2) {
      openList(date, concerts);
      return;
    }
    openDetail(concerts[concerts.length - 1], date);
  }, [openDetail, openList]);
  const closeSidebar = useCallback(() => {
    setShowSidebar(false);
    setSelectedDay(null);
  }, []);
  const closeList   = useCallback(() => setSlideState({
    list: { isOpen: false, date: null, concerts: [] },
    detail: { isOpen: false, concert: null },
  }), []);

  const formatTime = (t) => {
    if (!t) return '';
    if (String(t).includes(':')) return String(t).slice(0, 5);
    const d = String(t).replace(/\D/g, '');
    return d.length >= 4 ? `${d.slice(0,2)}:${d.slice(2,4)}` : t;
  };

  return (
    <div className={`vp-calendar-page${isAnimating ? ' animating' : ''}`}>
      {/* 타이틀바 */}
      <div className="vp-cal-titlebar">
        <div className="vp-cal-titlebar-left">
          <span className="vp-cal-anno">{year}</span>
          <h2 className="vp-cal-month-name">{MONTH_NAMES[month]}</h2>
        </div>
        <div className="vp-cal-titlebar-right">
          <span className="vp-cal-count">{concertInfos.length} CONCERTS</span>
          <button onClick={handlePrev} className="vp-cal-nav-btn" aria-label="이전 달">‹</button>
          <button onClick={handleToday} className="vp-cal-nav-btn-today">TODAY</button>
          <button onClick={handleNext} className="vp-cal-nav-btn" aria-label="다음 달">›</button>
        </div>
      </div>

      <div className={`vp-cal-body${showSidebar ? ' sidebar-visible' : ''}`}>
        {/* 메인 그리드 영역 */}
        <div className="vp-cal-grid-area">
          {/* 요일 헤더 */}
          <div className="vp-cal-weekdays">
            {WEEKDAYS.map((d, i) => (
              <div key={d} className={`vp-cal-weekday${i === 0 ? ' sun' : ''}`}>{d}</div>
            ))}
          </div>

          {/* 날짜 셀 그리드 */}
          <div className="vp-cal-grid">
            {grid.map((d, i) => {
              const concerts  = d ? (concertsByDate.get(d) || []) : [];
              const has       = concerts.length > 0;
              const isToday   = isCurrentMonth && d === today;
              const dow       = i % 7;
              const isWeekend = dow === 0 || dow === 6;

              return (
                <div
                  key={i}
                  onClick={() => d && handleCellClick(d, concerts)}
                  className={[
                    'vp-cal-cell',
                    !d ? 'empty' : '',
                    isToday ? 'today' : '',
                    has ? 'has-concerts' : '',
                  ].filter(Boolean).join(' ')}
                >
                  {d && (
                    <>
                      <div className="vp-cal-cell-top">
                        <span className={[
                          'vp-cal-date-num',
                          isToday ? 'today' : '',
                          isWeekend && !isToday ? 'weekend' : '',
                        ].filter(Boolean).join(' ')}>
                          {d}
                        </span>
                        {isToday && (
                          <span className="vp-cal-today-badge">TODAY</span>
                        )}
                      </div>

                      <div className="vp-cal-chips">
                        {concerts.slice(0, 2).map((c, j) => (
                          <div
                            key={c.concertNo ?? j}
                            className="vp-cal-chip"
                            style={{
                              '--chip-bg': tintFor(c),
                              '--chip-hover-bg': hoverTintFor(c),
                              '--chip-accent': accentFor(c),
                            }}
                            onClick={(e) => { e.stopPropagation(); openDetail(c, d); }}
                            role="button"
                            tabIndex={0}
                            aria-label={`${c.artistNmKor} 공연 상세보기`}
                            onKeyPress={(e) => { if (e.key === 'Enter') { e.stopPropagation(); openDetail(c, d); } }}
                          >
                            <div className="vp-chip-artist">
                              {c.artistNmKor || c.artistNmEng}
                            </div>
                          </div>
                        ))}
                        {concerts.length > 2 && (
                          <div
                            className="vp-cal-more"
                            onClick={(e) => { e.stopPropagation(); openList(d, concerts); }}
                            role="button"
                            tabIndex={0}
                            aria-label={`${d}일 전체 공연 보기`}
                          >
                            +{concerts.length - 2} 더보기
                          </div>
                        )}
                      </div>
                    </>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* 사이드바 - 날짜 클릭 시에만 표시 */}
        {showSidebar && selectedDay && (
          <aside className="vp-cal-sidebar">
            <div className="vp-cal-sidebar-header">
              <div className="vp-cal-sidebar-eyebrow">{selectedDay}일의 공연</div>
              <button
                className="vp-cal-sidebar-close"
                onClick={closeSidebar}
                aria-label="닫기"
              >✕</button>
            </div>
            <div className="vp-cal-sidebar-list">
              {(concertsByDate.get(selectedDay) || []).length === 0 ? (
                <div className="vp-cal-sidebar-empty">등록된 공연이 없습니다.</div>
              ) : (
                (concertsByDate.get(selectedDay) || []).map((c, i) => (
                  <div
                    key={c.concertNo ?? i}
                    className="vp-cal-sidebar-item"
                    onClick={() => openDetail(c)}
                    role="button"
                    tabIndex={0}
                    aria-label={`${c.artistNmKor} 공연 상세보기`}
                    onKeyPress={(e) => { if (e.key === 'Enter') openDetail(c); }}
                  >
                    <div className="vp-sidebar-info">
                      <div className="vp-sidebar-artist">{c.artistNmKor || c.artistNmEng}</div>
                      <div className="vp-sidebar-meta">
                        {formatTime(c.concertTime)}{c.venue ? ` · ${c.venue}` : ''}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </aside>
        )}
      </div>

      <ConcertListSlide
        concerts={slideState.list.concerts}
        date={slideState.list.date}
        isOpen={slideState.list.isOpen}
        onClose={closeList}
        onSelectConcert={openDetail}
      />
      <ConcertDetailSlide
        concert={slideState.detail.concert}
        isOpen={slideState.detail.isOpen}
        onClose={closeDetail}
      />
    </div>
  );
}

export default Calendar;

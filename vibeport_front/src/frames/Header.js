import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import client from '../api/client';
import '../styles/header.css';

const NAV_LINKS = [
  ['/', 'Today'],
  ['/calendar', 'Calendar'],
  ['/archive', 'Archive'],
  ['/about', 'About'],
];

const DAYS      = ['SUN','MON','TUE','WED','THU','FRI','SAT'];
const DAY_FULL  = ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'];
const MONTHS    = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const [featureArtist, setFeatureArtist] = useState(null);

  useEffect(() => {
    client.get('/concert/getTodayFeature')
      .then(res => { if (res.data) setFeatureArtist(res.data); })
      .catch(() => {});
  }, []);

  const now    = new Date();
  const mm     = String(now.getMonth() + 1).padStart(2, '0');
  const dd     = String(now.getDate()).padStart(2, '0');
  const dayStr = DAYS[now.getDay()];
  const dateStamp = `${mm}.${dd} ${dayStr}`;
  const issueDateStamp = `${DAY_FULL[now.getDay()]}, ${MONTHS[now.getMonth()]} ${now.getDate()}, ${now.getFullYear()}`;

  return (
    <header className="vp-header">
      {/* 메인 네비바 */}
      <div className="vp-header-navbar">
        <div className="vp-header-logo-group">
          <Link to="/" className="vp-header-logo">Vibeport</Link>
          <span className="vp-header-tagline">KOREA · DAILY</span>
        </div>

        <nav className="vp-header-nav">
          {NAV_LINKS.map(([path, label]) => (
            <Link
              key={path}
              to={path}
              className={`vp-nav-link${location.pathname === path ? ' active' : ''}`}
            >
              {label}
            </Link>
          ))}
        </nav>

        <div className="vp-header-meta">
          <span className="vp-header-date">{dateStamp}</span>
        </div>
      </div>

      {/* 이슈바 */}
      <div className="vp-header-issuebar">
        <span>{issueDateStamp}</span>
        <span>오늘의 추천 — {featureArtist ? (featureArtist.artistNmFor || featureArtist.artistNmKor).toUpperCase() : '—'}</span>
      </div>
    </header>
  );
}

export default Header;

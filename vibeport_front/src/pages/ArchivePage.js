import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';
import EmailPreview from '../components/EmailPreview';
import '../styles/archive.css';
import { htmlToPlainText } from '../utils/TextUtils';

function formatListDate(regTime) {
  if (!regTime) return '';
  return String(regTime).slice(0, 10).replace(/-/g, '.');
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

function ArchivePage() {
  const navigate = useNavigate();
  const [letters, setLetters] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    client.get('/user/getSentLetters')
      .then(res => {
        const items = Array.isArray(res.data) ? res.data : [];
        setLetters(items);
        setSelectedId(items[0]?.msgNo ?? null);
      })
      .catch(() => {
        setLetters([]);
        setSelectedId(null);
      })
      .finally(() => setIsLoading(false));
  }, []);

  const normalizedLetters = useMemo(() => {
    return letters.map(item => {
      const artistName = item.artistNmFor || item.artistNmKor || 'Vibeport';
      const title = item.artistNmKor && item.artistNmFor
        ? `${item.artistNmKor} · ${item.artistNmFor}`
        : artistName;
      const plainContent = htmlToPlainText(item.content);

      return {
        ...item,
        plainContent,
        snippet: plainContent,
        title,
        formattedDate: formatListDate(item.regTime),
      };
    });
  }, [letters]);

  useEffect(() => {
    if (!normalizedLetters.length || typeof Image === 'undefined') return;

    normalizedLetters.slice(0, 5).forEach(item => {
      const imageSrc = resolveImageSrc(item.artistImageUrl);
      if (!imageSrc) return;

      const image = new Image();
      image.src = imageSrc;
    });
  }, [normalizedLetters]);

  const selected = useMemo(() => {
    return normalizedLetters.find(item => item.msgNo === selectedId) || normalizedLetters[0] || null;
  }, [normalizedLetters, selectedId]);

  const handleReadMore = useCallback(() => {
    if (!selected) return;
    navigate('/calendar', {
      state: {
        year: selected.concertYear,
        month: selected.concertMonth - 1,
        date: selected.concertDate,
        concert: selected,
      },
    });
  }, [navigate, selected]);

  const handleSelect = useCallback((msgNo) => {
    setSelectedId(currentId => currentId === msgNo ? currentId : msgNo);
  }, []);

  return (
    <div className="vp-archive-page">
      <div className="vp-archive-desc">
        <div className="vp-archive-eyebrow">뉴스레터 아카이브</div>
        <h2 className="vp-archive-h2">
          최근 발송된<br />편지를 다시 열어보세요
        </h2>
        <p className="vp-archive-body">
          최근 발송된 뉴스레터 5개를 모아 보여줍니다. 목록에서 편지를 선택하면<br/>
          오른쪽에서 실제 이메일 미리보기를 확인할 수 있어요.
        </p>

        {isLoading ? (
          <div className="vp-archive-empty">발송 목록을 불러오는 중입니다.</div>
        ) : (
          <ul className="vp-archive-list" role="listbox">
            {normalizedLetters.map(item => {
              return (
                <li
                  key={item.msgNo}
                  role="option"
                  aria-selected={item.msgNo === selected?.msgNo}
                  className={`vp-archive-list-item${item.msgNo === selected?.msgNo ? ' is-active' : ''}`}
                  onClick={() => handleSelect(item.msgNo)}
                >
                  <div className="vp-archive-list-row">
                    <div className="vp-archive-list-title">{item.title}</div>
                    <div className="vp-archive-list-date">{item.formattedDate}</div>
                  </div>
                  <div className="vp-archive-list-snippet">
                    {item.snippet}
                  </div>
                </li>
              );
            })}
            {!normalizedLetters.length && (
              <li className="vp-archive-list-item">
                <div className="vp-archive-list-snippet">발송된 뉴스레터가 없습니다.</div>
              </li>
            )}
          </ul>
        )}
      </div>

      <EmailPreview
        feature={selected}
        className="vp-archive-preview"
        isLoading={isLoading}
        loadingMessage="오늘의 메일 미리보기를 불러오는 중입니다."
        emptyMessage="목록에서 뉴스레터를 선택해 주세요."
        bodyClassName="vp-archive-essay-clamp"
        showReadMore
        onReadMore={handleReadMore}
      />
    </div>
  );
}

export default ArchivePage;

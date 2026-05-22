import React, { useState, useEffect } from 'react';
import client from '../api/client'; // axios 인스턴스(예: src/api/client.js)
import '../styles/mypage.css';
import { useSelector } from 'react-redux';

export default function MyPage() {
  const userInfo = useSelector(state => state.user.userInfo);
  const [editing, setEditing] = useState(false);
  const [profile, setProfile] = useState({
    name: '홍길동',
  });

  const [ratedShows, setRatedShows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let canceled = false;
    const fetchUserData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [profileRes, ratingsRes] = await Promise.all([
          client.get('/user/getUserProfile'),
          client.get('/user/getUserRatings'),
        ]);

        if (canceled) return;

        if (profileRes?.data) setProfile(prev => ({ ...prev, ...profileRes.data }));
        if (ratingsRes?.data) setRatedShows(ratingsRes.data);
      } catch (err) {
        if (!canceled) {
          console.error('Failed to fetch user data', err);
          setError('사용자 정보를 불러오지 못했습니다.');
        }
      } finally {
        if (!canceled) setLoading(false);
      }
    };

    fetchUserData();
    return () => {
      canceled = true;
    };
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setProfile(prev => ({ ...prev, [name]: value }));
  };

  const handleSave = () => {
    setEditing(false);
  };

  const handleLogout = () => {
    console.log('logout');
  };

  const renderStars = (n) => {
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(
        <span key={i} className={i <= n ? 'star filled' : 'star'} aria-hidden="true">
          ★
        </span>
      );
    }
    return stars;
  };

  return (
    <div className="mypage-wrapper">
      <header className="mypage-header">
        <h1>마이페이지</h1>
        <div className="mypage-actions">
          <button className="btn btn-ghost" onClick={() => setEditing(!editing)}>
            {editing ? '취소' : '편집'}
          </button>
          <button className="btn btn-logout" onClick={handleLogout}>로그아웃</button>
        </div>
      </header>

      <section className="profile-card">
        <div className="avatar" aria-hidden="true">홍</div>
        <div className="profile-info">
          <label>
            이름
            <input name="name" value={profile.name} onChange={handleChange} disabled={!editing} />
          </label>

          <div className="profile-buttons">
            {editing ? (
              <button className="btn btn-primary" onClick={handleSave}>저장</button>
            ) : null}
          </div>
        </div>
      </section>

      <section className="activity-section">
        <h2>평가한 공연</h2>

        {loading ? (
          <div className="empty">로딩 중...</div>
        ) : error ? (
          <div className="empty">{error}</div>
        ) : (
          <div className="rated-list">
            {ratedShows.length === 0 ? (
              <div className="empty">아직 평가한 공연이 없습니다.</div>
            ) : (
              ratedShows.map(show => (
                <div key={show.id} className="rated-item">
                  <img className="poster" src={show.poster} alt={`${show.title} 포스터`} />
                  <div className="rated-info">
                    <div className="show-title">{show.title}</div>
                    <div className="rating-row">
                      <div className="rating-stars" aria-hidden="true">{renderStars(show.rating)}</div>
                      <div className="rating-date">{new Date(show.date).toLocaleDateString()}</div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </section>
    </div>
  );
}
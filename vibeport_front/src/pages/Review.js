import React, { useState } from 'react';
import '../styles/review.css';

export default function ReviewPage() {
  const [commentText, setCommentText] = useState('');
  const [comments] = useState([
    {
      id: 1,
      avatar: 'https://via.placeholder.com/48',
      user: 'user01',
      stars: 5,
      text: '정말 감동적인 공연이었어요!',
      date: '2025-10-12',
      likes: 12,
    },
    {
      id: 2,
      avatar: 'https://via.placeholder.com/48',
      user: 'user02',
      stars: 4,
      text: '무대 연출이 인상적.',
      date: '2025-09-05',
      likes: 4,
    },
    {
      id: 3,
      avatar: 'https://via.placeholder.com/48',
      user: 'user03',
      stars: 3,
      text: '좋았지만 좌석이 불편했어요.',
      date: '2025-08-20',
      likes: 2,
    },
  ]);

  const renderStars = (n) => {
    return Array.from({ length: 5 }, (_, i) => (
      <span key={i} className={i < n ? 'star filled' : 'star'}>★</span>
    ));
  };

  return (
    <div className="review-page">
      <main className="review-container">
        <section className="review-top">
          <div className="poster-box">
            <img className="poster-review" src="https://via.placeholder.com/200x260?text=Poster" alt="포스터" />
          </div>

          <div className="info-box">
            <input className="input title" defaultValue="콘서트 이름" />
            <input className="input artist" defaultValue="가수" />
            <textarea className="input desc" defaultValue="공연 설명이 들어갑니다. 공연의 간단한 소개 및 특징을 적어주세요." />
            <div className="write-row">
              <div className="label">별점</div>
              <div className="stars interactive">{renderStars(4)}</div>
            </div>

            <div className="write-comment">
              <div className="label">직접 적는 코멘트</div>
              <textarea
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                placeholder="공연에 대한 감상이나 평을 적어보세요."
              />
              <div className="write-actions">
                <button className="btn btn-primary">작성</button>
              </div>
            </div>
          </div>
        </section>

        <section className="comments-section">
          {comments.map(c => (
            <article key={c.id} className="comment-card">
              <img className="c-avatar" src={c.avatar} alt={`${c.user} avatar`} />
              <div className="c-body">
                <div className="c-header">
                  <div className="c-user">{c.user}</div>
                  <div className="c-stars">{renderStars(c.stars)}</div>
                </div>
                <div className="c-text">{c.text}</div>
                <div className="c-footer">
                  <div className="c-date">{new Date(c.date).toLocaleDateString()}</div>
                  <div className="c-actions">
                    <button className="small-btn">좋아요 {c.likes}</button>
                    <button className="small-btn">댓글 표시</button>
                  </div>
                </div>
              </div>
            </article>
          ))}
        </section>
      </main>
    </div>
  );
}
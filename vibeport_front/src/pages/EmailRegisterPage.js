import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';
import EmailPreview from '../components/EmailPreview';
import '../styles/archive.css';
import '../styles/email-register.css';
import { isValidEmail } from '../utils/CommonUtils';

function Eyebrow({ children, accent = 'var(--brick)' }) {
  return (
    <div className="vp-eyebrow" style={{ color: accent }}>
      {children}
    </div>
  );
}

function EmailRegisterPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [stage, setStage] = useState('idle');
  const [error, setError] = useState(null);
  const [feature, setFeature] = useState(null);

  useEffect(() => {
    client.get('/concert/getTodayFeature')
      .then(res => { if (res.data) setFeature(res.data); })
      .catch(() => {});
  }, []);

  const isValid = isValidEmail(email);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!isValid) {
      setError('유효한 이메일 주소를 입력해 주세요.');
      return;
    }
    setStage('sending');
    try {
      await client.post('/user/sendVerificationCode', { email });
      setStage('sent');
    } catch (err) {
      setStage('idle');
      setError(err.response?.data?.message || '인증번호 전송에 실패했습니다.');
    }
  };

  const handleVerify = async () => {
    setError(null);
    if (!code) {
      setError('인증코드를 입력해 주세요.');
      return;
    }
    setStage('verifying');
    try {
      const param = { email, code };
      await client.post('/user/verifyCode', param);
      await client.post('/user/registEmail', param);
      setStage('done');
    } catch (err) {
      setStage('sent');
      setError('인증코드 검증에 실패했습니다.');
    }
  };

  const handleReadMore = () => {
    if (!feature) return;
    navigate('/calendar', {
      state: {
        year: feature.concertYear,
        month: feature.concertMonth - 1,
        date: feature.concertDate,
        concert: feature,
      },
    });
  };

  return (
    <div className="vp-er-page">
      <div className="vp-er-grid">
        <div className="vp-er-left">
          <div>
            <Eyebrow>내한 큐레이션 뉴스레터</Eyebrow>
            <h1 className="vp-er-h1">
              한국에 오는<br />
              <span className="vp-er-h1-accent">목소리</span>들을,
            </h1>
            <h2 className="vp-er-h2">매일 아침 당신의 메일함으로</h2>
          </div>

          <div className="vp-er-form-wrap">
            <p className="vp-er-desc">
              큐레터가 고른 오늘의 아티스트 한 명. 아티스트에 대한 설명,
              추천곡과 공연 정보를 <br/>
              매일 9시 당신의 아침에 전해요.
            </p>

            {stage !== 'done' ? (
              <form onSubmit={handleSubmit} className="vp-er-form">
                <div className="vp-er-email-row">
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@email.com"
                    readOnly={stage !== 'idle'}
                    className="vp-er-email-input"
                  />
                  <button
                    type="submit"
                    disabled={!isValid || stage !== 'idle'}
                    className={`vp-er-submit-btn ${isValid && stage === 'idle' ? 'active' : 'inactive'}`}
                  >
                    {stage === 'sending' ? '전송 중...' : '구독'}
                  </button>
                </div>

                {(stage === 'sent' || stage === 'verifying') && (
                  <div className="vp-er-verify-box">
                    <Eyebrow accent="var(--olive)">인증코드 입력</Eyebrow>
                    <p className="vp-er-verify-desc">
                      {email}로 6자리 인증코드를 보냈어요.
                    </p>
                    <div className="vp-er-verify-row">
                      <input
                        value={code}
                        onChange={(e) => setCode(e.target.value)}
                        placeholder="------"
                        className="vp-er-code-input"
                      />
                      <button
                        type="button"
                        onClick={handleVerify}
                        disabled={!code || stage === 'verifying'}
                        className="vp-er-verify-btn"
                      >
                        {stage === 'verifying' ? '확인 중...' : '확인'}
                      </button>
                    </div>
                  </div>
                )}

                {error && <div className="vp-er-error">{error}</div>}
              </form>
            ) : (
              <div className="vp-er-done-box">
                <Eyebrow accent="var(--olive)">구독 완료</Eyebrow>
                <h3 className="vp-er-done-h3">내일 아침 9시에 만나요</h3>
                <p className="vp-er-done-body">
                  첫 메일은 내일 아침 도착해요. 그동안{' '}
                  <span onClick={() => navigate('/calendar')} className="vp-er-done-link">
                    이번 달 캘린더
                  </span>
                  를 둘러보세요.
                </p>
              </div>
            )}
          </div>
        </div>

        <div className="vp-er-right">
          <EmailPreview
            feature={feature}
            bodyClassName="vp-er-essay-clamp"
            showReadMore
            onReadMore={handleReadMore}
            emptyMessage="공연 정보를 불러오는 중..."
          />
        </div>
      </div>
    </div>
  );
}

export default EmailRegisterPage;

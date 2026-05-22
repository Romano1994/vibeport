import React, { useEffect, useState } from 'react';
import ReactDOM from 'react-dom';
import { isValidEmail } from '../utils/CommonUtils';
import '../styles/login-popup.css';
import { loginUser } from '../store/userSlice.js';
import { useDispatch } from 'react-redux';

export default function LoginPopup({ open, onClose }) {
  const dispatch = useDispatch();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [isEmaliFormat, setIsEmaliFormat] = useState(false);  // 이메일 포멧 확인 여부
  const [isPasswordValid, setIsPasswordValid] = useState(false);  // 비밀번호 포멧 확인 여부
  const [passwordErrorMsg, setPasswordErrorMsg] = useState('');   // 비밀번호 에러 메세지

  useEffect(() => {
    if(!open) return;

    // 팝업 열릴 떄 초기화
    setEmail('');
    setPassword('');
    setError(null);
    setIsEmaliFormat(false);

    const onKey = (e) => {
        if (e.key === 'Escape') onClose();
    }

    document.addEventListener('keydown', onKey);

    // 클린업 함수 반환
    // effect가 다시 실행되기 전에 호출됨
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  /*
   * 로그인
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    if (!isPasswordValid) {
        setError('비밀번호를 다시 입력해주세요.');
        return;
    }

    try {
        await dispatch(loginUser({email, password})).unwrap();

        onClose();
    } catch (err) {
      console.error(err);
      setError('로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  /*
   * 이메일 입력
   */
  const fn_emailOnChange = (value) => {
    setEmail(value);
    setIsEmaliFormat(isValidEmail(value));
  }

  /*
    * 비밀번호 입력 - 비밀번호 형식이 맞는지 확인
    */
  const fn_passwordOnChange = (value) => {
      setPassword(value);
      // 8자 이상, 영문, 특수문자 포함
      const re = /^(?=.*[A-Za-z])(?=.*[!@#$%^&*(),.?":{}|<>~`[\]\\\/;:'+=_\-]).{8,}$/;
      const valid = re.test(value);
      setIsPasswordValid(valid);
      setPasswordErrorMsg(valid ? '' : '비밀번호는 최소 8자이며 영문자와 특수문자를 포함해야 합니다.');
  };
  
  return ReactDOM.createPortal(
    <div className="lp-overlay" role="dialog" aria-modal="true" onMouseDown={(e) => {
      if (e.target === e.currentTarget) onClose();
    }}>
      <div className="lp-modal">
        <button className="lp-close" aria-label="닫기" onClick={onClose}>×</button>
        <h2 className="lp-title">로그인</h2>
        <form className="lp-form" onSubmit={handleSubmit}>
          <label>
            이메일
            <input type="email" value={email} onChange={(e) => fn_emailOnChange(e.target.value)} required />
          </label>
          <label>
            비밀번호
            <input type="password" value={password} onChange={(e) => fn_passwordOnChange(e.target.value)} required />
            {passwordErrorMsg && <div className="sp-error">{passwordErrorMsg}</div>}
          </label>

          {error && <div className="lp-error">{error}</div>}

          <div className="lp-actions">
            <button type="submit" className="btn" disabled={!isEmaliFormat || loading}>
              {loading ? '로그인 중...' : '로그인'}
            </button>
            <button type="button" className="btn" onClick={onClose}>취소</button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
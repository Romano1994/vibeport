import React, { useEffect, useRef, useState } from 'react';
import ReactDOM from 'react-dom';
import '../styles/signup-popup.css';
import client from '../api/client';
import { isValidEmail } from '../utils/CommonUtils';
import AlertPopup from './AlertPopup';

export default function SignupPopup({ open, onClose }) {
    const [email, setEmail] = useState(''); // 이메일
    const [code, setCode] = useState('');   // 인증코드
    const [isSent, setIsSent] = useState(false);    // 인증코드 발송 여부
    const [verified, setVerified] = useState(false);    // 인증코드 확인 여부
    const [verifiedEmail, setVerifiedEmail] = useState('');
    const [password, setPassword] = useState('');   // 비밀번호
    const [confirm, setConfirm] = useState(''); // 확인 비밀번호
    const [loading, setLoading] = useState(false);  // 로딩
    const [error, setError] = useState(null);   // 에러 메세지
    const [isEmaliFormat, setIsEmaliFormat] = useState(false);  // 이메일 포멧 확인 여부
    const [isPasswordValid, setIsPasswordValid] = useState(false);  // 비밀번호 포멧 확인 여부
    const [passwordErrorMsg, setPasswordErrorMsg] = useState('');   // 비밀번호 에러 메세지
    const [showAlert, setShowAlert] = useState(false);  // alert 팝업창
    const [alertMessage, setAlertMessage] = useState('');   // alert message

    const firstRef = useRef(null);

    useEffect(() => {
        if (!open) return;
        // 초기화
        setEmail('');
        setCode('');
        setIsSent(false);
        setVerified(false);
        setVerifiedEmail(null);
        setPassword('');
        setConfirm('');
        setLoading(false);
        setError(null);
        setIsPasswordValid(false);
        setPasswordErrorMsg('');
        setIsEmaliFormat(false);

        // 포커스
        setTimeout(() => firstRef.current?.focus?.(), 0);
    }, [open]);

    if (!open) return null;

    // 인증코드 전송
    const sendCode = async () => {
        setError(null);
        setVerified(false);
        if (!email) {
            setError('이메일을 입력해주세요.');
            return;
        }
        setLoading(true);
        try {
            await client.post('/user/sendVerificationCode', { "email" : email });
            setIsSent(true);

        } catch (err) {
            console.error(err);
            setError('인증번호 전송에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 인증코드 확인
    const verifyCode = async () => {
        setError(null);
        if (!code) {
            setError('인증코드를 입력해주세요.');
            return;
        }
        setLoading(true);
        try {
            // 확인용 파라미터
            const param = {
                "email": email
                , "code": code
            };

            await client.post('/user/verifyCode', param)
            .then(() => {
                setVerified(true);
                setVerifiedEmail(email);
            })
            .catch(() => {
                setVerified(false);
                setError('인증코드가 올바르지 않습니다.');
            });
        } catch (err) {
            setError('인증코드 검증에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // 회원가입 로직
    const handleSignupSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        // 반드시 현재 입력한 이메일이 인증된 이메일과 일치해야 가입 허용
        if (!verified || !verifiedEmail || verifiedEmail !== email) {
            setError('현재 입력된 이메일은 인증되지 않았습니다. 이메일 인증 후 회원가입하세요.');
            return;
        }

        if (!email) {
            setError('이메일이 입력되지 않았습니다.');
            return;
        }
        if (!password) {
            setError('비밀번호가 입력되지 않았습니다.');
            return;
        }
        if (password !== confirm) {
            setError('비밀번호가 일치하지 않습니다.');
            return;
        }
        if (!isPasswordValid) {
            setError('비밀번호를 다시 입력해주세요.');
            return;
        }

        setLoading(true);
        try {
            const param = { email, password };
            await client.post('/user/join', param);

            // 성공: 알림 띄우기 (팝업은 즉시 닫지 않고, 알림 닫을 때 onClose 호출)
            setAlertMessage('회원가입이 완료되었습니다.');
            setShowAlert(true);
        } catch (err) {
            console.error(err);
            setError('회원가입 중 에러가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    /*
     * 이메일 입력 - 이메일 형식이 맞는지 확인
     */
    const fn_emailOnChange = (e) => {
        setEmail(e);

        if(isValidEmail(email)) {
            setIsEmaliFormat(true);
        } else {
            setIsEmaliFormat(false);
        }
    };

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
        <div
            className="sp-overlay"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sp-title"
            onMouseDown={(e) => {
                if (e.target === e.currentTarget) onClose();
            }}
        >
            <div className="sp-modal" onMouseDown={(e) => e.stopPropagation()}>
                 <button className="sp-close" aria-label="닫기" onClick={onClose}>×</button>
                 <h2 id="sp-title">회원가입</h2>
 
                 <form className="sp-form" onSubmit={handleSignupSubmit} noValidate>
                     <label>
                         이메일
                         <input
                             ref={firstRef}
                             type="email"
                             value={email}
                             onChange={(e) => fn_emailOnChange(e.target.value)}
                             disabled={isSent}
                         />
                     </label>
 
                     <div className="sp-send-row">
                         <button className="btn" type="button" onClick={sendCode} disabled={!isEmaliFormat || loading || isSent}>
                             {loading ? '전송 중...' : isSent ? '전송됨' : '인증번호 전송'}
                         </button>
                     </div>
 
                     {isSent && (
                         <>
                             <label>
                                 인증코드
                                 <input type="text" value={code} onChange={(e) => setCode(e.target.value)} readOnly={verified}/>
                             </label>
 
                             <div className="sp-verify-row">
                                 <button type="button" className="btn" onClick={verifyCode} disabled={loading || verified}>
                                     {verified ? '인증됨' : '인증 확인'}
                                 </button>
                                 <button type="button" className="btn btn-ghost"
                                     onClick={() => {
                                         // 재전송(입력 초기화)
                                         setCode('');
                                         sendCode();
                                     }}
                                     disabled={loading}
                                 >
                                     재전송
                                 </button>
                                 {verified && <div className="sp-verified">이메일 인증이 완료되었습니다.</div>}
                             </div>
                         </>
                     )}
 
                     <label>
                         비밀번호
                         <input type="password" value={password} onChange={(e) => fn_passwordOnChange(e.target.value)} />
                         {passwordErrorMsg && <div className="sp-error">{passwordErrorMsg}</div>}
                     </label>
 
                     <label>
                         비밀번호 확인
                         <input type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />
                     </label>
 
                     {error && <div className="sp-error">{error}</div>}
 
                     <div className="sp-actions">
                         <button type="submit" className="btn btn-primary" disabled={loading}>
                             {loading ? '가입 중...' : '회원가입'}
                         </button>
                         <button type="button" className="btn btn-ghost" onClick={onClose} disabled={loading}>취소</button>
                     </div>
                 </form>
            </div>
            <AlertPopup
              open={showAlert}
              message={alertMessage}
              onClose={() => {
                setShowAlert(false);
                // 팝업도 함께 닫음
                onClose();
              }}
            />
        </div>,
         document.body
     );
}
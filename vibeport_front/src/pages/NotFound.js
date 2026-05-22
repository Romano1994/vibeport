import { Link } from 'react-router-dom';
import '../styles/notfound.css';

function NotFound() {
  return (
    <div className="vp-notfound-page">
      <div className="vp-notfound-eyebrow">Error</div>
      <h1 className="vp-notfound-code">404</h1>
      <p className="vp-notfound-desc">페이지를 찾을 수 없습니다.</p>
      <Link to="/" className="vp-notfound-home-link">홈으로 →</Link>
    </div>
  );
}

export default NotFound;

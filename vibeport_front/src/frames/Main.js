import Header from './Header';
import AlertHost from '../components/AlertHost';

function Main({ children }) {
  return (
    <div className="vp-shell">
      <Header />
      <main className="vp-main">
        {children}
      </main>
      <AlertHost />
    </div>
  );
}

export default Main;

import './styles/global.css';
import { Routes, Route } from 'react-router-dom';
import EmailRegister from './pages/EmailRegisterPage';
import Calendar from './pages/Calendar';
import About from './pages/About';
import NotFound from './pages/NotFound';
import Main from './frames/Main';
import MyPage from './pages/MyPage';
import Review from './pages/Review';
import ArchivePage from './pages/ArchivePage';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <Main>
      <Routes>
        <Route path="/" element={<EmailRegister />} />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="/about" element={<About />} />
        <Route path="/archive" element={<ArchivePage />} />
        <Route
          path="/mypage"
          element={
            <ProtectedRoute>
              <MyPage />
            </ProtectedRoute>
          }
        />
        <Route path="/review" element={<Review />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Main>
  );
}

export default App;

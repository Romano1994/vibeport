import '../styles/about.css';

function About() {
  return (
    <div className="vp-about-page">
      <div className="vp-about-eyebrow">About</div>
      <h1 className="vp-about-h1">
        큐레이션은 결국,<br />한 사람의 취향에서 시작됩니다.
      </h1>
      <p className="vp-about-body">
        Vibeport는 2026년 봄 시작된 작은 뉴스레터입니다. 한국에 오는 아티스트들을
        큐레이터의 눈으로 골라, 매일 아침 7시 한 통의 편지로 보내드립니다.
      </p>
      <p className="vp-about-body">
        추천곡 다섯 곡, 짧은 에세이, 이번 주 티켓 오픈 정보. 스팸 없이, 광고 없이.
        오직 음악과 공연 이야기만 담습니다.
      </p>
    </div>
  );
}

export default About;

import React from 'react';

function Footer() {
  return (
    <footer className="vp-footer">
      <div className="container">
        <span>© {new Date().getFullYear()} Vibeport</span>
      </div>
    </footer>
  );
}

export default Footer;



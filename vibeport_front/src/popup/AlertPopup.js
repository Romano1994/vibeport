import React from 'react';
import ReactDOM from 'react-dom';
import '../styles/alert-modal.css';

export default function AlertPopup({ open, message = '', type = 'alert', onClose, onCancel }) {
  if (!open) return null;

  return ReactDOM.createPortal(
    <div className="am-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }} role="dialog" aria-modal="true">
      <div className="am-modal" onMouseDown={(e) => e.stopPropagation()}>
        <div className="am-body">
          <p>{message}</p>
        </div>
        <div className="am-actions">
          <button className="btn btn-primary" onClick={onClose}>확인</button>
          {type === 'confirm' && (
            <button className="btn" onClick={onCancel}>취소</button>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
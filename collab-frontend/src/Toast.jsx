import React, { useEffect } from "react";

export default function Toast({ message, type = "success", onClose, ttl = 2600 }) {
  useEffect(() => {
    const t = setTimeout(() => onClose?.(), ttl);
    return () => clearTimeout(t);
  }, [ttl, onClose]);

  return (
    <div className={`toastX ${type}`} role="status">
      <div className="toastXIcon">{type === "error" ? "⚠️" : "✅"}</div>
      <div className="toastXMsg">{message}</div>
      <button className="toastXClose" onClick={onClose} aria-label="Close">
        ✕
      </button>
    </div>
  );
}

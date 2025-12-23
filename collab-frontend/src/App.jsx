import { Routes, Route, Navigate, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { clearAuth, loadAuth, saveAuth } from "./auth";
import RegisterPage from "./pages/RegisterPage";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import EditorPage from "./pages/EditorPage";

export default function App() {
  const [auth, setAuth] = useState(loadAuth());
  const [isDark, setIsDark] = useState(() => localStorage.getItem("theme") === "dark");
  const nav = useNavigate();

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  }, [isDark]);

  useEffect(() => { saveAuth(auth); }, [auth]);

  function logout() {
    clearAuth();
    setAuth(null);
    nav("/login");
  }

  return (
    <div className="container">
      <div className="nav">
        <div className="brand">
          <div className="badge" />
          <div>Collab Editing <span className="small">— User Portal</span></div>
        </div>

        <div className="row">
          {/* THE TOGGLE BUTTON */}
          <button className="btn" onClick={() => setIsDark(!isDark)}>
            {isDark ? "☀️ Light" : "🌙 Dark"}
          </button>
          
          {auth && (
            <>
              <span className="small">Signed in as <b>{auth.username}</b></span>
              <button className="btn btnDanger" onClick={logout}>Logout</button>
            </>
          )}
        </div>
      </div>

      <div style={{ height: 16 }} />

      <Routes>
        <Route path="/" element={<Navigate to={auth ? "/dashboard" : "/login"} replace />} />
        <Route path="/register" element={auth ? <Navigate to="/dashboard" replace /> : <RegisterPage onDone={() => nav("/login")} />} />
        <Route path="/login" element={auth ? <Navigate to="/dashboard" replace /> : <LoginPage onLogin={(a) => { setAuth(a); nav("/dashboard"); }} />} />
        <Route
          path="/dashboard"
          element={
            auth ? <DashboardPage auth={auth} onLogout={logout} /> : <Navigate to="/login" replace />
          }
        />

        <Route path="/edit/:id" element={auth ? <EditorPage auth={auth} /> : <Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}
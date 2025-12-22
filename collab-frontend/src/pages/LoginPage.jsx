import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../Api";

export default function LoginPage({ onLogin }) {
  const nav = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");

  async function submit(e) {
    e.preventDefault();
    setErr("");
    setBusy(true);

    const res = await login({ username, password });
    if (!res.ok) {
      setErr(res.message || "Login failed");
      setBusy(false);
      return;
    }

    onLogin?.({
      username: res.data.username,
      token: res.data.token,
      email: res.data.email,
    });

    setBusy(false);
  }

  return (
    <div className="authShell">
      <div className="card authHero">
        <div>
          <div className="badge" style={{ width: 18, height: 18, borderRadius: 7 }} />
          <div style={{ height: 12 }} />
          <h1 className="authTitle">Welcome back</h1>
          <p className="authSub">
            Sign in to manage documents, collaborate live, and save snapshots in version history.
          </p>
        </div>

        <div className="small">
          Don’t have an account?{" "}
          <button className="linkBtn" onClick={() => nav("/register")}>
            Create one
          </button>
        </div>
      </div>

      <div className="card authCard">
        <h1 className="h1">Login</h1>
        <p className="p">Enter your username and password to login into your account.</p>

        <form onSubmit={submit} style={{ marginTop: 12 }}>
          <label className="label">Username</label>
          <input
            className="input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
          />

          <label className="label">Password</label>
          <div className="inputWrap">
            <input
              className="input"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
            <button
              type="button"
              className="eyeBtn"
              onClick={() => setShowPassword((v) => !v)}
              title={showPassword ? "Hide" : "Show"}
            >
              {showPassword ? "🙈" : "👁️"}
            </button>
          </div>

          {err && <div className="alert">{err}</div>}

          <button
            className="btn btnPrimary"
            style={{ marginTop: 16, width: "100%" }}
            disabled={busy}
          >
            {busy ? "Authenticating..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  );
}

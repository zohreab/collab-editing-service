import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { register } from "../Api";

function EyeIcon({ open }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      {open ? (
        <path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      ) : (
        <>
          <path d="M3 3l18 18" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          <path d="M10.6 10.6a3 3 0 0 0 4.24 4.24" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M9.88 5.08A10.36 10.36 0 0 1 12 5c6.5 0 10 7 10 7a18.2 18.2 0 0 1-3.2 4.3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
          <path d="M6.2 6.2A18.3 18.3 0 0 0 2 12s3.5 7 10 7c1.25 0 2.4-.22 3.43-.6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
        </>
      )}
    </svg>
  );
}

export default function RegisterPage({ onDone }) {
  const nav = useNavigate();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");

  async function submit(e) {
    e.preventDefault();
    setErr("");
    setMsg("");
    setBusy(true);

    const res = await register({ username, email, password });
    if (!res.ok) {
      setErr(res.message || "Registration failed");
      setBusy(false);
      return;
    }

    setMsg("Account created. You can login now.");
    setBusy(false);

    setTimeout(() => onDone?.(), 400);
  }

  return (
    <div className="authShell">
      <div className="card authHero">
        <div>
          <div className="badge" style={{ width: 18, height: 18, borderRadius: 7 }} />
          <div style={{ height: 12 }} />
          <h1 className="authTitle">Create your account</h1>
          <p className="authSub">
            Register once, then create documents, invite collaborators, and edit together in real-time.
          </p>
        </div>

        <div className="small">
          Already have an account?{" "}
          <button className="linkBtn" onClick={() => nav("/login")}>
            Sign in
          </button>
        </div>
      </div>

      <div className="card authCard">
        <h1 className="h1">Register</h1>
        <p className="p">Create a new user account.</p>

        <form onSubmit={submit} style={{ marginTop: 12 }}>
          <label className="label">Username</label>
          <input
            className="input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Username"
            autoComplete="username"
          />

          <label className="label">Email</label>
          <input
            className="input"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Email"
            autoComplete="email"
          />

          <label className="label">Password</label>
          <div className="inputWrap">
            <input
              className="input"
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password"
              autoComplete="new-password"
            />
            <button
              type="button"
              className="eyeBtn"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? "Hide password" : "Show password"}
              title={showPassword ? "Hide password" : "Show password"}
            >
              <EyeIcon open={showPassword} />
            </button>
          </div>

          {err && <div className="alert" style={{ marginTop: 10 }}>{err}</div>}
          {msg && <div className="toast" style={{ marginTop: 10 }}>{msg}</div>}

          <button
            className="btn btnPrimary"
            style={{ marginTop: 16, width: "100%" }}
            disabled={busy}
          >
            {busy ? "Creating..." : "Create account"}
          </button>
        </form>
      </div>
    </div>
  );
}

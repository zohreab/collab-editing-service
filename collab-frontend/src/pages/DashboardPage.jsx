// src/pages/DashboardPage.jsx
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  getDocuments,
  createDocument,
  shareDocument,
  changePassword,
  deleteDocument,
  revokeDocumentAccess,
  deleteAccount,
} from "../Api";

import Toast from "../Toast";
import ConfirmDialog from "../ConfirmDialog";

/* ---------- Icons ---------- */
function Icon({ name, size = 18 }) {
  const common = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    xmlns: "http://www.w3.org/2000/svg",
  };
  const stroke = {
    stroke: "currentColor",
    strokeWidth: "1.8",
    strokeLinecap: "round",
    strokeLinejoin: "round",
  };
  if (name === "user")
    return (
      <svg {...common}>
        <path {...stroke} d="M20 21a8 8 0 0 0-16 0" />
        <path {...stroke} d="M12 11a4 4 0 1 0-4-4 4 4 0 0 0 4 4Z" />
      </svg>
    );
  if (name === "doc")
    return (
      <svg {...common}>
        <path {...stroke} d="M14 2H7a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8Z" />
        <path {...stroke} d="M14 2v6h6" />
      </svg>
    );
  if (name === "plus")
    return (
      <svg {...common}>
        <path {...stroke} d="M12 5v14M5 12h14" />
      </svg>
    );
  if (name === "refresh")
    return (
      <svg {...common}>
        <path {...stroke} d="M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
      </svg>
    );
  if (name === "share")
    return (
      <svg {...common}>
        <path {...stroke} d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8M16 6l-4-4-4 4M12 2v13" />
      </svg>
    );
  return null;
}

export default function DashboardPage({ auth, onLogout }) {
  const navigate = useNavigate();
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [newTitle, setNewTitle] = useState("");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [profileMsg, setProfileMsg] = useState("");

  const [sharingDocId, setSharingDocId] = useState(null);
  const [collaboratorName, setCollaboratorName] = useState("");
  const [view, setView] = useState("docs");

  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);

  // ✅ Toasts
  const [toasts, setToasts] = useState([]);
  function pushToast(message, type = "success") {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, message, type }]);
  }
  function removeToast(id) {
    setToasts((t) => t.filter((x) => x.id !== id));
  }

  // ✅ Confirm modal state (revoke/delete)
  const [confirm, setConfirm] = useState({
    open: false,
    title: "",
    message: "",
    danger: false,
    onConfirm: null,
  });

  function openConfirm({ title, message, danger = false, onConfirm }) {
    setConfirm({ open: true, title, message, danger, onConfirm });
  }

  function closeConfirm() {
    setConfirm({ open: false, title: "", message: "", danger: false, onConfirm: null });
  }

  const today = useMemo(
    () => new Date().toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric" }),
    []
  );

  async function loadData() {
    setLoading(true);
    const res = await getDocuments(auth.token);
    if (res.ok) {
      setDocs(res.data);
      setError("");
    } else {
      setError(res.message || "Note: Make sure DocService is running!");
    }
    setLoading(false);
  }

  async function handleCreateDoc(e) {
    if (e) e.preventDefault();
    if (!newTitle.trim()) {
      pushToast("Please enter a title", "error");
      return;
    }

    const res = await createDocument(auth.token, { title: newTitle, content: "" });
    if (res.ok) {
      setNewTitle("");
      await loadData();
      pushToast("Document created");
    } else {
      pushToast(res.message || "Failed to create document", "error");
    }
  }

  async function handlePasswordChange(e) {
    e.preventDefault();
    setProfileMsg("");

    const res = await changePassword({ token: auth.token, currentPassword, newPassword });
    if (res.ok) {
      setProfileMsg("Password updated!");
      setCurrentPassword("");
      setNewPassword("");
      pushToast("Password updated");
    } else {
      setProfileMsg("Error: " + (res.message || "Password update failed"));
      pushToast(res.message || "Password update failed", "error");
    }
  }

  function requestDeleteDoc(docId) {
    const doc = docs.find((d) => d.id === docId);
    openConfirm({
      title: "Delete document?",
      message: `Delete "${doc?.title || "this document"}" and all its history? This cannot be undone.`,
      danger: true,
      onConfirm: async () => {
        closeConfirm();
        const res = await deleteDocument(auth.token, docId);
        if (res.ok) {
          await loadData();
          pushToast("Document deleted");
        } else {
          pushToast(res.message || "Error deleting document", "error");
        }
      },
    });
  }

  function requestRevoke(docId, username) {
    openConfirm({
      title: "Revoke access?",
      message: `Remove access for "${username}"? They will no longer see or edit this document.`,
      danger: true,
      onConfirm: async () => {
        closeConfirm();
        const res = await revokeDocumentAccess(auth.token, docId, username);
        if (res.ok) {
          await loadData();
          pushToast(`Access revoked for ${username}`);
        } else {
          pushToast(res.message || "Failed to revoke access", "error");
        }
      },
    });
  }

  function requestDeleteAccount() {
    openConfirm({
      title: "Delete account?",
      message:
        "This will permanently delete your account and all documents you own (including their version history). This cannot be undone.",
      danger: true,
      onConfirm: async () => {
        closeConfirm();

        const res = await deleteAccount(auth.token);
        if (res.ok) {
          pushToast("Account deleted");
          onLogout?.(); // clears auth + navigates to login (your App logout already does this)
        } else {
          pushToast(res.message || "Failed to delete account", "error");
        }
      },
    });
  }


  async function handleInvite() {
    if (!sharingDocId) return;

    const name = collaboratorName.trim();
    if (!name) {
      pushToast("Enter a username to invite", "error");
      return;
    }

    const res = await shareDocument(auth.token, sharingDocId, name);
    if (res.ok) {
      setCollaboratorName("");
      await loadData();
      pushToast(`Invited ${name}`);
    } else {
      pushToast(res.message || "Invite failed", "error");
    }
  }

  useEffect(() => {
    if (auth?.token) loadData();
  }, [auth]);

  return (
    <>

      {/* ✅ Toast Area */}
      <div className="toastWrap">
        {toasts.map((t) => (
          <Toast key={t.id} message={t.message} type={t.type} onClose={() => removeToast(t.id)} />
        ))}
      </div>

      {/* ✅ Confirm Modal */}
      <ConfirmDialog
        open={confirm.open}
        title={confirm.title}
        message={confirm.message}
        danger={confirm.danger}
        confirmText={confirm.danger ? "Yes, remove" : "Confirm"}
        cancelText="Cancel"
        onCancel={closeConfirm}
        onConfirm={confirm.onConfirm}
      />

      <div className="saaSLayout">
        <aside className="sidebar">
          <div
            className="sidebarBrand"
            style={{ marginBottom: "20px", display: "flex", alignItems: "center", gap: "10px" }}
          >
            <div className="badge" style={{ width: "16px", height: "16px" }} />
            <div style={{ fontWeight: 900 }}>Collab Docs</div>
          </div>

          <div className="sidebarNav">
            <button className={`navItem ${view === "docs" ? "active" : ""}`} onClick={() => setView("docs")}>
              <Icon name="doc" /> My Documents
            </button>
            <button className={`navItem ${view === "profile" ? "active" : ""}`} onClick={() => setView("profile")}>
              <Icon name="user" /> Profile Settings
            </button>
            <button className="navItem" onClick={loadData}>
              <Icon name="refresh" /> Refresh
            </button>
          </div>

          <div className="divider" style={{ margin: "20px 0" }} />
          <div className="small">Signed in as</div>
          <div style={{ fontWeight: 800 }}>{auth.username}</div>
        </aside>

        <main className="mainArea">
          <div className="card" style={{ padding: "15px 20px", marginBottom: "20px" }}>
            <div style={{ fontSize: "22px", fontWeight: 900 }}>{view === "docs" ? "Documents" : "Account Settings"}</div>
            <div className="small">{today}</div>
            {error && <div className="alert" style={{ marginTop: 10 }}>{error}</div>}
          </div>

          <div className="contentGrid">
            {view === "docs" ? (
              <>
                <section className="card">
                  <div className="sectionTitle" style={{ fontWeight: 800, marginBottom: "15px" }}>
                    Create New Document
                  </div>

                  <form onSubmit={handleCreateDoc} style={{ display: "flex", gap: "10px" }}>
                    <input
                      className="input"
                      placeholder="Document title..."
                      value={newTitle}
                      onChange={(e) => setNewTitle(e.target.value)}
                      required
                    />
                    <button className="btn btnPrimary" type="submit" style={{ display: "flex", alignItems: "center", gap: "5px" }}>
                      <Icon name="plus" size={16} /> Create
                    </button>
                  </form>

                  <div className="divider" style={{ margin: "20px 0" }} />

                  <div className="timeline">
                    {loading ? (
                      <p>Loading documents...</p>
                    ) : (
                      docs.map((doc) => (
                        <div
                          key={doc.id}
                          className="step"
                          style={{
                            justifyContent: "space-between",
                            padding: "10px",
                            background: "rgba(124,92,255,0.05)",
                            borderRadius: "12px",
                          }}
                        >
                          <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
                            <div className="dot" />
                            <div>
                              <div style={{ fontWeight: 700 }}>{doc.title}</div>
                              <div className="small">Owner: {doc.ownerUsername}</div>
                            </div>
                          </div>

                          <div className="row">
                            {doc.ownerUsername === auth.username && (
                              <>
                                <button className="btn" onClick={() => setSharingDocId(doc.id)} title="Share">
                                  <Icon name="share" size={16} />
                                </button>

                                <button className="btn btnDanger" onClick={() => requestDeleteDoc(doc.id)}>
                                  Delete
                                </button>
                              </>
                            )}

                            <button className="btn btnPrimary" onClick={() => navigate(`/edit/${doc.id}`)}>
                              Open
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </section>

                <section className="card">
                  <div className="sectionTitle" style={{ fontWeight: 800, marginBottom: "15px" }}>
                    Collaboration
                  </div>

                  {sharingDocId ? (
                    <div>
                      <p className="small">
                        Managing: <b>{docs.find((d) => d.id === sharingDocId)?.title}</b>
                      </p>

                      <div style={{ margin: "15px 0", maxHeight: "150px", overflowY: "auto" }}>
                        {docs
                          .find((d) => d.id === sharingDocId)
                          ?.collaborators?.map((user) => (
                            <div
                              key={user}
                              style={{
                                display: "flex",
                                justifyContent: "space-between",
                                alignItems: "center",
                                padding: "8px 0",
                                borderBottom: "1px solid rgba(0,0,0,0.06)",
                              }}
                            >
                              <span style={{ fontSize: "14px" }}>{user}</span>

                              <button
                                className="btn btnDanger"
                                style={{ padding: "6px 10px", borderRadius: 12 }}
                                onClick={() => requestRevoke(sharingDocId, user)}
                              >
                                Revoke
                              </button>
                            </div>
                          ))}
                      </div>

                      <div className="divider" style={{ margin: "15px 0" }} />

                      <input
                        className="input"
                        placeholder="Enter username to invite..."
                        value={collaboratorName}
                        onChange={(e) => setCollaboratorName(e.target.value)}
                      />

                      <div className="row" style={{ marginTop: "15px" }}>
                        <button className="btn btnPrimary" type="button" onClick={handleInvite}>
                          Invite
                        </button>
                        <button className="btn" type="button" onClick={() => setSharingDocId(null)}>
                          Close
                        </button>
                      </div>
                    </div>
                  ) : (
                    <p className="small">Select a document's share icon in the list to manage permissions.</p>
                  )}
                </section>
              </>
            ) : (
              <>
                <section className="card">
                  <div className="sectionTitle" style={{ fontWeight: 800 }}>
                    Security
                  </div>

                  <form onSubmit={handlePasswordChange}>
                    <label className="small">Current Password</label>
                    <div className="inputWrap">
                      <input
                        className="input"
                        type={showCurrent ? "text" : "password"}
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        required
                      />
                      <button
                        type="button"
                        className="eyeBtn"
                        onClick={() => setShowCurrent((v) => !v)}
                        title={showCurrent ? "Hide" : "Show"}
                      >
                        {showCurrent ? "🙈" : "👁️"}
                      </button>
                    </div>

                    <label className="small" style={{ marginTop: 10, display: "block" }}>
                      New Password
                    </label>
                    <div className="inputWrap">
                      <input
                        className="input"
                        type={showNew ? "text" : "password"}
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        required
                      />
                      <button
                        type="button"
                        className="eyeBtn"
                        onClick={() => setShowNew((v) => !v)}
                        title={showNew ? "Hide" : "Show"}
                      >
                        {showNew ? "🙈" : "👁️"}
                      </button>
                    </div>

                    <button className="btn btnPrimary" style={{ marginTop: "15px" }}>
                      Update Password
                    </button>

                    {profileMsg && (
                      <div className={profileMsg.startsWith("Error") ? "alert" : "toast"} style={{ marginTop: 12 }}>
                        {profileMsg}
                      </div>
                    )}
                  </form>
                </section>

                <section className="card">
                  <div className="sectionTitle" style={{ fontWeight: 800 }}>
                    User Profile
                  </div>
                  <div className="small">
                    Logged in as: <b>{auth.username}</b>
                  </div>
                  <div className="small">Email: {auth.email || "No email linked"}</div>

                  <div className="divider" style={{ margin: "16px 0" }} />

                  <div className="small" style={{ marginBottom: 8, fontWeight: 800 }}>
                    Danger zone
                  </div>

                  <button className="btn btnDanger" onClick={requestDeleteAccount}>
                    Delete Account
                  </button>

                  <div className="small" style={{ marginTop: 8 }}>
                    This deletes your user and all documents you own.
                  </div>
                </section>

              </>
            )}
          </div>
        </main>
      </div>
    </>
  );
}

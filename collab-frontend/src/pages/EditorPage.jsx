import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { API_BASE, getDocVersionHistory } from "../Api";
import SockJS from "sockjs-client";
import Stomp from "stompjs";
import Toast from "../Toast";

export default function EditorPage({ auth }) {
  const { id } = useParams();
  const nav = useNavigate();

  const [doc, setDoc] = useState({ title: "Loading...", content: "" });
  const [status, setStatus] = useState("Connecting...");
  const [activeUsers, setActiveUsers] = useState([]);

  const [history, setHistory] = useState([]);
  const [showHistory, setShowHistory] = useState(false);

  // ✅ Toasts
  const [toasts, setToasts] = useState([]);

  const stompClient = useRef(null);
  const saveTimer = useRef(null);

  function pushToast(message, type = "success") {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, message, type }]);
  }

  function removeToast(id) {
    setToasts((t) => t.filter((x) => x.id !== id));
  }

  async function loadHistory() {
    const res = await getDocVersionHistory(auth.token, id);
    if (res.ok) {
      setHistory(res.data);
    } else {
      pushToast(res.message || "Failed to load history", "error");
    }
  }

  function handleRestore(version) {
    if (window.confirm(`Restore version from ${new Date(version.createdAt).toLocaleString()}?`)) {
      setDoc((prev) => ({ ...prev, content: version.content }));

      if (stompClient.current && stompClient.current.connected) {
        stompClient.current.send(
          `/app/edit/${id}`,
          {},
          JSON.stringify({
            sender: auth.username,
            content: version.content,
            type: "EDIT",
          })
        );
      }

      setShowHistory(false);
      pushToast("Version restored");
    }
  }

  useEffect(() => {
    fetch(`${API_BASE}/docs/${id}`, {
      headers: { Authorization: `Bearer ${auth.token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error("Document not found");
        return res.json();
      })
      .then((data) => setDoc(data))
      .catch(() => nav("/dashboard"));

    loadHistory();

    const socket = new SockJS(`${API_BASE}/ws-docs?token=${auth.token}`);
    const client = Stomp.over(socket);
    stompClient.current = client;
    client.debug = null;

    client.connect(
      {},
      () => {
        setStatus("Live");

        client.subscribe(`/topic/doc/${id}`, (message) => {
          const body = JSON.parse(message.body);

          if (body.type === "EDIT") {
            if (body.sender !== auth.username) {
              setDoc((prev) => ({ ...prev, content: body.content }));
            }
          } else if (body.type === "JOIN" || body.type === "LEAVE") {
            const usersFromServer = body.content.split(",").filter((u) => u !== "");
            setActiveUsers(usersFromServer);
          }
        });

        client.send(
          `/app/edit/${id}`,
          {},
          JSON.stringify({
            sender: auth.username,
            type: "JOIN",
            content: "",
          })
        );
      },
      () => {
        setStatus("Offline Mode");
        pushToast("Realtime connection lost (Offline Mode)", "error");
      }
    );

    return () => {
      if (saveTimer.current) clearTimeout(saveTimer.current);

      if (stompClient.current && stompClient.current.connected) {
        stompClient.current.send(
          `/app/edit/${id}`,
          {},
          JSON.stringify({
            sender: auth.username,
            type: "LEAVE",
            content: "",
          })
        );
        stompClient.current.disconnect();
      }
    };
  }, [id, auth, nav]);

  const handleTextChange = (e) => {
    const newContent = e.target.value;
    setDoc((prev) => ({ ...prev, content: newContent }));

    if (saveTimer.current) clearTimeout(saveTimer.current);
    saveTimer.current = setTimeout(() => {
      if (stompClient.current && stompClient.current.connected) {
        stompClient.current.send(
          `/app/edit/${id}`,
          {},
          JSON.stringify({
            sender: auth.username,
            content: newContent,
            type: "EDIT",
          })
        );
      }
    }, 500);
  };

  return (
    <div className="editorWrap">
      {/* ✅ Toast Area */}
      <div className="toastWrap">
        {toasts.map((t) => (
          <Toast
            key={t.id}
            message={t.message}
            type={t.type}
            onClose={() => removeToast(t.id)}
          />
        ))}
      </div>

      <div className="card editorCard">
        {/* TOP BAR */}
        <div className="editorTop">
          <div>
            <h1 className="editorTitle">{doc.title}</h1>

            <div className="editorMeta">
              <span className="pill">
                <span
                  className="badge"
                  style={{
                    width: 10,
                    height: 10,
                    borderRadius: 999,
                    boxShadow: "none",
                    background:
                      status === "Live"
                        ? "linear-gradient(135deg, #22c55e, #38bdf8)"
                        : "linear-gradient(135deg, #ff4d6d, #fb7185)",
                  }}
                />
                {status}
              </span>

              <span className="pill">Owner: {doc.ownerUsername}</span>

              {activeUsers.length > 0 && (
                <span className="pill">Editing now: {activeUsers.join(", ")}</span>
              )}
            </div>
          </div>

          <div className="row">
            <button className="btn" onClick={loadHistory}>
              Refresh History
            </button>

            <button
              className={`btn ${showHistory ? "btnPrimary" : ""}`}
              onClick={() => {
                const next = !showHistory;
                setShowHistory(next);
                if (next) loadHistory();
              }}
            >
              History
            </button>

            <button className="btn btnDanger" onClick={() => nav("/dashboard")}>
              Exit
            </button>
          </div>
        </div>

        {/* MAIN GRID */}
        <div className={`editorMainGrid ${showHistory ? "" : "noHistory"}`}>
          <textarea
            className="editorTextarea"
            value={doc.content || ""}
            onChange={handleTextChange}
            placeholder="Start typing your collaborative masterpiece..."
          />

          {showHistory && (
            <div className="editorSide">
              <div className="sideTitle">Version History</div>

              {history.length === 0 ? (
                <p className="small">No snapshots saved yet. Type edits to create versions automatically.</p>
              ) : (
                <div className="historyList">
                  {history.map((v) => (
                    <div className="historyItem" key={v.id}>
                      <div className="small" style={{ fontWeight: 950 }}>
                        {new Date(v.createdAt).toLocaleString()}
                      </div>
                      <div className="small">By: {v.authorUsername}</div>

                      <button
                        className="btn btnPrimary historyBtn"
                        onClick={() => handleRestore(v)}
                      >
                        Restore
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

"use client";

import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { motion, AnimatePresence } from "motion/react";
import {
  Plus,
  PaperPlaneTilt,
  Microphone,
} from "@phosphor-icons/react";
import {
  CANNED_RESPONSES,
  CONNECTORS,
  FRICTION_TIMES,
  getConnectorFromInput,
  renderMentionSegments,
  type Connector,
} from "./mock-data";

type ChatMessage = {
  role: "user" | "assistant";
  content: string;
};

function formatClock(date: Date) {
  return date.toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

function formatDate(date: Date) {
  return date
    .toLocaleDateString("en-US", {
      weekday: "short",
      month: "short",
      day: "2-digit",
    })
    .toUpperCase();
}

function filterConnectors(tab: "integrations" | "app", query: string) {
  const normalized = query.replace(/^@/, "").toLowerCase();
  return CONNECTORS.filter((connector) => {
    if (connector.tab !== tab) return false;
    if (!normalized) return true;
    return (
      connector.id.includes(normalized) ||
      connector.name.toLowerCase().includes(normalized)
    );
  });
}

export function PhoneMock() {
  const [now, setNow] = useState(() => new Date());
  const [input, setInput] = useState("");
  const [focused, setFocused] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerTab, setDrawerTab] = useState<"integrations" | "app">("integrations");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [frictionOpen, setFrictionOpen] = useState(false);
  const [frictionTime, setFrictionTime] = useState<string>("10m");
  const [frictionReason, setFrictionReason] = useState("");
  const [pendingTrap, setPendingTrap] = useState<Connector | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 2800);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const activeConnector = useMemo(() => getConnectorFromInput(input), [input]);
  const showChat = messages.length > 0;

  const drawerQuery = useMemo(() => {
    const atIndex = input.lastIndexOf("@");
    if (atIndex === -1) return "";
    return input.slice(atIndex + 1);
  }, [input]);

  const visibleConnectors = filterConnectors(drawerTab, drawerQuery);

  function openDrawer(tab: "integrations" | "app" = "integrations") {
    setDrawerTab(tab);
    setDrawerOpen(true);
  }

  function handleInputChange(value: string) {
    setInput(value);
    if (value.includes("@")) {
      setDrawerOpen(true);
    }
    const connector = getConnectorFromInput(value);
    if (connector?.kind === "trap") {
      setPendingTrap(connector);
      setFrictionOpen(true);
    } else {
      setFrictionOpen(false);
      setPendingTrap(null);
    }
  }

  function selectConnector(connector: Connector) {
    const atIndex = input.lastIndexOf("@");
    const prefix = atIndex >= 0 ? input.slice(0, atIndex) : input;
    const next = `${prefix}@${connector.id} `;
    setInput(next);
    setDrawerOpen(false);
    if (connector.kind === "trap") {
      setPendingTrap(connector);
      setFrictionOpen(true);
    } else {
      setPendingTrap(null);
      setFrictionOpen(false);
    }
  }

  function handleSend() {
    const trimmed = input.trim();
    if (!trimmed) return;

    if (frictionOpen && pendingTrap) {
      if (!frictionReason.trim()) {
        setToast("Add a reason before launching.");
        return;
      }
    }

    const connector = getConnectorFromInput(trimmed);
    const responseKey = connector?.id ?? "default";
    const response =
      CANNED_RESPONSES[responseKey] ?? CANNED_RESPONSES.default;

    setMessages((current) => [
      ...current,
      { role: "user", content: trimmed },
      { role: "assistant", content: response },
    ]);
    setInput("");
    setDrawerOpen(false);
    setFrictionOpen(false);
    setPendingTrap(null);
    setFrictionReason("");
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      className="relative mx-auto w-full max-w-[390px]"
    >
      <div
        className="relative overflow-hidden rounded-[40px] border border-kai-border bg-kai-bg shadow-[0_0_50px_rgba(0,0,0,0.8)]"
        style={{ aspectRatio: "390 / 844" }}
      >
        <div className="pointer-events-none absolute inset-0 kai-device-glow opacity-80" />

        <div className="relative flex h-full flex-col">
          <div className="flex items-center justify-between px-6 pt-6 text-xs font-bold uppercase tracking-[0.08em] text-kai-muted">
            <span>KaiOS</span>
            <span>{formatClock(now)}</span>
          </div>

          <div className="relative flex flex-1 flex-col overflow-hidden px-4 pb-4">
            <AnimatePresence mode="wait">
              {!showChat ? (
                <motion.div
                  key="clock"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="flex flex-1 flex-col items-center justify-center"
                >
                  <Image
                    src="/brand/logomark-for-dark.svg"
                    alt=""
                    width={72}
                    height={72}
                    className="mb-8 drop-shadow-[0_0_15px_rgba(255,107,0,0.3)]"
                  />
                  <p className="text-[clamp(3rem,12vw,4.5rem)] font-bold leading-none tracking-[-0.03em] text-kai-fg drop-shadow-[0_0_30px_rgba(255,107,0,0.2)]">
                    {formatClock(now)}
                  </p>
                  <p className="mt-2 text-sm font-bold uppercase tracking-[0.08em] text-kai-muted">
                    {formatDate(now)}
                  </p>
                </motion.div>
              ) : (
                <motion.div
                  key="chat"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex flex-1 flex-col gap-3 overflow-y-auto py-4"
                >
                  {messages.map((message, index) =>
                    message.role === "user" ? (
                      <div key={`user-${index}`} className="flex justify-end">
                        <div className="max-w-[75%] rounded-xl rounded-br border border-kai-border bg-kai-surface px-4 py-3 text-sm font-bold text-kai-muted">
                          {renderMentionSegments(message.content)}
                        </div>
                      </div>
                    ) : (
                      <div key={`ai-${index}`} className="max-w-[92%] border-l-2 border-kai-accent py-1 pl-4 text-sm font-bold text-kai-fg">
                        {message.content}
                      </div>
                    ),
                  )}
                </motion.div>
              )}
            </AnimatePresence>

            <div className="relative mt-auto space-y-2">
              <AnimatePresence>
                {drawerOpen ? (
                  <motion.div
                    key="drawer"
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: 8 }}
                    transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                    className="rounded-2xl border border-kai-accent/40 bg-kai-surface/95 p-3 backdrop-blur-md"
                  >
                    <div className="mb-3 flex gap-2">
                      {(["integrations", "app"] as const).map((tab) => (
                        <button
                          key={tab}
                          type="button"
                          onClick={() => setDrawerTab(tab)}
                          className={`rounded-lg px-3 py-1.5 text-[11px] font-bold uppercase tracking-[0.08em] transition ${
                            drawerTab === tab
                              ? "bg-kai-accent text-black"
                              : "border border-kai-border text-kai-muted hover:text-kai-fg"
                          }`}
                        >
                          {tab}
                        </button>
                      ))}
                    </div>
                    <ul className="max-h-40 space-y-1 overflow-y-auto">
                      {visibleConnectors.map((connector) => (
                        <li key={connector.id}>
                          <button
                            type="button"
                            onClick={() => selectConnector(connector)}
                            className="flex w-full items-center gap-3 rounded-lg px-2 py-2 text-left transition hover:bg-kai-bg"
                          >
                            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-kai-accent/15 text-sm">
                              {connector.emoji}
                            </span>
                            <span className="flex-1 text-sm font-bold text-kai-fg">
                              {connector.name}
                            </span>
                            <span className="text-xs font-bold text-kai-muted">
                              @{connector.id}
                            </span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  </motion.div>
                ) : null}
              </AnimatePresence>

              <AnimatePresence>
                {frictionOpen && pendingTrap ? (
                  <motion.div
                    key="friction"
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="overflow-hidden rounded-2xl border border-kai-border bg-kai-bg/90 p-3"
                  >
                    <p className="text-xs font-bold leading-relaxed text-kai-muted">
                      Intentional friction for{" "}
                      <span className="kai-mention">@{pendingTrap.id}</span>.
                      Set a duration and reason.
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {FRICTION_TIMES.map((time) => (
                        <button
                          key={time}
                          type="button"
                          onClick={() => setFrictionTime(time)}
                          className={`rounded-lg border px-3 py-1.5 text-xs font-bold uppercase tracking-[0.08em] transition ${
                            frictionTime === time
                              ? "border-kai-accent bg-kai-accent/10 text-kai-accent"
                              : "border-kai-border text-kai-muted hover:text-kai-fg"
                          }`}
                        >
                          {time}
                        </button>
                      ))}
                    </div>
                    <input
                      type="text"
                      value={frictionReason}
                      onChange={(event) => setFrictionReason(event.target.value)}
                      placeholder="Reason (e.g. reply to DM)"
                      maxLength={80}
                      className="mt-3 w-full rounded-lg border border-kai-border bg-kai-surface px-3 py-2 text-xs font-bold text-kai-fg placeholder:text-kai-muted focus:border-kai-accent focus:outline-none"
                    />
                  </motion.div>
                ) : null}
              </AnimatePresence>

              <div
                className={`rounded-2xl border kai-glass p-3 transition ${
                  focused
                    ? "border-kai-accent kai-glow"
                    : "border-kai-border"
                }`}
              >
                <div className="flex items-end gap-2">
                  <button
                    type="button"
                    aria-label="Add connector"
                    onClick={() => openDrawer("integrations")}
                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-kai-border text-kai-muted transition hover:border-kai-accent hover:text-kai-accent"
                  >
                    <Plus size={18} weight="bold" />
                  </button>
                  <div className="min-h-10 flex-1 py-2">
                    <input
                      type="text"
                      value={input}
                      onChange={(event) => handleInputChange(event.target.value)}
                      onFocus={() => setFocused(true)}
                      onBlur={() => setFocused(false)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") {
                          event.preventDefault();
                          handleSend();
                        }
                      }}
                      placeholder="Type your command"
                      className="w-full bg-transparent text-sm font-bold text-kai-fg placeholder:text-kai-muted focus:outline-none"
                      aria-label="Command input"
                    />
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <button
                      type="button"
                      aria-label="Voice input"
                      className="flex h-10 w-10 items-center justify-center rounded-lg text-kai-muted transition hover:text-kai-accent"
                    >
                      <Microphone size={18} weight="bold" />
                    </button>
                    <button
                      type="button"
                      aria-label="Send command"
                      onClick={handleSend}
                      className="flex h-10 w-10 items-center justify-center rounded-lg bg-kai-accent text-black transition hover:bg-kai-accent-bright"
                    >
                      <PaperPlaneTilt size={18} weight="bold" />
                    </button>
                  </div>
                </div>
                {activeConnector ? (
                  <p className="mt-2 text-[11px] font-bold uppercase tracking-[0.08em] text-kai-muted">
                    Routing to @{activeConnector.id}
                  </p>
                ) : null}
              </div>
            </div>
          </div>
        </div>

        <AnimatePresence>
          {toast ? (
            <motion.div
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              className="absolute bottom-28 left-1/2 z-20 -translate-x-1/2 rounded-lg border border-kai-border bg-kai-surface px-4 py-2 text-xs font-bold text-kai-fg"
            >
              {toast}
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}

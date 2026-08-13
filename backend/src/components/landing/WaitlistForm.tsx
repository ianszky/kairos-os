"use client";

import { FormEvent, useState } from "react";
import { ArrowRight } from "@phosphor-icons/react";

type WaitlistFormProps = {
  source?: string;
  compact?: boolean;
};

type FormState = "idle" | "loading" | "success" | "duplicate" | "error";

export function WaitlistForm({ source = "landing", compact = false }: WaitlistFormProps) {
  const [email, setEmail] = useState("");
  const [state, setState] = useState<FormState>("idle");
  const [message, setMessage] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setState("loading");
    setMessage("");

    try {
      const response = await fetch("/api/waitlist", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, source }),
      });

      const data = (await response.json()) as { error?: string };

      if (response.status === 409) {
        setState("duplicate");
        setMessage("You're already on the list.");
        return;
      }

      if (!response.ok) {
        setState("error");
        setMessage(data.error ?? "Something went wrong.");
        return;
      }

      setState("success");
      setMessage("You're on the list. We'll reach out when KaiOS is ready.");
      setEmail("");
    } catch {
      setState("error");
      setMessage("Network error. Try again.");
    }
  }

  const disabled = state === "loading" || state === "success";

  return (
    <div className={compact ? "w-full max-w-md" : "w-full max-w-lg"}>
      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-3 sm:flex-row sm:items-stretch"
        aria-label="Join the KaiOS waitlist"
      >
        <label htmlFor={`waitlist-email-${source}`} className="sr-only">
          Email address
        </label>
        <input
          id={`waitlist-email-${source}`}
          type="email"
          name="email"
          autoComplete="email"
          required
          value={email}
          disabled={disabled}
          onChange={(event) => setEmail(event.target.value)}
          placeholder="you@email.com"
          className="min-h-12 flex-1 rounded-lg border border-kai-border bg-kai-surface px-4 text-sm font-bold text-kai-fg placeholder:text-kai-muted focus:border-kai-accent focus:outline-none focus:ring-2 focus:ring-kai-accent/30 disabled:opacity-60"
        />
        <button
          type="submit"
          disabled={disabled}
          className="inline-flex min-h-12 items-center justify-center gap-2 rounded-lg bg-kai-accent px-5 text-sm font-bold uppercase tracking-wide text-black transition hover:-translate-y-px hover:bg-kai-accent-bright focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-kai-accent disabled:translate-y-0 disabled:opacity-60"
        >
          {state === "loading" ? "Joining…" : "Join waitlist"}
          <ArrowRight size={16} weight="bold" aria-hidden />
        </button>
      </form>
      {message ? (
        <p
          role="status"
          aria-live="polite"
          className={`mt-3 text-sm font-bold ${
            state === "error" ? "text-red-400" : "text-kai-muted"
          }`}
        >
          {message}
        </p>
      ) : null}
    </div>
  );
}

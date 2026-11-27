from __future__ import annotations

import json
import httpx

from app.config import ServerSettings


class OllamaClient:
    """Lightweight async/sync HTTP client for optional private-server Ollama AI."""

    def __init__(self, settings: ServerSettings, timeout_seconds: float = 30.0) -> None:
        self.settings = settings
        self.timeout_seconds = timeout_seconds

    def generate(self, model: str, prompt: str) -> str | None:
        url = f"{self.settings.ollama_base_url.rstrip('/')}/api/generate"
        payload = {"model": model, "prompt": prompt, "stream": False}
        try:
            with httpx.Client(timeout=self.timeout_seconds) as client:
                resp = client.post(url, json=payload)
                resp.raise_for_status()
                data = resp.json()
                text = (data.get("response") or "").strip()
                return text or None
        except Exception:
            return None

    def explain_passage(self, book_title: str, passage_text: str) -> tuple[str, str]:
        prompt = (
            f"Explain the following passage from '{book_title}' clearly and concisely in 2-4 sentences:\n\n"
            f"{passage_text}"
        )
        reply = self.generate(self.settings.ollama_instruct_model, prompt)
        if reply:
            return reply, self.settings.ollama_instruct_model
        return (
            f"Context note for '{book_title}': {passage_text[:160]}",
            f"{self.settings.ollama_instruct_model} (fallback)",
        )

    def suggest_tags(self, book_title: str, passage_text: str) -> tuple[list[str], str]:
        prompt = (
            f"Return a JSON array of 3-5 short lowercase topic tags for this passage from '{book_title}'. "
            f"Only output the JSON array.\n\n{passage_text}"
        )
        reply = self.generate(self.settings.ollama_instruct_model, prompt)
        if reply:
            try:
                parsed = json.loads(reply)
                if isinstance(parsed, list):
                    tags = [str(t).strip().lower() for t in parsed if str(t).strip()][:5]
                    if tags:
                        return tags, self.settings.ollama_instruct_model
            except Exception:
                pass
        words = [w.strip(".,;:!?\"'()").lower() for w in passage_text.split() if len(w) > 5]
        return list(dict.fromkeys(words))[:4], f"{self.settings.ollama_instruct_model} (fallback)"

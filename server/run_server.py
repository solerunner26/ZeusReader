from __future__ import annotations

import os
import sys
from pathlib import Path

import uvicorn

SERVER_DIR = Path(__file__).resolve().parent
if str(SERVER_DIR) not in sys.path:
    sys.path.insert(0, str(SERVER_DIR))

if __name__ == "__main__":
    os.environ.setdefault("ZEUS_PAIRING_TOKEN", "zeus-dev-pairing-token")
    uvicorn.run(
        "app.main:app",
        host=os.environ.get("ZEUS_HOST", "0.0.0.0"),
        port=int(os.environ.get("ZEUS_PORT", "8765")),
        reload=False,
    )

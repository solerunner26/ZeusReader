# Multi-arch (linux/amd64 Windows 11 & linux/arm64 Oracle Always Free)
FROM python:3.12-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml ./
RUN pip install --no-cache-dir .

COPY app ./app
COPY spike ./spike
COPY run_server.py ./run_server.py

ENV ZEUS_SERVER_DATA_DIR=/data
EXPOSE 8765

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8765"]

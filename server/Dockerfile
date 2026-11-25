FROM python:3.12-slim

WORKDIR /srv/zeusreader

COPY pyproject.toml ./
RUN pip install --no-cache-dir fastapi uvicorn[standard] pydantic sqlalchemy python-multipart httpx lxml

COPY app ./app
COPY spike ./spike

ENV ZEUS_SERVER_DATA_DIR=/srv/zeusreader/data
EXPOSE 8787

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8787"]

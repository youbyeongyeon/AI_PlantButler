# server/app/config.py
from typing import List, Optional
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",            # server 디렉토리 기준
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # ---- App ----
    APP_ENV: str = "dev"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8000
    DEBUG: bool = True

    # ---- Keys ----
    OPENAI_API_KEY: Optional[str] = None

    # ---- Azure (선택) ----
    AZURE_OPENAI_ENDPOINT: Optional[str] = None
    AZURE_OPENAI_API_KEY: Optional[str] = None
    AZURE_STORAGE_CONNECTION_STRING: Optional[str] = None
    AZURE_STORAGE_CONTAINER: str = "plant-images"

    # ---- DB ----
    DB_URL: str = "sqlite:///./plantbutler.db"

    # ---- Auth ----
    JWT_SECRET: str = "change_me"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRE_MINUTES: int = 120

    # ---- CORS ----
    # .env에서 JSON 배열(["http://...","http://..."]) 또는 콤마 구분("http://...,http://...") 모두 허용
    CORS_ORIGINS: List[str] = ["http://localhost:3000", "http://127.0.0.1:3000"]

    @field_validator("CORS_ORIGINS", mode="before")
    @classmethod
    def parse_cors_origins(cls, v):
        if isinstance(v, list):
            return v
        if isinstance(v, str):
            s = v.strip()
            # JSON 배열 문자열이면 그대로 파싱 시도
            if s.startswith("[") and s.endswith("]"):
                import json
                try:
                    arr = json.loads(s)
                    if isinstance(arr, list):
                        return arr
                except Exception:
                    pass
            # 콤마 구분 문자열 처리
            return [x.strip() for x in s.split(",") if x.strip()]
        return v

settings = Settings()
# server/app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routes import plant
from app.config import settings  # pydantic-settings 기반 .env 로더 (env_file=".env")

app = FastAPI(
    title="AI Plant Butler Server",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS 설정 (.env의 CORS_ORIGINS 사용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(plant.router, prefix="/plant", tags=["Plant"])

@app.get("/")
def root():
    return {
        "message": "AI Plant Butler Server is running 🚀",
        "env": settings.APP_ENV,
        "debug": settings.DEBUG,
        "version": app.version,
    }

@app.get("/health")
def health_check():
    """
    가벼운 헬스체크:
    - 서버 프로세스 동작 여부
    - 필수 설정(예: OPENAI_API_KEY 존재 여부)만 빠르게 확인
    """
    checks = {
        "app": "ok",
        "openai_key_present": bool(settings.OPENAI_API_KEY),
    }
    status = "ok" if all(checks.values()) else "degraded"
    return {"status": status, "checks": checks}

@app.get("/env")
def env_info():
    """
    안전한 범위의 환경 정보만 노출(비밀값은 절대 노출 금지)
    운영 시 필요 없다면 이 엔드포인트는 삭제해도 됩니다.
    """
    return {
        "app_env": settings.APP_ENV,
        "host": settings.APP_HOST,
        "port": settings.APP_PORT,
        "debug": settings.DEBUG,
        "cors_origins": settings.CORS_ORIGINS,
        "db_url_scheme": settings.DB_URL.split("://", 1)[0] if settings.DB_URL else None,
        "azure_openai_endpoint_set": bool(settings.AZURE_OPENAI_ENDPOINT),
    }

# uvicorn으로 직접 실행할 때 편의용
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.APP_HOST,
        port=settings.APP_PORT,
        reload=settings.DEBUG,
    )

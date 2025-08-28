from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routes import plant
from app.routes import chat
from app.config import settings

app = FastAPI(
    title="AI Plant Butler Server",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(plant.router, prefix="/plant", tags=["Plant"])
app.include_router(chat.router,  prefix="/chat",  tags=["Chat"])

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
    checks = {
        "app": "ok",
        "openai_key_present": bool(settings.OPENAI_API_KEY),
    }
    status = "ok" if all(checks.values()) else "degraded"
    return {"status": status, "checks": checks}

@app.get("/env")
def env_info():
    return {
        "app_env": settings.APP_ENV,
        "host": settings.APP_HOST,
        "port": settings.APP_PORT,
        "debug": settings.DEBUG,
        "cors_origins": settings.CORS_ORIGINS,
        "db_url_scheme": settings.DB_URL.split("://", 1)[0] if settings.DB_URL else None,
        "azure_openai_endpoint_set": bool(settings.AZURE_OPENAI_ENDPOINT),
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.APP_HOST,
        port=settings.APP_PORT,
        reload=settings.DEBUG,
    )

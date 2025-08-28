# server/app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routes import plant
from app.routes import chat
from app.config import settings  # pydantic-settings 기반 .env 로더 (env_file=".env")

# [추가] pydantic 모델
from pydantic import BaseModel

class ChatRequest(BaseModel):
    text: str

class ChatResponse(BaseModel):
    reply: str

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

# [추가] 텍스트 채팅 엔드포인트
@app.post("/chat/text", response_model=ChatResponse, tags=["Chat"])
async def chat_text(req: ChatRequest) -> ChatResponse:
    """
    클라이언트의 텍스트 메시지를 받아 답변을 반환.
    - 1차: 간단한 에코/규칙 기반
    - TODO: care_guides.json/모델/OpenAI 연동 로직 연결
    """
    user = req.text.strip()

    # 아주 간단한 규칙 기반 예시 (원하는 로직으로 교체 가능)
    if any(k in user for k in ["물", "관수", "물주", "수분"]):
        reply = "관수는 겉흙 2~3cm가 마르면 충분히 주세요. 배수구로 물이 흘러나올 정도로 주고, 남은 물은 버려주세요."
    elif any(k in user for k in ["빛", "광", "일조", "햇빛"]):
        reply = "대부분의 실내식물은 밝은 간접광을 선호해요. 직사광이 강하면 잎이 탈 수 있어요."
    else:
        reply = f"질문 확인! 현재는 테스트 응답이에요: “{user}”"

    return ChatResponse(reply=reply)

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

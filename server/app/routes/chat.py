# server/app/routes/chat.py
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.config import settings

# (선택) OpenAI가 설치/설정되어 있으면 사용, 없으면 규칙 기반으로 응답
try:
    from openai import OpenAI
    _openai_available = bool(settings.OPENAI_API_KEY)
except Exception:
    OpenAI = None  # type: ignore
    _openai_available = False

router = APIRouter()

class ChatRequest(BaseModel):
    text: str

class ChatResponse(BaseModel):
    reply: str

@router.post("/text", response_model=ChatResponse)
async def chat_text(body: ChatRequest):
    """
    간단한 텍스트 챗 엔드포인트.
    - OpenAI API 키가 있으면 LLM으로 답변
    - 없으면 규칙 기반/기본 멘트로 응답
    """
    user_text = (body.text or "").strip()
    if not user_text:
        raise HTTPException(status_code=400, detail="text is required")

    # 1) OpenAI 사용 가능한 경우
    if _openai_available and OpenAI is not None:
        try:
            client = OpenAI(api_key=settings.OPENAI_API_KEY)
            # gpt-4o-mini 등 경량 모델 추천. 모델명은 환경/계정에 맞게 조정 가능.
            completion = client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": "You are a helpful plant-care assistant."},
                    {"role": "user", "content": user_text},
                ],
                temperature=0.4,
            )
            reply = completion.choices[0].message.content or "죄송해요, 빈 응답이 왔어요."
            return ChatResponse(reply=reply)
        except Exception as e:
            # LLM 실패 시에도 서버는 200으로 기본 응답 반환(클라 UX를 위해)
            return ChatResponse(
                reply=f"일시적으로 AI 응답을 가져오지 못했어요. (fallback)\n"
                      f"메시지: {user_text}"
            )

    # 2) OpenAI 미사용(키 없음) 시: 간단한 규칙 기반
    # 식물 관련 키워드가 있으면 관련 멘트, 없으면 일반 응답
    lower = user_text.lower()
    if any(k in lower for k in ["물", "water", "급수", "watering"]):
        return ChatResponse(reply="급수 팁: 흙 상단 2~3cm가 마르면 충분히 물을 주세요. 배수구가 막히지 않았는지도 확인하세요.")
    if any(k in lower for k in ["빛", "sun", "light", "광량"]):
        return ChatResponse(reply="광량 팁: 대부분의 실내 식물은 밝은 간접광을 선호해요. 직사광선은 잎을 태울 수 있어요.")
    if any(k in lower for k in ["비료", "영양", "fertilizer"]):
        return ChatResponse(reply="비료 팁: 생장기(봄~여름)엔 4~6주 간격의 희석 비료가 좋아요. 겨울엔 빈도를 줄이세요.")

    # 기본 응답
    return ChatResponse(
        reply="식물 관리와 관련된 질문을 하시면 더 구체적으로 도와드릴게요 🌿\n"
              "예) '몬스테라 물 주기', '빛이 부족할 때 증상', '분갈이 시기'"
    )

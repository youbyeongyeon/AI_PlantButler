# server/app/routes/chat.py
from fastapi import APIRouter, HTTPException
from fastapi.concurrency import run_in_threadpool
from pydantic import BaseModel
from typing import Optional, Deque, Dict, List
from collections import deque
from app.config import settings

# OpenAI 사용 여부/클라이언트 준비
try:
    from openai import OpenAI
    _openai_available = bool(settings.OPENAI_API_KEY)
except Exception:
    OpenAI = None  # type: ignore
    _openai_available = False

router = APIRouter()

# ---- (선택) 간단한 인메모리 히스토리: room_id 별 최근 N턴 보관 ----
_MAX_TURNS = 8  # 최근 8개 메시지만 유지
_histories: Dict[str, Deque[Dict[str, str]]] = {}

class ChatRequest(BaseModel):
    text: str
    room_id: Optional[str] = None  # 클라가 방ID 넘기면 대화 맥락 유지

class ChatResponse(BaseModel):
    reply: str

_SYSTEM_PROMPT = (
    "당신은 식물 관리 도우미입니다. 사용자가 식물 관련 질문을 하면 "
    "핵심만 간결하게 한국어로 답하고, 필요시 간단한 체크리스트/주의사항을 2~3개 제시하세요. "
    "불확실하면 모르는 부분을 분명히 밝히고, 안전이슈(독성식물/살충제 등)는 주의 문구를 포함하세요."
)

def _get_openai_client() -> Optional[OpenAI]:
    if not _openai_available or OpenAI is None:
        return None
    # OPENAI_BASE_URL을 사용하고 싶으면 .env에 설정
    if settings.OPENAI_BASE_URL:
        return OpenAI(api_key=settings.OPENAI_API_KEY, base_url=settings.OPENAI_BASE_URL)
    return OpenAI(api_key=settings.OPENAI_API_KEY)

def _push_history(room_id: str, role: str, content: str):
    dq = _histories.setdefault(room_id, deque(maxlen=_MAX_TURNS))
    dq.append({"role": role, "content": content})

def _build_messages(user_text: str, room_id: Optional[str]) -> List[Dict[str, str]]:
    messages: List[Dict[str, str]] = [{"role": "system", "content": _SYSTEM_PROMPT}]
    if room_id and room_id in _histories:
        messages.extend(list(_histories[room_id]))
    messages.append({"role": "user", "content": user_text})
    return messages

async def _llm_reply(user_text: str, room_id: Optional[str]) -> str:
    client = _get_openai_client()
    if not client:
        raise RuntimeError("OpenAI client not available")

    model = settings.OPENAI_MODEL or "gpt-4o-mini"
    messages = _build_messages(user_text, room_id)

    # OpenAI SDK v1은 동기이므로 스레드풀에서 실행
    def _call():
        return client.chat.completions.create(
            model=model,
            messages=messages,
            temperature=0.4,
            max_tokens=350,
        )

    completion = await run_in_threadpool(_call)
    return (completion.choices[0].message.content or "").strip()

@router.post("/text", response_model=ChatResponse)
async def chat_text(body: ChatRequest):
    """
    텍스트 챗 엔드포인트:
    - OPENAI_API_KEY가 있으면 OpenAI로 모든 채팅 처리
    - 키가 없거나 실패 시, 간단한 규칙 기반 폴백
    - room_id를 넘기면 최근 몇 턴의 맥락을 유지
    """
    user_text = (body.text or "").strip()
    if not user_text:
        raise HTTPException(status_code=400, detail="text is required")

    # 히스토리 기록(유저)
    if body.room_id:
        _push_history(body.room_id, "user", user_text)

    # 1) OpenAI 사용 경로
    if _openai_available and OpenAI is not None:
        try:
            reply = await _llm_reply(user_text, body.room_id)
            # 히스토리 기록(assistant)
            if body.room_id:
                _push_history(body.room_id, "assistant", reply)
            return ChatResponse(reply=reply)
        except Exception:
            # LLM 실패 시 폴백
            pass

    # 2) 폴백 규칙(키 없음/실패)
    lower = user_text.lower()
    if any(k in lower for k in ["물", "water", "급수", "watering"]):
        return ChatResponse(reply="급수 팁: 흙 상단 2~3cm가 마르면 충분히 물을 주세요. 배수구가 막히지 않았는지도 확인하세요.")
    if any(k in lower for k in ["빛", "sun", "light", "광량"]):
        return ChatResponse(reply="광량 팁: 대부분의 실내 식물은 밝은 간접광을 선호해요. 직사광선은 잎을 태울 수 있어요.")
    if any(k in lower for k in ["비료", "영양", "fertilizer"]):
        return ChatResponse(reply="비료 팁: 생장기(봄~여름)엔 4~6주 간격의 희석 비료가 좋아요. 겨울엔 빈도를 줄이세요.")

    return ChatResponse(
        reply="식물 관리와 관련된 질문을 하시면 더 구체적으로 도와드릴게요 🌿\n"
              "예) '몬스테라 물 주기', '빛이 부족할 때 증상', '분갈이 시기'"
    )

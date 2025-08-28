# server/app/routes/plant.py
from fastapi import APIRouter, UploadFile, File, HTTPException
from fastapi.concurrency import run_in_threadpool
from pydantic import BaseModel
from typing import Optional, Dict, Any
import numpy as np
from PIL import Image
import io
import json
import os

# Keras
from tensorflow.keras.models import load_model

# OpenAI (공식 SDK v1)
from openai import OpenAI

# 설정 (env는 app/config/settings.py에서 불러온다고 가정)
from app.config import settings

router = APIRouter()

# ---------- 전역 자원: 앱 시작 시 1회 로드 ----------
_MODEL = None
_GUIDES: Dict[str, Any] = {}
_OPENAI: Optional[OpenAI] = None

MODEL_PATH = settings.MODEL_PATH or "app/models/plant_disease_model.keras"
GUIDE_PATH = settings.CARE_GUIDES_PATH or "app/data/care_guides.json"

def _load_model_and_guides_once():
    global _MODEL, _GUIDES, _OPENAI
    if _MODEL is None:
        if not os.path.exists(MODEL_PATH):
            raise RuntimeError(f"Model file not found: {MODEL_PATH}")
        _MODEL = load_model(MODEL_PATH)

    if not _GUIDES:
        if not os.path.exists(GUIDE_PATH):
            raise RuntimeError(f"care_guides.json not found: {GUIDE_PATH}")
        with open(GUIDE_PATH, "r", encoding="utf-8") as f:
            _GUIDES = json.load(f)

    if settings.OPENAI_API_KEY and _OPENAI is None:
        _OPENAI = OpenAI(api_key=settings.OPENAI_API_KEY)

# FastAPI가 import할 때 1회 로드
try:
    _load_model_and_guides_once()
except Exception as e:
    # 초기 로드 실패 시에도 앱은 뜨되, 요청 시 에러를 주도록 함
    print(f"[plant] warmup failed: {e}")

# ---------- 스키마 ----------
class AnalyzeResult(BaseModel):
    label: str                  # 예: "healthy", "powdery_mildew" ...
    confidence: float           # 0.0 ~ 1.0
    nameKo: Optional[str] = None
    description: Optional[str] = None
    solution: Optional[str] = None
    extraTips: Optional[str] = None   # OpenAI 보강 설명(있을 때만)

class AnalyzeResponse(BaseModel):
    ok: bool
    result: Optional[AnalyzeResult] = None
    error: Optional[str] = None

# ---------- 유틸 ----------
def _preprocess_image(file_bytes: bytes, target_size=(224, 224)) -> np.ndarray:
    img = Image.open(io.BytesIO(file_bytes)).convert("RGB").resize(target_size)
    arr = np.asarray(img, dtype=np.float32) / 255.0
    arr = np.expand_dims(arr, axis=0)
    return arr

def _predict_label(arr: np.ndarray) -> (str, float):
    """
    Keras 모델의 최종 softmax 출력 가정.
    settings.CLASS_NAMES 에 label 배열을 둔다고 가정.
    """
    if _MODEL is None:
        raise RuntimeError("Model not loaded")

    preds = _MODEL.predict(arr)   # shape: (1, num_classes)
    probs = preds[0]
    idx = int(np.argmax(probs))
    label = settings.CLASS_NAMES[idx] if settings.CLASS_NAMES and idx < len(settings.CLASS_NAMES) else str(idx)
    conf = float(probs[idx])
    return label, conf

def _lookup_guide(label: str) -> Optional[Dict[str, Any]]:
    """
    care_guides.json의 기본 구조 예시:
    {
      "healthy": { "nameKo": "정상", "description": "...", "solution": "..." },
      "powdery_mildew": {...},
      ...
    }
    """
    if not _GUIDES:
        return None
    key = label.lower()
    if key in _GUIDES:
        return _GUIDES[key]
    # 대소문자/스페이스/하이픈 보정
    key2 = key.replace(" ", "_").replace("-", "_")
    return _GUIDES.get(key2)

async def _ask_openai_boost(label: str, base_ko: Optional[str], is_healthy: bool) -> Optional[str]:
    """
    OpenAI로 간단 보강 설명 요청(옵션).
    - 건강(healthy) 시: 관리 팁 위주
    - 질병 시: 원인/응급조치/예방 팁 (care_guides.json 내용을 반복하지 않게)
    """
    if _OPENAI is None:
        return None

    try:
        if is_healthy:
            user = f"식물 잎 상태는 정상({label})으로 판정. 초보자용 한국어 관리 팁 3가지만 간단히."
        else:
            user = f"식물 질병 '{base_ko or label}'에 대해, 기존 요약을 반복하지 말고 실전 팁 2가지만 한국어로 간단히."

        resp = await run_in_threadpool(
            lambda: _OPENAI.chat.completions.create(
                model=settings.OPENAI_MODEL or "gpt-4o-mini",
                messages=[{"role": "user", "content": user}],
                temperature=0.4,
                max_tokens=180,
            )
        )
        return (resp.choices[0].message.content or "").strip()
    except Exception as e:
        print(f"[openai] boost failed: {e}")
        return None

# ---------- 엔드포인트 ----------
@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_image(image: UploadFile = File(...)):
    """
    - 이미지 업로드(Multipart)
    - Keras로 라벨/확률
    - care_guides.json에서 한글 설명/해결책 매칭
    - 상황에 따라 OpenAI로 보강설명 추가(extraTips)
    """
    try:
        _load_model_and_guides_once()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Server not ready: {e}")

    try:
        file_bytes = await image.read()
        arr = await run_in_threadpool(lambda: _preprocess_image(file_bytes))
        label, conf = await run_in_threadpool(lambda: _predict_label(arr))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid image or model error: {e}")

    guide = _lookup_guide(label)
    is_healthy = (label.lower() == "healthy") or (guide and str(guide.get("label", "")).lower() == "healthy")

    # OpenAI 보강(선택)
    extra = await _ask_openai_boost(label, guide.get("nameKo") if guide else None, is_healthy=is_healthy)

    result = AnalyzeResult(
        label=label,
        confidence=round(conf, 4),
        nameKo=guide.get("nameKo") if guide else ("정상" if is_healthy else None),
        description=guide.get("description") if guide else ( "겉보기 이상 없음" if is_healthy else None),
        solution=guide.get("solution") if guide else ( "물·광량·통풍을 주기적으로 점검하세요." if is_healthy else None),
        extraTips=extra
    )

    return AnalyzeResponse(ok=True, result=result)

# server/app/routes/plant.py
from fastapi import APIRouter, UploadFile, File, HTTPException
from fastapi.concurrency import run_in_threadpool
from pydantic import BaseModel
from typing import Optional, Dict, Any, List
import numpy as np
from PIL import Image
import io
import json
import os

# Keras
from tensorflow.keras.models import load_model

# OpenAI (공식 SDK v1)
from openai import OpenAI

# 설정
from app.config import settings

router = APIRouter()

# ---------- 전역 자원 ----------
_MODEL = None
_GUIDES: Dict[str, Any] = {}
_OPENAI: Optional[OpenAI] = None

# settings에 없으면 안전한 기본값 사용
MODEL_PATH = getattr(settings, "MODEL_PATH", "app/models/plant_disease_model.keras")
GUIDE_PATH = getattr(settings, "CARE_GUIDES_PATH", "app/data/care_guides.json")
CLASS_NAMES: List[str] = getattr(settings, "CLASS_NAMES", [])  # 예: ["healthy","powdery_mildew",...]
OPENAI_MODEL = getattr(settings, "OPENAI_MODEL", "gpt-4o-mini")
OPENAI_API_KEY = getattr(settings, "OPENAI_API_KEY", None)

def _load_model_and_guides_once():
    """필요 리소스를 1회 로드 (없으면 가능한 한 gracefully 처리)"""
    global _MODEL, _GUIDES, _OPENAI

    # 모델
    if _MODEL is None:
        if os.path.exists(MODEL_PATH):
            print(f"[plant] loading model: {MODEL_PATH}")
            _MODEL = load_model(MODEL_PATH)
        else:
            # 모델이 꼭 필요하므로 없으면 명시적으로 에러를 던지게 둠
            raise RuntimeError(f"Model file not found: {MODEL_PATH}")

    # care_guides.json
    if not _GUIDES:
        if os.path.exists(GUIDE_PATH):
            print(f"[plant] loading guides: {GUIDE_PATH}")
            with open(GUIDE_PATH, "r", encoding="utf-8") as f:
                _GUIDES = json.load(f)
        else:
            print(f"[plant] WARNING: guides file not found: {GUIDE_PATH} (extra info는 제한될 수 있음)")
            _GUIDES = {}

    # OpenAI
    if OPENAI_API_KEY and _OPENAI is None:
        print("[plant] initializing OpenAI client")
        _OPENAI = OpenAI(api_key=OPENAI_API_KEY)

# 앱 import 시 워밍업(없어도 요청시 다시 시도)
try:
    _load_model_and_guides_once()
except Exception as e:
    print(f"[plant] warmup failed: {e}")

# ---------- 스키마 ----------
class AnalyzeResult(BaseModel):
    label: str
    confidence: float
    nameKo: Optional[str] = None
    description: Optional[str] = None
    solution: Optional[str] = None
    extraTips: Optional[str] = None   # OpenAI 보강 설명

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
    CLASS_NAMES가 채워져 있으면 그 라벨을, 없으면 인덱스를 문자열로 반환.
    """
    if _MODEL is None:
        raise RuntimeError("Model not loaded")

    preds = _MODEL.predict(arr)   # shape: (1, num_classes)
    probs = preds[0]
    idx = int(np.argmax(probs))
    label = CLASS_NAMES[idx] if CLASS_NAMES and idx < len(CLASS_NAMES) else str(idx)
    conf = float(probs[idx])
    return label, conf

def _lookup_guide(label: str) -> Optional[Dict[str, Any]]:
    """
    care_guides.json 구조 예:
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
    key2 = key.replace(" ", "_").replace("-", "_")
    return _GUIDES.get(key2)

async def _ask_openai_boost(label: str, base_ko: Optional[str], is_healthy: bool) -> Optional[str]:
    """
    OpenAI로 간단 보강 설명 요청(옵션).
    - healthy: 관리 팁 3가지
    - 질병: 기존 가이드 반복 피하고 실전 팁 2가지
    """
    if _OPENAI is None:
        return None

    try:
        if is_healthy:
            user = f"식물 잎 상태는 정상({label})으로 판정. 초보자용 한국어 관리 팁 3가지만 간단히."
        else:
            title = base_ko or label
            user = f"식물 질병 '{title}'에 대해, 이미 알려진 요약/해결책을 반복하지 말고 실전 팁 2가지를 한국어로 간단히."

        resp = await run_in_threadpool(
            lambda: _OPENAI.chat.completions.create(
                model=OPENAI_MODEL,
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
    - OpenAI로 보강설명(extraTips, 선택)
    """
    # 리소스 확보 시도(워밍업 실패했어도 여기서 다시 로드)
    try:
        _load_model_and_guides_once()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Server not ready: {e}")

    # 이미지 전처리 & 예측
    try:
        file_bytes = await image.read()
        arr = await run_in_threadpool(lambda: _preprocess_image(file_bytes))
        label, conf = await run_in_threadpool(lambda: _predict_label(arr))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid image or model error: {e}")

    # 가이드 매칭
    guide = _lookup_guide(label)
    is_healthy = (label.lower() == "healthy") or (
        guide and str(guide.get("label", "")).lower() == "healthy"
    )

    # OpenAI 보강(선택)
    extra = await _ask_openai_boost(label, guide.get("nameKo") if guide else None, is_healthy=is_healthy)

    result = AnalyzeResult(
        label=label,
        confidence=round(conf, 4),
        nameKo= (guide.get("nameKo") if guide else ("정상" if is_healthy else None)),
        description=(guide.get("description") if guide else ("겉보기 이상 없음" if is_healthy else None)),
        solution=(guide.get("solution") if guide else ("물·광량·통풍을 주기적으로 점검하세요." if is_healthy else None)),
        extraTips=extra
    )
    return AnalyzeResponse(ok=True, result=result)

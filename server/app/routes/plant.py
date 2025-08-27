# server/app/routes/plant.py
from fastapi import APIRouter, UploadFile, File, HTTPException
from pydantic import BaseModel
from PIL import Image
import io

from app.services.vision_openai import analyze_image
from app.services.advice_rag import get_care_advice
from app.services.vision_keras import analyze_image_with_keras

router = APIRouter()

class AnalyzeResponse(BaseModel):
    plant: str
    disease: str
    confidence: float
    advice: str
    references: list[str] = []

@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze(file: UploadFile = File(...)):
    if not file.content_type or "image" not in file.content_type:
        raise HTTPException(400, "이미지 파일을 업로드하세요.")
    img_bytes = await file.read()
    # (선택) 유효성 체크/리사이즈
    try:
        Image.open(io.BytesIO(img_bytes)).verify()
    except Exception:
        raise HTTPException(400, "손상된 이미지입니다.")

    # A. 비전 분석 (OpenAI/자체모델/Custom Vision 중 택1)
    plant, disease, confidence = await analyze_image(img_bytes)

    # B. 해결책 (벡터검색 기반 RAG)
    advice, refs = await get_care_advice(plant, disease)

    return AnalyzeResponse(
        plant=plant, disease=disease, confidence=confidence,
        advice=advice, references=refs
    )

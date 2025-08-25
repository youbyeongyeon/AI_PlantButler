from fastapi import APIRouter, UploadFile, File
from app.services.ai_service import analyze_plant_image

router = APIRouter()

@router.post("/upload")
async def upload_plant(file: UploadFile = File(...)):
    # 1. 이미지 읽기
    image_bytes = await file.read()
    
    # 2. AI 분석 (뼈대)
    result = analyze_plant_image(image_bytes)
    
    # 3. 결과 반환
    return {"result": result}

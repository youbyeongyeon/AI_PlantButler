def analyze_plant_image(image_bytes: bytes):
    """
    AI 분석 뼈대 함수
    실제 Azure Custom Vision 호출 코드를 여기서 작성
    """
    # TODO: Azure Custom Vision API 호출 로직 추가
    # 예시 반환
    return {
        "plant_name": "Tomato",
        "disease": "Healthy",
        "confidence": 0.95,
        "advice": "Keep watering regularly"
    }


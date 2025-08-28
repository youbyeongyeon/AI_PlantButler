# server/app/services/vision_openai.py
import base64, json, time
from fastapi import HTTPException
from openai import OpenAI
from app.config import settings

client = OpenAI(api_key=settings.OPENAI_API_KEY)

SYSTEM_PROMPT = """당신은 식물 질병 진단 보조모델입니다.
이미지에서 작물명(추정), 의심 병명, 신뢰도(0~1)를 JSON으로만 답하세요:
{"plant": "...", "disease": "...", "confidence": 0.0}
모르면 {"plant":"unknown","disease":"unknown","confidence":0.0}로.
"""

def _img_b64(image_bytes: bytes) -> str:
    return "data:image/jpeg;base64," + base64.b64encode(image_bytes).decode("utf-8")

async def analyze_image(image_bytes: bytes):
    img_url = _img_b64(image_bytes)

    # 간단 재시도(429 대비)
    for attempt in range(3):
        try:
            resp = client.chat.completions.create(
                model="gpt-4o",  # 비전 입력 지원 모델이어야 함
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": [
                        {"type": "text", "text": "이 사진의 작물/증상/의심 병명을 추론하고 JSON으로만 답하세요."},
                        {"type": "image_url", "image_url": {"url": img_url}}
                    ]},
                ],
                temperature=0.2,
                max_tokens=300,
            )
            txt = resp.choices[0].message.content
            data = json.loads(txt)
            return (
                data.get("plant", "unknown"),
                data.get("disease", "unknown"),
                float(data.get("confidence", 0.0)),
            )
        except Exception as e:
            # 429 쿼터/레이트 제한이면 짧게 대기 후 재시도
            if "429" in str(e) or "insufficient_quota" in str(e):
                time.sleep(1.5 * (attempt + 1))
                continue
            raise

    # 여기까지 오면 레이트/쿼터 문제 지속
    raise HTTPException(status_code=503, detail="Upstream model rate-limited or quota exhausted.")

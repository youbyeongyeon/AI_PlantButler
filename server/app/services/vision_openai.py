# server/app/services/vision_openai.py
import base64
from openai import OpenAI
from app.config import settings

client = OpenAI(api_key=settings.OPENAI_API_KEY)

SYSTEM_PROMPT = """당신은 식물 질병 진단 보조모델입니다.
이미지에서 작물명(추정), 의심 병명, 신뢰도(0~1)를 JSON으로만 답하세요:
{"plant": "...", "disease": "...", "confidence": 0.0}
모르면 {"plant":"unknown","disease":"unknown","confidence":0.0}로.
"""

async def analyze_image(image_bytes: bytes):
    b64 = base64.b64encode(image_bytes).decode("utf-8")
    # 모델 이름은 계정에서 사용 가능한 비전 모델로 교체 (예: gpt-4o, gpt-4o-mini)
    resp = client.chat.completions.create(
        model="gpt-4o",
        messages=[
            {"role":"system","content":SYSTEM_PROMPT},
            {"role":"user","content":[
                {"type":"input_text","text":"이 사진을 분석해 주세요."},
                {"type":"input_image","image_url": f"data:image/jpeg;base64,{b64}"}
            ]}
        ],
        temperature=0.2,
    )
    txt = resp.choices[0].message.content
    # 간단 파싱(실서비스는 json.loads try/except, 정규화)
    import json
    data = json.loads(txt)
    return data.get("plant","unknown"), data.get("disease","unknown"), float(data.get("confidence",0.0))

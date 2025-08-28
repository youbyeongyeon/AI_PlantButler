# server/app/services/advice_rag.py
import json
from pathlib import Path

GUIDE = json.loads(Path("data/care_guides.json").read_text(encoding="utf-8"))

async def get_care_advice(plant: str, disease: str):
    # MVP: 정직 매핑
    entry = GUIDE.get(disease.lower()) or GUIDE.get("default", {})
    advice = entry.get("advice","전문가의 추가 진단이 필요합니다.")
    refs = entry.get("references",[])
    return advice, refs

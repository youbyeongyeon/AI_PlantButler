# server/app/services/vision_keras.py
from __future__ import annotations
from typing import Tuple
import io
import numpy as np
from PIL import Image

from keras.models import load_model
import threading

# ===== 경로/하이퍼파라미터 (학습 시와 동일하게 맞추세요) =====
MODEL_PATH = "models/plant_disease_detector.keras"
INPUT_SIZE = (224, 224)  # 학습할 때의 입력 크기로 교체
CLASS_NAMES = [
    # 학습 클래스 순서대로 정확히 채우세요
    "healthy",
    "late_blight",
    "powdery_mildew",
    "leaf_spot"
]

# ===== 모델 로드 (앱 시작 시 1회) =====
_model = None
_model_lock = threading.Lock()

def _get_model():
    global _model
    if _model is None:
        with _model_lock:
            if _model is None:
                _model = load_model(MODEL_PATH)
                # 워밍업(콜드스타트 방지)
                dummy = np.zeros((1, INPUT_SIZE[0], INPUT_SIZE[1], 3), dtype=np.float32)
                _model.predict(dummy)
    return _model

def _preprocess(img_bytes: bytes) -> np.ndarray:
    img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
    img = img.resize(INPUT_SIZE)
    arr = np.asarray(img, dtype=np.float32) / 255.0
    arr = np.expand_dims(arr, axis=0)  # (1, H, W, 3)
    return arr

def _postprocess(pred: np.ndarray) -> Tuple[str, float]:
    # pred: (1, C) softmax 가정
    idx = int(np.argmax(pred, axis=1)[0])
    return CLASS_NAMES[idx], float(pred[0, idx])

async def analyze_image_with_keras(img_bytes: bytes) -> tuple[str, str, float]:
    """
    반환: (plant, disease, confidence)
    plant 분류 모델이 따로 없다면 'unknown' 유지
    """
    x = _preprocess(img_bytes)
    model = _get_model()
    pred = model.predict(x)
    disease, conf = _postprocess(pred)
    plant = "unknown"
    return plant, disease, conf

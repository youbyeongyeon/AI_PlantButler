# Keras 모델 로드 & 추론
from typing import Tuple
import numpy as np
from PIL import Image
import io

# Keras 3 (tf.keras와 호환)
from keras.models import load_model

# 1) 서버 시작 때 1회 로드(가장 중요: 전역 싱글톤)
MODEL = load_model("models/plant_disease_model.keras")  # 경로 맞추기
INPUT_SIZE = (224, 224)  # 학습 시 사용한 입력 크기로 교체
CLASS_NAMES = [
    "healthy",
    "late_blight",
    "powdery_mildew",
    "leaf_spot",
]  # 학습 클래스 순서대로

def _preprocess(img_bytes: bytes) -> np.ndarray:
    # PIL로 열기 → 리사이즈 → [0,1] 정규화 → NCHW/ NHWC 확인(보통 NHWC)
    img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
    img = img.resize(INPUT_SIZE)
    arr = np.array(img).astype(np.float32) / 255.0
    arr = np.expand_dims(arr, axis=0)  # (1, H, W, 3)
    return arr

def _postprocess(prob: np.ndarray) -> Tuple[str, float]:
    # prob: (1, num_classes) softmax라고 가정
    idx = int(np.argmax(prob, axis=1)[0])
    return CLASS_NAMES[idx], float(prob[0, idx])

async def analyze_image_with_keras(img_bytes: bytes) -> tuple[str, str, float]:
    """
    반환: (plant, disease, confidence)
    plant는 모르면 "unknown"으로 두고, disease만 분류하는 경우가 많음
    """
    x = _preprocess(img_bytes)
    pred = MODEL.predict(x)  # (1, C)
    disease, conf = _postprocess(pred)

    # 필요하면 plant 분류 모델을 따로 두거나, 하나의 모델에서 멀티헤드로 출력
    plant = "unknown"
    return plant, disease, conf

# AI_PlantButler 서버 팀원용 실행 가이드

## 1️⃣ 레포 클론 및 패치 적용
1. GitHub에서 레포 클론:
```powershell
git clone https://github.com/youbyeongyeon/AI_PlantButler.git
cd AI_PlantButler
git apply server_initial.patch
ls server/app
# main.py, routes/, services/ 디렉토리 확인

2️⃣ 로컬 Python 환경 테스트 (선택)

서버 디렉토리 이동:

cd server


가상환경 생성 및 활성화:

python -m venv venv
venv\Scripts\activate


패키지 설치:

pip install -r requirements.txt


서버 실행:

python -m uvicorn app.main:app --reload


브라우저 확인:

기본 엔드포인트: http://127.0.0.1:8000

Swagger UI: http://127.0.0.1:8000/docs

3️⃣ Docker 환경 실행 (추천)

서버 디렉토리에서 Docker 이미지 빌드:

docker build -t plantbutler-server .


컨테이너 실행:

docker run -p 8000:8000 plantbutler-server


브라우저: http://127.0.0.1:8000/docs

/plant/upload API 테스트 가능 (Postman 등 사용)

기존 컨테이너 삭제 필요 시:

docker ps -a
docker rm <container_id>


4️⃣ 업로드 API 테스트

엔드포인트: POST /plant/upload

Body → form-data → key: file, value: 이미지 파일

반환 예시:

{
  "result": {
    "plant_name": "Tomato",
    "disease": "Healthy",
    "confidence": 0.95,
    "advice": "Keep watering regularly"
  }
}


5️⃣ 향후 확장

Azure Custom Vision / ML 모델 연동

DB(PostgreSQL 등) 연결

JWT 인증, 유저 관리

Android 클라이언트와 통합 테스트
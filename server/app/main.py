from fastapi import FastAPI
from app.routes import plant

app = FastAPI(title="AI Plant Butler Server")

# 라우터 등록
app.include_router(plant.router, prefix="/plant")

@app.get("/")
def root():
    return {"message": "AI Plant Butler Server is running 🚀"}

@app.get("/health")
def health_check():
    return {"status": "ok"}
GitHub 팀 프로젝트 작업 가이드
1. 브랜치 작업 시작

본인 브랜치로 이동
git checkout <본인_브랜치>

최신 main 브랜치 병합
git merge main

main 브랜치 최신 상태를 반영하여 충돌 최소화

2. 작업 수행

코드 수정, 기능 구현 등 작업 진행

3. 변경사항 커밋

git add .
git commit -m "작업 내용 설명"

4. 브랜치 원격 저장소에 푸시

git push origin <본인_브랜치>

5. Pull Request(PR) 생성 (권장)

GitHub에서 본인 포크 레포 접속

작업 브랜치 선택 (<본인_브랜치>)

Compare & Pull Request 클릭

PR 작성 화면에서:

Base repository: 원본 레포 (Team/Project)

Base branch: main

Head repository: 본인 포크 레포

Compare branch: <본인_브랜치>

제목과 설명 작성 후 Create Pull Request 클릭

원본 레포 관리자가 PR 확인 후 Merge

PR 방식은 팀원 작업 기록과 리뷰 관리가 가능하여 충돌 방지에 안전함

6. 작업 브랜치 병합 후 main 브랜치 업데이트

git checkout main
git merge <본인_브랜치>
git push origin main

main 브랜치에 최종 반영

7. 서버/클라이언트 동시 작업 주의

server와 client는 동시에 작업 가능

단, main 브랜치 병합 전 팀원과 사전 협의 필수

동시에 main에 병합 시 충돌 발생 가능

8. 팀원 포크 동기화 (선택)

원본 레포 변경사항을 자신의 포크와 로컬에 반영
git fetch upstream
git checkout main
git merge upstream/main
git push origin main

✅ 요약

본인 브랜치로 이동 → main 최신 병합

작업 → 커밋 → 브랜치 푸시

PR 생성 → 원본 main 머지

main 브랜치 병합 및 푸시

서버/클라이언트 동시 작업 시 사전 협의

필요 시 upstream에서 동기화

# 배포 가이드

AWS EC2 한 대에 도커 상자 4개(nginx · 톰캣 · MySQL · Redis)를 띄우는 방법.

```
                    브라우저
                       │
                       ▼  80 포트
              📦 frontend (nginx)      ← 외부에 열리는 유일한 상자
                       │  /api 요청만 넘김
                       ▼
              📦 backend (톰캣 9)
                       │
              ┌────────┴────────┐
              ▼                 ▼
          📦 mysql          📦 redis
```

**빌드는 노트북에서, 실행은 서버에서** 한다. 서버(2GB)에서 Gradle·npm 빌드를 돌리면 메모리가 부족해 멈출 수 있다.

---

## 준비물

- AWS 계정 (EC2 t3.small · 저장공간 20GB · 서울 리전)
- 키페어 `.pem` 파일
- 노트북에 JDK 17, Node 22+

---

## 1. 노트북에서 빌드

```bash
cd backend && ./gradlew clean war -Pprod -Pdemo
```

```bash
cd frontend && npm run build
```

결과물 두 개가 생긴다. 이것만 서버로 보낸다.

- `backend/build/libs/backend-0.0.1-SNAPSHOT.war`
- `frontend/dist/`

 > `-Pprod` 는 개인 비밀 설정(`application-secret.properties`)을 WAR 에서 빼고,
> `-Pdemo` 는 데모 계정 시드를 다시 넣는다. 포트폴리오로 공개하는 서버라 데모 계정이 필요하다
> (방문자가 가입·연동·설문을 다 거치지 않아도 완성된 리포트를 볼 수 있다).
> 실제 서비스로 전환할 때는 `-Pdemo` 만 빼면 된다.
>
> `build` 가 아니라 `war` 를 쓴다. `build` 는 테스트까지 돌리는데, DB 연동 테스트가 있어
> 로컬 MySQL 이 떠 있지 않으면 실패한다. 배포용 산출물을 만드는 데는 `war` 면 충분하다.

### 서버에 올리기 전에 노트북에서 확인하기 (권장)

서버에서 문제를 찾으면 오래 걸린다. 같은 구성을 노트북에서 먼저 띄워본다.

```bash
cp .env.prod.example .env   # 값은 아무 문자열이나 채워도 로컬 테스트는 된다
```

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

`http://localhost` 로 접속해 회원가입까지 되면 서버에서도 거의 그대로 된다.

확인이 끝나면 정리한다.

```bash
docker compose -f docker-compose.prod.yml down
```

> 이 파일은 프로젝트 이름이 `finance-match-prod` 라 로컬 개발용(`docker-compose.yml`)과 섞이지 않는다.
> 컨테이너도 볼륨도 따로 쓰므로 개발 DB 는 건드리지 않는다.

---

## 2. 서버(EC2) 만들기

AWS 콘솔 → EC2 → **인스턴스 시작**

| 항목 | 값 |
| --- | --- |
| 이름 | `finance-match` |
| OS | Ubuntu Server 22.04 LTS |
| 인스턴스 유형 | `t3.small` |
| 키 페어 | 새로 생성 후 `.pem` 다운로드 (**분실 시 접속 불가**) |
| 스토리지 | 20 GiB gp3 |

**보안 그룹** — 인바운드 규칙 두 개만 만든다.

| 유형 | 포트 | 소스 |
| --- | --- | --- |
| SSH | 22 | 내 IP |
| HTTP | 80 | 위치 무관 (0.0.0.0/0) |

> 8080·3306·6379 는 열지 않는다. 상자끼리는 도커 내부 네트워크로 통신하므로 열 필요가 없고,
> 열면 서버 IP 만 알면 DB 에 직접 접속할 수 있게 된다.

---

## 3. 서버에 접속해서 도커 설치

```bash
chmod 400 ~/Downloads/내키.pem
```

```bash
ssh -i ~/Downloads/내키.pem ubuntu@서버IP
```

접속되면 도커를 설치한다.

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2
```

```bash
sudo usermod -aG docker ubuntu && exit
```

**한 번 나갔다 다시 접속해야** 도커 권한이 적용된다.

```bash
ssh -i ~/Downloads/내키.pem ubuntu@서버IP
docker --version
```

---

## 4. 파일 올리기

**노트북 터미널**에서 실행한다 (서버 아님).

```bash
ssh -i ~/Downloads/내키.pem ubuntu@서버IP 'mkdir -p ~/finance-match/backend/build/libs ~/finance-match/frontend'
```

```bash
cd ~/Desktop/finance-match && scp -i ~/Downloads/내키.pem docker-compose.prod.yml .env.prod.example ubuntu@서버IP:~/finance-match/
```

```bash
scp -i ~/Downloads/내키.pem backend/Dockerfile ubuntu@서버IP:~/finance-match/backend/ && scp -i ~/Downloads/내키.pem backend/build/libs/*.war ubuntu@서버IP:~/finance-match/backend/build/libs/
```

```bash
scp -i ~/Downloads/내키.pem frontend/Dockerfile frontend/nginx.conf ubuntu@서버IP:~/finance-match/frontend/ && scp -i ~/Downloads/내키.pem -r frontend/dist ubuntu@서버IP:~/finance-match/frontend/
```

---

## 5. 비밀값 작성 (서버에서)

```bash
cd ~/finance-match && cp .env.prod.example .env
```

먼저 값을 만든다.

```bash
openssl rand -base64 24    # 비밀번호용 — 3번 실행해서 각각 복사
openssl rand -base64 48    # JWT_SECRET 용
```

```bash
nano .env
```

빈 칸을 채운다. `CORS_ALLOWED_ORIGINS` 에는 `http://서버IP` 를 적는다.

저장은 `Ctrl+O` → `Enter`, 종료는 `Ctrl+X`.

> **이 파일은 절대 깃에 올리지 않는다.** `.gitignore` 에 이미 걸려 있다.

---

## 6. 실행

```bash
cd ~/finance-match && docker compose -f docker-compose.prod.yml up -d --build
```

처음에는 이미지를 받느라 3~5분 걸린다.

```bash
docker compose -f docker-compose.prod.yml ps
```

상자 4개가 전부 `running` 이면 성공이다.

---

## 7. 확인

브라우저에서 순서대로 열어본다.

1. `http://서버IP/api/health` → `{"success":true,"data":"ok"}` 가 나오면 백엔드 정상
2. `http://서버IP` → 온보딩 화면
3. 회원가입 → 로그인
4. 아무 화면에서 **새로고침** → 404 가 안 나야 정상

---

## 8. 다시 배포할 때 — 자동

**`develop` 에 머지하면 자동으로 배포된다.** 손으로 할 일이 없다.
진행 상황은 GitHub 저장소의 **Actions** 탭에서 `deploy` 워크플로로 볼 수 있다.

수동으로 돌리고 싶으면 Actions → `deploy` → **Run workflow**.

### 최초 1회 — Secrets 등록

자동 배포가 서버에 접속하려면 두 값이 필요하다.
저장소 **Settings → Secrets and variables → Actions → New repository secret**.

| 이름 | 값 |
| --- | --- |
| `EC2_HOST` | 서버 공인 IP |
| `EC2_SSH_KEY` | `.pem` 파일 **내용 전체** (`-----BEGIN` ~ `END-----` 포함) |

> Secrets 는 등록 후 다시 볼 수 없고 로그에도 찍히지 않는다. `.pem` 파일 자체는 저장소에
> 절대 커밋하지 않는다.

### 배포가 하는 일

변경 범위 판단 → 빌드(`-Pprod -Pdemo`) → 산출물 전송 → 컨테이너 재시작 → 헬스체크 → 기록.
헬스체크까지 통과해야 성공으로 표시된다. 실패하면 서버 로그 40줄이 함께 출력된다.

**바뀐 쪽만 빌드하고 전송한다.** 프론트만 고친 배포는 백엔드를 빌드하지도, WAR 를 보내지도 않는다.
기준은 서버에 기록된 `~/finance-match/.deployed_sha`(마지막으로 배포에 성공한 커밋)다.
직전 커밋과 비교하지 않는 이유는, 배포가 한 번 실패하면 그때 빠진 변경이 영영 올라가지 않기
때문이다. 기록은 성공한 뒤에만 갱신한다.

전송은 `rsync` 라 내용이 같은 파일은 건너뛴다. `dist` 는 86개 파일이지만 Vite 가 파일명에
내용 해시를 붙이므로, 실제로 바뀐 몇 개만 전송된다.

`.env`(비밀값)는 서버에만 있고 배포가 건드리지 않는다. 값을 바꿨다면 재시작만 하면 된다.

> DB 스키마가 바뀐 경우에도 별도 작업은 없다. 백엔드가 뜰 때 Flyway 가 자동으로 적용한다
> (`FLYWAY_ENABLED=true`).

---

## 8-1. 수동 배포 (자동이 막혔을 때)

```bash
cd backend && ./gradlew war -Pprod -Pdemo && cd ../frontend && npm run build && cd ..
```

```bash
scp -i ~/Downloads/내키.pem backend/build/libs/*.war ubuntu@서버IP:~/finance-match/backend/build/libs/ && scp -i ~/Downloads/내키.pem -r frontend/dist/. ubuntu@서버IP:~/finance-match/frontend/dist/
```

```bash
ssh -i ~/Downloads/내키.pem ubuntu@서버IP 'cd ~/finance-match && docker compose -f docker-compose.prod.yml up -d --build'
```

> **`-Pdemo` 를 빠뜨리지 말 것.** 데모 계정이 WAR 에서 빠진다.

---

## 자주 쓰는 명령어 (서버에서)

```bash
docker compose -f docker-compose.prod.yml ps          # 상태 보기
```

```bash
docker compose -f docker-compose.prod.yml logs -f backend   # 백엔드 로그
```

```bash
docker compose -f docker-compose.prod.yml restart backend   # 백엔드만 재시작
```

```bash
docker compose -f docker-compose.prod.yml down        # 전부 끄기 (데이터는 남음)
```

```bash
docker compose -f docker-compose.prod.yml exec mysql mysql -u root -p finance_match   # DB 접속
```

---

## 문제가 생기면

### 화면이 안 뜬다

```bash
docker compose -f docker-compose.prod.yml ps
```

`frontend` 가 `restarting` 을 반복하면 백엔드 상자를 못 찾은 것이다. 백엔드가 먼저 뜨면 자동으로 붙는다.

### 백엔드가 계속 죽는다

```bash
docker compose -f docker-compose.prod.yml logs backend | tail -50
```

흔한 원인 세 가지.

| 로그에 보이는 것 | 원인 |
| --- | --- |
| `Could not resolve placeholder 'DB_URL'` | `.env` 를 안 만들었거나 값이 비어 있다 |
| `Could not resolve placeholder '...'` (다른 이름) | `application-prod.properties` 에 그 설정이 빠졌다. 로컬 설정에만 추가하고 운영 설정에 안 넣으면 배포 때 터진다 |
| `WeakKeyException` | `JWT_SECRET` 이 32자보다 짧다 |
| `Access denied for user` | `.env` 의 DB 비밀번호가 안 맞는다. `down -v` 로 볼륨까지 지우고 다시 올려야 한다 |

### API 는 되는데 화면 새로고침 시 404

`frontend/nginx.conf` 의 `try_files` 줄이 빠진 것이다.

### 메모리가 부족하다

```bash
free -h
docker stats --no-stream
```

`docker-compose.prod.yml` 의 `-Xmx640m` 을 낮추거나, 인스턴스를 `t3.medium` 으로 올린다.

---

## 요금 아끼기

쓰지 않는 동안에는 **인스턴스를 중지**해 둔다. 중지하면 서버 요금은 안 나가고 저장공간 값만 나간다.

AWS 콘솔 → EC2 → 인스턴스 선택 → **인스턴스 상태** → **중지**

> 중지 후 다시 시작하면 **공인 IP 가 바뀐다.** 주소를 고정하려면 탄력적 IP(Elastic IP)를 붙여야 하고,
> 붙여두면 중지 중에도 소액이 과금된다.

---

## 데모 계정

운영 DB 에 데모 회원 8명·커플 4쌍·리포트 4건이 들어 있다. 비밀번호는 모두 `Test1234!`.
시나리오별 계정 목록은 [`backend/src/main/java/com/financematch/asset/README.md`](./backend/src/main/java/com/financematch/asset/README.md) 참고.

들어가려면 **두 가지가 모두** 맞아야 한다. 하나라도 빠지면 데모 계정이 생성되지 않는다.

1. WAR 를 `-Pprod -Pdemo` 로 빌드 (자동 배포는 이미 그렇게 한다)
2. 서버 `.env` 에 `FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/dev-seed`

> 데모 시드는 회원 테이블을 비우고 다시 채운다. 다만 **시드 파일이 바뀔 때만 재실행**되므로,
> 평소 배포에서는 그 사이 가입한 회원이 지워지지 않는다. 시드를 수정하는 날에는 지워진다.

---

## 아직 안 한 것

- **HTTPS** — 지금은 `http` 라 브라우저에 "안전하지 않음" 이 뜬다. 도메인을 붙이고 Let's Encrypt 인증서를 받으면 해결된다.
- **AI 종합 코멘트** — `OPENAI_API_KEY` 를 비워두면 리포트의 AI 코멘트만 생성되지 않는다(나머지는 정상). 키를 넣으면 호출할 때마다 비용이 발생한다.

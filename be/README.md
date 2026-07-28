# 백엔드 구조 가이드

새 기능을 만들 때 **파일을 어디에 두고 어떻게 이름 붙일지**를 정리한 문서. 컨벤션 정본은 루트 [`AGENTS.md`](../AGENTS.md).

---

## 1. 폴더 구조

```
be/src/main/java/com/financematch/
├── common/                   ApiResponse, ErrorCode, HealthController
├── config/                   RootConfig, WebConfig, RedisConfig, WebAppInitializer
├── exception/                ApiException, GlobalExceptionHandler
└── <도메인>/                  ← 각 담당자가 추가
    ├── controller/
    ├── service/
    ├── mapper/               MyBatis 인터페이스
    ├── dto/                  요청 · 응답 객체
    └── domain/               VO

be/src/main/resources/
├── db/                       Flyway 마이그레이션 · 시드 (README 참고)
└── mappers/                  매퍼 XML (README 참고)
```

**도메인 우선 구조**다. 계층(`controller/`·`service/`)이 아니라 **도메인(`member/`·`couple/`)이 먼저** 온다. 기능 하나를 고칠 때 폴더 하나만 보면 되고, 담당자별 경계가 폴더로 드러난다.

### 예시 — 회원 도메인

```
com/financematch/member/
├── controller/
│   └── MemberController.java
├── service/
│   └── MemberService.java
├── mapper/
│   └── MemberMapper.java
├── dto/
│   ├── SignupRequest.java
│   └── SignupResponse.java
└── domain/
    └── Member.java
```

```
resources/mappers/member/MemberMapper.xml
```

> **빈 폴더는 만들지 않는다.** 상품 도메인처럼 VO가 없으면 `domain/`도 없다. 필요할 때 만든다.

---

## 2. 계층별 역할

| 폴더 | 역할 | 하지 말 것 |
|---|---|---|
| `controller/` | 요청을 받아 서비스에 넘기고 응답을 감싼다 | **비즈니스 로직 금지.** DB 접근 금지 |
| `service/` | 비즈니스 로직. 트랜잭션 경계 | HTTP 관련 코드(`HttpServletRequest` 등) 금지 |
| `mapper/` | MyBatis 인터페이스. SQL은 XML에 | 자바 코드에 SQL 문자열 금지 |
| `dto/` | 요청 · 응답 전용 객체 | 비즈니스 로직 금지 |
| `domain/` | DB 테이블과 대응하는 VO | — |

**DTO와 VO를 구분한다.** DTO는 API 계약(프론트와 주고받는 모양), VO는 DB 구조다. 둘을 하나로 쓰면 DB 컬럼을 바꿀 때 API 응답이 같이 바뀐다.

---

## 3. 네이밍

### 단수 · 복수

세 가지가 각각 다른 규칙을 쓴다. 헷갈리기 쉬우니 주의.

| | 예시 | 규칙 |
|---|---|---|
| **패키지** | `com.financematch.product` | **항상 단수** — 코드 묶음이지 컬렉션이 아니다 |
| **테이블** | `product` | **항상 단수** (AGENTS.md D2) |
| **클래스** | `Member`, `MemberController` | **항상 단수** |
| **URL** | `/api/v1/products` | **컬렉션일 때만 복수** |

URL만 복수인 이유는 여러 개를 담는 통을 가리킬 때라서다. 하나뿐이면 URL도 단수다.

```
/api/v1/products              여러 상품 중에서    → 복수
/api/v1/members/me/couple     내 커플 하나        → 단수
```

### 클래스 이름

```
MemberController · MemberService · MemberMapper · Member
SignupRequest · SignupResponse
```

DTO는 **용도 + Request/Response**로 짓는다. 도메인 폴더 안에 있으므로 `MemberSignupRequest`처럼 도메인을 다시 붙이지 않는다.

---

## 4. 계층 규칙

### Controller

```java
@RestController
@RequestMapping("/v1/auth")                 // 클래스 레벨에 /v1
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")                 // → /api/v1/auth/signup
    public ApiResponse<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }
}
```

- **경로에 `/api`를 쓰지 않는다.** DispatcherServlet이 `/api/*`에 매핑돼 있어 톰캣이 떼고 넘긴다
- **`/v1`은 클래스 레벨에** 둔다. 메서드마다 쓰면 언젠가 빠뜨리는데, **404가 아니라 엉뚱한 경로에 정상 등록**되어 발견이 늦다
- 성공은 `ApiResponse.ok(data)`로 감싼다. **실패 응답을 직접 만들지 않는다** — `ApiException`을 던지면 `GlobalExceptionHandler`가 변환한다
- 개인 리소스는 `/v1/members/me/...` 아래에 둔다

### Service

```java
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberMapper memberMapper;
}
```

- **인터페이스 없이 클래스 하나로** 만든다. 구현이 하나뿐인데 인터페이스를 두면 파일만 두 배가 된다. 구현을 갈아끼울 일이 생기면 그때 추출한다
- 의존성은 **생성자 주입**(`@RequiredArgsConstructor`). 필드에 `@Autowired` 쓰지 않는다
- 트랜잭션이 필요하면 `@Transactional`을 서비스 메서드에 붙인다

### Mapper

```java
@Mapper                                     // 생략 가능 — @MapperScan 이 잡는다
public interface MemberMapper {
    Member findByEmail(String email);
    int insert(Member member);
}
```

- 인터페이스만 만들고 **SQL은 XML에** 쓴다
- XML 위치와 namespace 규칙은 [`resources/mappers/README.md`](src/main/resources/mappers/README.md)

### 소유권 검증

경로에 `{id}`가 있으면 **"내 것 중에서 찾는" 쿼리**를 쓴다. 찾은 뒤에 검사하지 않는다.

```java
// 권장 — 조회 자체에 소유 조건이 들어간다
RecommendationSlot slot = slotMapper.findByIdAndCoupleId(slotId, myCoupleId);
if (slot == null) throw new ApiException(ErrorCode.NOT_FOUND);

// 지양 — 검사를 깜빡하면 남의 리소스가 노출된다
RecommendationSlot slot = slotMapper.findById(slotId);
if (!slot.getCoupleId().equals(myCoupleId)) throw ...;
```

`{productId}` 같은 공용 리소스는 예외다.

---

## 5. 새 도메인을 만들 때

1. `com/financematch/<도메인>/` 폴더 생성 — **단수**로
2. 필요한 계층 폴더만 만든다 (`controller/`·`service/`·`mapper/`·`dto/`·`domain/`)
3. 매퍼 XML은 `resources/mappers/<도메인>/`에
4. 새 에러 코드는 `common/ErrorCode.java`에 **자기 도메인 구획**을 만들어 추가

```java
// ===== 인증 (임민지) =====
EMAIL_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
```

`ErrorCode` enum 이름이 그대로 응답의 `code` 필드가 되어 프론트로 간다. **이름을 바꾸면 프론트 에러 분기도 함께 바꿔야 한다.**

---

## 6. 참고

| 문서 | 내용 |
|---|---|
| [`AGENTS.md`](../AGENTS.md) | 컨벤션 정본 — 커밋·브랜치·절대 규칙 |
| [`resources/db/README.md`](src/main/resources/db/README.md) | Flyway 마이그레이션 |
| [`resources/mappers/README.md`](src/main/resources/mappers/README.md) | 매퍼 XML |
| `API설계서_v2.xlsx` | 전체 엔드포인트 · API 설계 규칙 (저장소 밖) |

### 아직 없는 것

아래는 인증 작업에서 만들 예정이다. 지금은 코드에 없다.

- `@LoginMember` — JWT에서 회원 id를 꺼내 컨트롤러 파라미터로 주입하는 리졸버
- `SecurityConfig` — Spring Security 필터 체인

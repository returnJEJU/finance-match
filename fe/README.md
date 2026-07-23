# 찰떡궁합 Frontend (finance-match/fe)

Vue 3 + Vite + Tailwind CSS v4 기반 모바일 웹앱.

## 실행

```bash
npm install      # 최초 1회 (의존성 설치 · Pretendard 폰트 포함)
npm run dev      # 개발 서버 (http://localhost:5173)
npm run build    # 프로덕션 빌드
```

- 폰트는 **Pretendard**로 고정되어 있습니다(`src/assets/main.css`). 별도 설정 없이 `npm install`만 하면 적용됩니다.

---

## 레이아웃 (Layout) 사용법

화면(페이지)마다 상단바·하단탭을 직접 붙일 필요가 없습니다.
**레이아웃 2종이 상단바·하단탭을 대신 그려주므로, 화면은 내용만 만들고 라우터에서 레이아웃 밑에 넣기만** 하면 됩니다.

### 레이아웃 종류

| 레이아웃                    | 구성                                         | 쓰는 화면                                    |
| --------------------------- | -------------------------------------------- | -------------------------------------------- |
| `layouts/DefaultLayout.vue` | 상단바(AppHeader) + 내용 + 하단탭(BottomNav) | 홈 · 추천 · 리포트 · 마이페이지              |
| `layouts/BlankLayout.vue`   | 내용만 (상단바·탭 없음)                      | 로그인 · 회원가입 · 설문 · 로딩 등 몰입 화면 |

부품 컴포넌트(직접 쓸 일은 거의 없음):

- `components/layout/AppHeader.vue` — 상단바 (뒤로가기 · 로고/제목)
- `components/layout/BottomNav.vue` — 하단탭 (홈/추천/리포트/마이페이지)

### 새 화면 만드는 법 (2단계)

**① 페이지 컴포넌트 생성** — `src/pages/`에 내용만 작성 (상단바·탭 신경 X)

```vue
<!-- src/pages/ReportPage.vue -->
<template>
  <div>
    <!-- 리포트 화면 내용 -->
  </div>
</template>
```

**② 라우터에 등록** — `src/router/index.js`에서 레이아웃을 부모로 감싸고 `children`에 추가

```js
const routes = [
  // 탭 있는 화면들 → DefaultLayout
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/pages/HomePage.vue') },
      {
        path: 'recommend',
        name: 'recommend',
        component: () => import('@/pages/RecommendPage.vue'),
        meta: { title: '상품 추천', showBack: true },
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/pages/ReportPage.vue'),
        meta: { title: '리포트', showBack: true },
      },
      {
        path: 'my',
        name: 'my',
        component: () => import('@/pages/MyPage.vue'),
        meta: { title: '마이페이지', showBack: true },
      },
    ],
  },

  // 탭 없는 화면들 → BlankLayout
  {
    path: '/',
    component: () => import('@/layouts/BlankLayout.vue'),
    children: [
      { path: 'login', name: 'login', component: () => import('@/pages/LoginPage.vue') },
      { path: 'signup', name: 'signup', component: () => import('@/pages/SignupPage.vue') },
      { path: 'diagnose', name: 'diagnose', component: () => import('@/pages/DiagnosePage.vue') },
    ],
  },
]
```

→ 이렇게만 하면 `DefaultLayout` 자식 화면엔 상단바·탭이 **자동으로** 붙고, `BlankLayout` 자식 화면엔 붙지 않습니다. 화면 컴포넌트 안에서 상단바·탭 코드를 쓸 필요가 없습니다.

### 상단바 제어 (meta 옵션)

`DefaultLayout`은 라우트의 `meta`를 읽어 상단바를 그립니다.

| meta 키    | 타입    | 설명                                                    |
| ---------- | ------- | ------------------------------------------------------- |
| `title`    | string  | 상단바 가운데 제목. **비우면 로고**가 표시됨(홈 화면용) |
| `showBack` | boolean | 왼쪽 뒤로가기(`<`) 표시 여부. 기본 `false`              |

예:

- 홈 → `meta` 없음 또는 `{ showBack: false }` → 로고 표시, 뒤로가기 없음
- 리포트 → `{ title: '리포트', showBack: true }` → "리포트" 제목 + 뒤로가기

### 하단탭 활성 표시 규칙 ⚠️

`BottomNav`는 현재 라우트의 **`name`**으로 활성 탭을 판단합니다.
아래 `name`을 **정확히 맞춰야** 해당 탭 아이콘이 채워진(filled) 상태 + 진한 글씨로 바뀝니다.

| 탭         | 필요한 route name | 경로         |
| ---------- | ----------------- | ------------ |
| 홈         | `home`            | `/`          |
| 추천       | `recommend`       | `/recommend` |
| 리포트     | `report`          | `/report`    |
| 마이페이지 | `my`              | `/my`        |

---

## 폴더 구조 (레이아웃 관련)

```
src/
├─ layouts/
│  ├─ DefaultLayout.vue     # 상단바 + 탭
│  └─ BlankLayout.vue       # 화면만
├─ components/
│  └─ layout/
│     ├─ AppHeader.vue      # 상단바
│     └─ BottomNav.vue      # 하단탭
├─ pages/                   # 각자 만드는 화면들
├─ router/
│  └─ index.js              # 여기에 화면 등록
└─ assets/images/
   ├─ logo/logo-wordmark.png   # 상단바 로고(여백 제거본)
   └─ icons/                   # 하단탭 아이콘 (empty/filled 쌍)
```

## 아이콘 규칙

- **아이콘은 Lucide**(`lucide-vue-next`)를 사용합니다. 예: 상단바 뒤로가기 = `<ChevronLeft :size="24" />`.
  ```vue
  <script setup>
  import { ChevronLeft } from 'lucide-vue-next'
  </script>
  <template>
    <ChevronLeft :size="24" />
  </template>
  ```
- **예외:** 하단탭(BottomNav)만 프로젝트 에셋 아이콘(`assets/images/icons/`)을 사용합니다(홈/추천/리포트/마이페이지 · empty/filled 쌍).

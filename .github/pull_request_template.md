<!--
  PR 제목은 커밋 컨벤션과 동일하게: <type>(<scope>): <subject>
  예) feat(auth): 로그인 화면 추가   (scope는 fe/be/infra/config/api 또는 도메인 하나 — 결합 X)
-->

## 무엇을 / 왜 (What & Why)

<!-- 어떤 변경인지, 왜 필요한지 간단히 -->

## 작업 내용

<!-- 주요 변경 사항을 목록으로 -->
-
-

## 관련 이슈

<!-- 있으면 연결. 이 PR로 이슈가 닫히면 Closes 사용 -->
Closes #

## 체크리스트

- [ ] 타겟 브랜치가 `develop` 인가
- [ ] 커밋 메시지가 컨벤션을 따르는가 (`<type>(<scope>): <subject>`)
- [ ] 로컬에서 정상 동작을 확인했는가 (FE: `npm run dev` / BE: `./gradlew build`)
- [ ] Lint·포맷이 통과하는가 (커밋 시 lefthook 자동, FE)
- [ ] 리뷰어를 지정했는가 (승인 1명 필요)
- [ ] 불필요한 파일·비밀값(.env, secret)이 포함되지 않았는가

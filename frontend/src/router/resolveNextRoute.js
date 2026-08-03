/**
 * 로그인 직후 어느 화면으로 보낼지 정한다.
 *
 * 판단 재료는 로그인 응답의 `isFirstLogin` 과 `progress` 4개뿐이다. <b>위에서부터 먼저 걸리는
 * 조건이 답이다</b> — 순서를 바꾸면 결과가 달라진다.
 *
 * 초대를 "보낸 사람"과 "받은 사람"을 따로 구분하지 않는다. 두 흐름이 같은 표를 다르게 지날 뿐이다.
 *   - 보낸 사람: 공동설문 → 초대코드 생성(hasInvitation=true) → 개인설문 → 상대 대기
 *   - 받은 사람: 코드 입력(coupleConnected=true) → 개인설문 → 결과
 * 즉 `hasInvitation` 이 "내가 초대를 보냈다"는 표시 역할을 한다.
 *
 * 설문을 막 끝냈을 때의 이동(개인설문 → 개인성향 → 계산중 → 대시보드)은 각 화면이 이미 담당한다.
 * 여기서는 그 사슬의 <b>어느 지점에 떨어뜨릴지</b>만 정한다.
 */
export function resolveNextRoute({ isFirstLogin, progress }) {
  // 가입 직후 첫 로그인. 서비스 소개를 거쳐 초대 화면으로 넘어간다(이동은 소개 화면이 담당).
  if (isFirstLogin) {
    return { name: 'service-introduction' }
  }

  const {
    coupleConnected,
    hasInvitation,
    personalSurveyCompleted,
    partnerPersonalSurveyCompleted,
  } = progress

  if (!coupleConnected) {
    // 초대코드를 만들려면 공동설문을 먼저 해야 한다.
    if (!hasInvitation) {
      return { name: 'survey-couple' }
    }
    // 초대는 보냈고 상대가 아직 오지 않은 상태.
    //
    // 개인설문 전이라면 개인설문이 아니라 <b>생성된 초대코드 화면</b>으로 보낸다. 상대에게 코드를
    // 전달하는 것이 먼저이기 때문이다(그 화면이 코드를 직접 조회해 보여주고, 다음 버튼이
    // 개인설문으로 이어준다). 개인설문으로 바로 보내면 코드를 다시 찾아볼 길이 없다.
    return personalSurveyCompleted
      ? { name: 'dashboard-waiting' }
      : { name: 'couple-invite-created' }
  }

  if (!personalSurveyCompleted) {
    return { name: 'survey-personal' }
  }

  // 둘 다 끝났으면 계산 화면을 거쳐 대시보드로 간다(계산 화면이 대시보드로 넘긴다).
  return partnerPersonalSurveyCompleted
    ? { name: 'match-calculating' }
    : { name: 'dashboard-waiting' }
}

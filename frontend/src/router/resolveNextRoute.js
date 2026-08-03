/**
 * 로그인 직후 어느 화면으로 보낼지 정한다.
 *
 * 판단 재료는 로그인 응답의 `isFirstLogin` 과 `progress` 4개뿐이다. <b>위에서부터 먼저 걸리는
 * 조건이 답이다</b> — 순서를 바꾸면 결과가 달라진다.
 *
 * 초대를 "보낸 사람"과 "받은 사람"은 같은 표를 다르게 지난다.
 *   - 보낸 사람: 공동설문 → 초대코드 생성(hasInvitation=true) → 개인설문 → 상대 대기
 *   - 받은 사람: 코드 입력(coupleConnected=true) → 연결 완료(공동 목표 확인) → 개인설문 → 결과
 * 즉 `hasInvitation` 이 "내가 초대를 보냈다"는 표시 역할을 한다.
 *
 * 설문을 막 끝냈을 때의 이동(개인설문 → 개인성향 → 계산중 → 대시보드)은 각 화면이 이미 담당한다.
 * 여기서는 그 사슬의 <b>어느 지점에 떨어뜨릴지</b>만 정한다.
 */
export function resolveNextRoute({ isFirstLogin, progress }, redirect = null) {
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

  // 로그인이 필요해서 튕겨 나왔던 사람을 원래 가려던 화면으로 돌려보낸다.
  //
  // 단 <b>온보딩을 다 끝낸 사람에게만</b> 허용한다. 퍼널 중간에 있는 사람을 원하는 곳으로 보내면
  // 볼 것이 없는 화면(커플도 없는데 리포트 등)에 떨어진다. 그런 사람은 다음 단계로 안내하는 편이
  // 낫다.
  if (redirect && isOnboardingComplete(progress)) {
    return redirect
  }

  if (!coupleConnected) {
    // 아직 아무것도 하지 않은 사람. <b>코드를 만들 사람인지 받을 사람인지 알 수 없다.</b> 그래서
    // 초대 화면으로 보내 직접 고르게 한다 — 그 화면이 코드 입력과 공동설문(=코드 만들기) 양쪽으로
    // 이어준다. 공동설문으로 바로 보내면 만드는 쪽으로 단정하게 되어, 상대가 이미 보낸 코드를
    // 입력할 길이 사라진다.
    if (!hasInvitation) {
      return { name: 'couple-invite' }
    }
    // 초대는 보냈고 상대가 아직 오지 않은 상태.
    //
    // 개인설문 전이라면 개인설문이 아니라 <b>생성된 초대코드 화면</b>으로 보낸다. 상대에게 코드를
    // 전달하는 것이 먼저이기 때문이다(그 화면이 코드를 직접 조회해 보여주고, 다음 버튼이
    // 개인설문으로 이어준다). 개인설문으로 바로 보내면 코드를 다시 찾아볼 길이 없다.
    //
    // 개인설문까지 마쳤다면 지금 할 수 있는 일이 없으니 대기 화면으로 보낸다. 그 화면은 커플이
    // 아직 연결되지 않았음을 알아보고 문구를 바꾸며, 내 초대코드를 다시 볼 수 있는 길을 준다.
    return personalSurveyCompleted
      ? { name: 'dashboard-waiting' }
      : { name: 'couple-invite-created' }
  }

  // 커플은 연결됐고 개인설문만 남았다.
  //
  // 초대를 <b>받은 사람</b>은 공동설문을 하지 않는다. 파트너가 정해 둔 공동 목표를 아직 본 적이
  // 없으므로 연결 완료 화면에서 확인시킨 뒤 개인설문으로 넘긴다(이동은 그 화면이 담당한다).
  // 보낸 사람은 자기가 만든 목표라 다시 보여줄 이유가 없어 바로 개인설문으로 간다.
  //
  // ⚠️ 지금은 백엔드가 <b>연결되는 순간 초대코드를 USED 로 바꾸고</b>(`markInvitationUsed`)
  // `hasInvitation` 은 ACTIVE 인 것만 세기 때문에, 연결 후에는 보낸 사람도 false 가 되어 두
  // 사람이 갈리지 않는다 — 둘 다 연결 완료 화면으로 간다. 보낸 사람이 목표를 한 번 더 보게 될
  // 뿐이라 해는 없다. 백엔드에서 ACTIVE 조건을 빼기로 했고(담당자 조율 완료), 들어오면 이
  // 코드는 그대로 둔 채 의도대로 갈린다.
  if (!personalSurveyCompleted) {
    return hasInvitation ? { name: 'survey-personal' } : { name: 'couple-connected' }
  }

  // 둘 다 끝났으면 계산 화면을 거쳐 대시보드로 간다(계산 화면이 대시보드로 넘긴다).
  return partnerPersonalSurveyCompleted
    ? { name: 'match-calculating' }
    : { name: 'dashboard-waiting' }
}

/**
 * 온보딩을 모두 마쳐 앱을 자유롭게 돌아다녀도 되는 상태인지.
 *
 * 커플이 연결되고 두 사람의 개인설문이 모두 끝나야 리포트·추천·대시보드에 볼 것이 생긴다.
 */
export function isOnboardingComplete(progress) {
  return Boolean(
    progress?.coupleConnected &&
    progress?.personalSurveyCompleted &&
    progress?.partnerPersonalSurveyCompleted,
  )
}

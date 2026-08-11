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
  // 가입 직후 첫 로그인. 서비스 소개를 거쳐 커플 연동 시작 화면으로 넘어간다(이동은 소개 화면이 담당).
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

  // 커플 연동 전
  if (!coupleConnected) {
    // 초대 코드 아직 안 만든 초대자, 혹은 입력자
    if (!hasInvitation) {
      return { name: 'couple-start' }
    }

    // 초대 코드 만든 초대자
    return personalSurveyCompleted
      ? { name: 'dashboard-waiting' } // 개인 설문 완료한 초대자
      : { name: 'couple-invite-created' } // 개인 설문 미완료한 초대자 (상대에게 코드 전달할 수 있도록)
  }

  // 커플 연동 후, hasInvitation은 초대자와 입력자를 구분해줌
  // 백엔드에서 hasInvitation 판별 시 ACTIVE 조건을 뺀 이유는
  // 커플 연동 후 코드 상태가 USED로 바뀌더라도 초대자와 입력자를 구분하기 위함

  // 이제 커플 연동 완료된 상태
  if (!personalSurveyCompleted) {
    return hasInvitation
      ? { name: 'survey-personal' } // 초대자 (사용된 코드를 다시 노출시키지 않고 바로 개인 설문 하도록)
      : { name: 'couple-connected' } // 입력자 (공동 목표 확인 후 개인 설문 하도록)
  }

  // 내 개인 설문까지 끝난 상태
  // 파트너도 완료했으면 완성된 대시보드로, 아직이라면 미완성 대시보드로 이동
  // match-calculating 은 "방금 막 둘 다 끝난" 최초 1회에만 필요한 화면이라(SurveyResultPage.vue 가 그 케이스를 담당)
  // 이미 계산이 끝나 있을 로그인 시점엔 계산 중 애니메이션을 다시 보여줄 이유가 없다.
  return partnerPersonalSurveyCompleted ? { name: 'dashboard' } : { name: 'dashboard-waiting' }
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

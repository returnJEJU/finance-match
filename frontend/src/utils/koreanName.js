// 화면에 이름을 부를 때 성을 뗀 축약형으로 보여준다 (백엔드 KoreanNameFormatter 와 같은 규칙).
// 예: "김민수" → "민수". 두 글자 이하 이름은 성을 떼면 어색하거나 아무것도 안 남아 그대로 둔다.
// 두 글자 성씨(남궁·황보 등)는 지원하지 않는다 — 필요해지면 성씨 목록을 추가한다.
//
// 정렬(가나다순)이나 신원 식별에는 이 축약형이 아니라 원래 전체 이름을 써야 한다.
export function abbreviateKoreanName(fullName) {
  if (!fullName || fullName.length <= 1) {
    return fullName
  }
  return fullName.slice(1)
}

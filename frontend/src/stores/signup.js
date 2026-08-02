import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 회원가입 퍼널의 임시 보관소.
 *
 * 회원가입은 화면 3개에 걸쳐 입력받지만 백엔드는 한 번에 받는다
 * (`POST /api/v1/auth/signup`). 화면 안의 ref 는 다음 화면으로 넘어가면 사라지므로,
 * 1·2단계 입력값을 여기에 모아뒀다가 3단계에서 한 번에 보낸다.
 *
 * <b>필드 이름을 백엔드 계약과 똑같이 맞춰 뒀다.</b> 화면과 이름이 다르면 보낼 때 옮겨 담는
 * 코드가 생기고, 항목이 늘 때마다 거기를 빠뜨려 INVALID_INPUT 이 난다. 그래서 성별은 화면에서부터
 * 'F'·'M' 을, 약관은 'mydataTerms'·'assetLink'·'coupleShare' 를 그대로 쓴다.
 *
 * 저장 위치는 <b>메모리</b>다(localStorage 아님). 비밀번호를 잠깐 들고 있어야 하므로
 * 디스크에 남기지 않고, 가입이 끝나면 reset() 으로 즉시 지운다. 대신 새로고침하면 사라지므로
 * 3단계에서 isReady 를 확인해 1단계로 돌려보낸다.
 */
export const useSignupStore = defineStore('signup', () => {
  /** 1단계 — 정보 입력. passwordConfirm 은 화면에서만 쓰는 값이라 담지 않는다. */
  const form = ref(emptyForm())

  /** 2단계 — 약관 동의. 필수 4개 + 선택 1개. */
  const agreements = ref(emptyAgreements())

  /** 백엔드로 보낼 형태. 옮겨 담지 않고 그대로 합치기만 한다. */
  const payload = computed(() => ({
    ...form.value,
    agreements: { ...agreements.value },
  }))

  /**
   * 3단계에 도달했을 때 보낼 것이 갖춰져 있는지.
   *
   * 새로고침이나 주소 직접 입력으로 중간 단계에 들어온 경우를 걸러낸다. 필수 약관 검사는
   * 백엔드도 하지만(CONSENT_REQUIRED), 여기서는 "1·2단계를 실제로 거쳤는가"만 본다.
   */
  const isReady = computed(
    () =>
      Boolean(
        form.value.name &&
        form.value.gender &&
        form.value.birthDate &&
        form.value.email &&
        form.value.password,
      ) && Object.values(agreements.value).some(Boolean),
  )

  function setForm(values) {
    form.value = { ...form.value, ...values }
  }

  function setAgreements(values) {
    agreements.value = { ...agreements.value, ...values }
  }

  /** 가입 완료·이탈 시 호출한다. 비밀번호를 메모리에 남겨두지 않기 위한 것이다. */
  function reset() {
    form.value = emptyForm()
    agreements.value = emptyAgreements()
  }

  return { form, agreements, payload, isReady, setForm, setAgreements, reset }
})

function emptyForm() {
  return {
    name: '',
    gender: '', // 'F' | 'M'
    birthDate: '', // 'YYYY-MM-DD'
    email: '',
    password: '',
  }
}

function emptyAgreements() {
  return {
    mydataTerms: false,
    privacy: false,
    assetLink: false,
    coupleShare: false,
    marketing: false,
  }
}

# 매퍼 XML

MyBatis 매퍼 XML을 두는 곳. 구조 전반은 [`be/README.md`](../../../../README.md).

---

## 1. 위치

```
be/src/main/resources/mappers/<도메인>/XxxMapper.xml
```

```
mappers/
├── member/MemberMapper.xml
├── couple/CoupleMapper.xml
└── product/ProductMapper.xml
```

`RootConfig`가 이 경로를 읽는다.

```java
.getResources("classpath*:mappers/**/*.xml")
```

**이 패턴에 맞지 않으면 조용히 무시된다.** 에러가 나지 않고 그냥 안 읽히므로, 쿼리를 호출할 때가 되어서야 알게 된다.

| 잘못된 예 | 왜 |
|---|---|
| `resources/mapper/MemberMapper.xml` | `mappers`(복수)가 아님 |
| `resources/com/financematch/member/MemberMapper.xml` | `mappers/` 아래가 아님 |
| `mappers/member/MemberMapper.xml.example` | 확장자가 `.xml`이 아님 |

> 파일 이름은 자바 인터페이스와 같게 짓는다 — `MemberMapper.java` ↔ `MemberMapper.xml`.

---

## 2. namespace — 가장 자주 틀리는 부분

`namespace`는 **자바 인터페이스의 전체 경로**와 정확히 같아야 한다. **XML이 놓인 폴더 위치와는 무관하다.**

```java
// be/src/main/java/com/financematch/member/mapper/MemberMapper.java
package com.financematch.member.mapper;

public interface MemberMapper {
    Member findByEmail(String email);
}
```

```xml
<!-- be/src/main/resources/mappers/member/MemberMapper.xml -->
<mapper namespace="com.financematch.member.mapper.MemberMapper">
```

폴더는 `mappers/member/`인데 namespace는 `com.financematch.member.mapper.MemberMapper`다. **둘은 다르다.**

`<select id="...">`의 `id`도 인터페이스의 **메서드 이름과 정확히** 같아야 한다.

---

## 3. 예시

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.financematch.member.mapper.MemberMapper">

    <select id="findByEmail" parameterType="string" resultType="Member">
        SELECT id, email, name, investment_type, created_at
          FROM member
         WHERE email = #{email}
    </select>

    <insert id="insert" parameterType="Member" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO member (email, password, name, gender, birth_date)
        VALUES (#{email}, #{password}, #{name}, #{gender}, #{birthDate})
    </insert>

</mapper>
```

### 알아둘 것

- **`resultType="Member"`** — 전체 경로를 안 써도 된다. `RootConfig`에 `typeAliasesPackage("com.financematch")`가 설정돼 있어 클래스 이름만으로 찾는다
- **`snake_case → camelCase` 자동 매핑**이 켜져 있다. `investment_type` 컬럼이 `investmentType` 필드로 알아서 들어간다. `resultMap`을 따로 안 만들어도 된다
- **`#{}`를 쓴다.** `${}`는 SQL 인젝션에 뚫린다. 컬럼명을 동적으로 넣어야 하는 등 불가피할 때만 쓰고, 그때도 값을 검증한다
- `useGeneratedKeys="true" keyProperty="id"` — INSERT 후 생성된 PK를 객체에 채워준다

---

## 4. 자주 나는 오류

### `Invalid bound statement (not found): com.financematch.member.mapper.MemberMapper.findByEmail`

**가장 흔한 오류.** 인터페이스는 찾았는데 대응하는 SQL을 못 찾은 것이다. 순서대로 확인한다.

1. XML이 `resources/mappers/` 아래에 있는가
2. 확장자가 `.xml`인가
3. `namespace`가 **인터페이스 전체 경로**와 글자 하나까지 같은가
4. `<select id="...">`가 **메서드 이름**과 같은가
5. 빌드 결과물에 XML이 들어갔는가 — `be/build/resources/main/mappers/` 확인

### `There is no getter for property named 'xxx' in class ...`

`#{xxx}`에 쓴 이름이 파라미터 객체의 필드와 다르다. 파라미터가 여러 개면 `@Param("xxx")`을 붙인다.

```java
Member findByIdAndCoupleId(@Param("id") Long id, @Param("coupleId") Long coupleId);
```

### 쿼리를 고쳤는데 반영이 안 됨

XML은 리소스라 **다시 빌드해야** 반영된다. IDE에서 톰캣을 재배포하거나 `./gradlew build`를 다시 돌린다.

---

## 5. 소유권 검증

경로에 `{id}`가 있는 API는 **조회 쿼리에 소유 조건을 넣는다.**

```xml
<select id="findByIdAndCoupleId" resultType="RecommendationSlot">
    SELECT s.*
      FROM recommendation_slot s
      JOIN recommendation r ON s.recommendation_id = r.id
     WHERE s.id = #{id}
       AND r.couple_id = #{coupleId}     <!-- 내 커플 것만 -->
</select>
```

**찾은 뒤에 검사하는 방식보다 안전하다.** 검사를 깜빡할 여지가 없고, 남의 것이면 애초에 조회되지 않는다.

---

## 6. 참고

| 문서 | 내용 |
|---|---|
| [`be/README.md`](../../../../README.md) | 백엔드 폴더 구조 · 계층 규칙 |
| [`db/README.md`](../db/README.md) | Flyway 마이그레이션 · 테이블 구조 |

# 백엔드 상자 — 톰캣 9 + 빌드된 WAR.
#
# 【중요】 톰캣 9 여야 한다.
#   이 프로젝트는 javax.servlet(4.0.1) 을 쓴다. 톰캣 10 부터는 jakarta.servlet 으로
#   패키지명이 바뀌어서, 10/11 에 올리면 앱이 기동조차 하지 않는다.
#   "최신 버전이 좋겠지" 하고 태그를 올리지 말 것.
#
# 【빌드는 노트북에서 한다】
#   서버(t3.small)에서 Gradle 빌드를 돌리면 메모리가 부족해 멈출 수 있다.
#   노트북에서 ./gradlew build -Pprod 로 WAR 를 만든 뒤, 그 결과물만 서버로 보낸다.
#   그래서 이 Dockerfile 은 "복사"만 한다 (멀티스테이지 빌드 아님).
#
#   -Pprod 를 붙이는 이유: 데모 시드(db/dev-seed)를 WAR 에 넣지 않기 위해서다.

FROM tomcat:9.0-jdk17-temurin

# 톰캣 기본 예제 앱(manager, examples 등)을 지운다.
# 남겨두면 /manager 같은 경로가 외부에 노출되고, 알려진 취약점의 표적이 된다.
RUN rm -rf /usr/local/tomcat/webapps/*

# WAR 를 ROOT.war 로 넣는다.
#   톰캣은 WAR 파일명을 컨텍스트 경로로 쓴다. finance-match.war 로 두면
#   주소가 /finance-match/api/... 가 되어 프론트가 부르는 /api/... 와 어긋난다.
#   ROOT.war 로 넣어야 /api/... 그대로 맞는다.
COPY build/libs/*.war /usr/local/tomcat/webapps/ROOT.war

ENV TZ=Asia/Seoul

EXPOSE 8080

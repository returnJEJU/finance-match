# 프론트 상자 — nginx + 빌드된 화면 파일.
#
# 【화면 파일은 스스로 못 뜬다】
#   npm run build 가 만드는 dist/ 는 html·js·css 뭉치일 뿐 실행되는 프로그램이 아니다.
#   브라우저에 나눠줄 웹서버가 필요하고, 그 역할을 nginx 가 한다.
#   그래서 "화면 상자"와 "웹서버 상자"가 따로가 아니라 하나다.
#
# 【이 상자가 문지기다】
#   4개 상자 중 외부(80 포트)에 열리는 건 이 상자 하나뿐이다.
#   /api 요청은 nginx 가 백엔드 상자로 넘긴다 (nginx.conf 참고).
#
# 【빌드는 노트북에서】
#   backend 와 같은 이유. npm run build 를 먼저 돌린 뒤 이미지를 만든다.

FROM nginx:1.27-alpine

# 기본 안내 페이지 제거
RUN rm -rf /usr/share/nginx/html/*

COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

ENV TZ=Asia/Seoul

EXPOSE 80

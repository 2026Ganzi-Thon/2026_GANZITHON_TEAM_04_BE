# 상황·고민 입력 및 미루기 API 연결 메모

이 문서는 현재 백엔드 브랜치 `feat/concern-reminders`의 구현 계약이다. 사용자 식별은 익명 토큰이며, 첫 화면 탭 시 최초 한 번 발급한다. 앱을 삭제하거나 기기를 바꾸면 새 사용자로 시작한다.

## 첫 화면부터 카드 단계까지

1. 기기에 토큰이 없다면 `POST /api/anonymous-users`를 호출한다. 응답은 `{ "userId": "...", "token": "..." }`이다. 토큰은 응답에 한 번만 나타나므로 기기 내 보호된 저장소에 보관한다. 토큰이 이미 있으면 재발급하지 않는다.
2. 이후 요청에는 `Authorization: Bearer <token>`을 보낸다. `POST /api/sessions`는 `{ "sessionId": "..." }`를 반환한다.
3. 상황과 초기 마음 무게를 모두 선택한 뒤 `PUT /api/sessions/{sessionId}/concern`에 `{ "situationType": "RELATIONSHIP", "weightBefore": 3 }`을 보낸다. 상황 코드는 `RELATIONSHIP`, `STUDY_OR_WORK`, `WAITING_FOR_RESULT`, `OTHER`이고 무게는 1~5이다.
4. `PUT /api/sessions/{sessionId}/concern/text`에 `{ "rawText": "..." }`를 보낸다. 공백만 입력하거나 사용자에게 보이는 문자 기준 200자를 넘기면 400이다. 응답은 `{ "guidanceRequired": false, "guideType": null }` 형식이며 원문은 응답하지 않는다. 위기 표현이면 `guidanceRequired`가 참이고 원문은 서버 세션에 저장하지 않는다. 이때 카드 단계보다 상담 안내를 먼저 표시한다.
5. 이후 카드 단계에는 `sessionId`를 넘긴다. 현재 카드 생성 기능과 활성 세션 저장소의 연결은 담당 코드에서 추가해야 한다.
6. 중단 시 `POST /api/sessions/{sessionId}/cancel`을 호출한다. 성공하면 204이며 임시 고민 입력이 삭제된다.

## 메인과 미루기 기록

메인 진입·새로고침과 삭제 후에 `GET /api/reminders/status`를 호출한다. 응답은 `{ "count": 0, "showNotice": false }` 형식이다. `showNotice`가 참일 때만 알림을 표시한다.

- `GET /api/reminders`: 본인 미루기 기록을 최신순으로 반환한다.
- `GET /api/reminders/{reminderId}`: 완료 일시·상황·초기 무게·추천/선택 경로·마음 감소 여부를 반환한다. 고민 원문과 생각 카드는 포함하지 않는다.
- `DELETE /api/reminders/{reminderId}`: 해당 미루기 항목만 삭제하고 204를 반환한다. 완료 기록과 걸음은 유지한다.
- `DELETE /api/records/{sessionId}`: 본인 완료 기록을 삭제하고 연결 미루기 항목과 한 걸음을 함께 차감한 뒤 204를 반환한다.

모든 기록 조회·삭제는 익명 토큰으로 소유권을 확인한다. 다른 사용자 기록은 404를 반환한다.

## 세션 완료 담당 연결 지점

`DEFER` 세션이 정상 완료될 때 완료 요약 생성 및 걸음 적립과 **같은 DB 트랜잭션**에서 `ReminderService.createAfterCompletion(userId, sessionId)`를 호출한다. 이 메서드는 사용자 행을 잠그고 6번째 미루기 항목 생성 시 가장 오래된 미루기 항목만 삭제한다. 완료 요약과 걸음은 유지한다. 세션 완료·카드 삭제 처리는 별도 담당 코드와 연결해야 한다.

Flyway 파일 `V1__create_concern_and_reminder_tables.sql`은 현재 `main`에 마이그레이션 파일이 없는 것을 확인하고 작성했다. 병렬 브랜치의 Flyway 번호와 테이블을 머지 전에 다시 비교한다.

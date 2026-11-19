# 말랑 API 연결

기본 경로는 `/api/v1`이다. 성공 응답은 `{ "success": true, "data": ... }` 형태이며, 데이터가 없는 작업은 `data: null`을 반환한다. 실패 응답에는 HTTP 상태와 `error.code`, `error.message`가 포함된다.

## 사용자와 진행 세션

1. 토큰이 없다면 `POST /users`로 `{ userId, token }`을 발급받는다. 이후 요청에는 `Authorization: Bearer <token>`을 보낸다.
2. `POST /sessions`로 숫자 `sessionId`와 `IN_PROGRESS` 상태를 받는다. 해당 사용자의 이전 진행 세션이 있으면 중단 처리된다.
3. `PUT /sessions/{sessionId}/concern`에는 `{ situationType, initialWeight }`를 보낸다. 상황 코드는 `RELATIONSHIP`, `STUDY_WORK`, `WAITING`, `OTHER`이고 무게는 1~5이다.
4. `PUT /sessions/{sessionId}/concern/text`에는 `{ content }`를 보낸다. 공백 포함 최대 200글자이며 공백만 입력할 수 없다. 위기 감지 시 `SAFETY_GUIDANCE_REQUIRED`를 반환하고 원문을 저장하지 않는다.
5. 중단 시 `DELETE /sessions/{sessionId}`를 호출한다. 같은 탭에서 새로고침하면 기존 세션 ID로 이어간다. 마지막 활동 후 24시간이 지나면 세션과 카드가 삭제된다.

## 생각 카드와 경로

이하 `/concerns/{id}`의 `id`는 발급받은 `sessionId`다.

| 메서드 | 경로 | 요청 및 응답 |
| --- | --- | --- |
| `POST` | `/concerns/{id}/cards/generate` | `{ cards, generatedBy, truncated }` 반환. 기존 카드가 있으면 `CARDS_ALREADY_EXIST` 반환 |
| `GET` | `/concerns/{id}/cards` | `{ cards }` 반환 |
| `POST` | `/concerns/{id}/cards` | `{ content }`를 받아 `{ card }` 반환 |
| `PATCH` | `/cards/{cardId}` | `{ content }`를 받아 `{ card, coreSelectionCleared }` 반환 |
| `DELETE` | `/cards/{cardId}` | 카드 삭제. 마지막 카드는 삭제 불가 |
| `PUT` | `/concerns/{id}/cards/classification` | `{ items: [{ cardId, category }] }`를 받아 `{ cards }` 반환 |
| `POST` | `/concerns/{id}/path/recommendation` | `{ coreCardId, canAct, needsTime }`를 받아 `{ recommendedPath, options }` 반환 |
| `PUT` | `/concerns/{id}/path/selection` | `{ selectedPath }`를 저장하고 반환 |
| `POST` | `/sessions/{id}/action-recommendations` | 행동하기 선택 시 `{ actions, generatedBy }` 반환 |
| `GET` | `/path/actions?path=SWITCH` | 중복 없이 전환 활동 3개 반환 |

## 완료, 마이페이지와 미루기

- `POST /concerns/{id}/completion`에 `{ moodReduced }`를 보낸다. 완료 요약 저장, 걸음 1회 적립, 미루기 선택 시 항목 생성, 진행 세션·카드 삭제를 한 트랜잭션에서 처리한다.
- `GET /me/summary`는 걸음 수와 성장 단계, `GET /reflections`는 완료 기록 목록, `GET /reflections/{reflectionId}`는 완료 요약을 반환한다. `DELETE /reflections/{reflectionId}`는 해당 기록과 연결 미루기를 삭제하고 걸음을 1회 차감한다.
- `GET /reminders/status`는 `{ count, showNotification }`, `GET /reminders`는 `{ items }`를 반환한다. `DELETE /reminders/{reminderId}`는 미루기 항목을 삭제한다.

## 설정

Swagger에서 `POST /users`로 발급한 토큰을 **Authorize**에 입력하면 인증이 필요한 경로를 호출할 수 있다. Upstage를 사용하려면 `UPSTAGE_BASE_URL`, `UPSTAGE_API_KEY`, `UPSTAGE_MODEL`을 로컬 `.env` 또는 배포용 GitHub Actions 시크릿에 설정한다. 값이 없거나 AI 요청이 실패하면 기본 분리 및 행동 제안을 사용한다.

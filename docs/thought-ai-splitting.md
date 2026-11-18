# 생각 카드 AI 분리 연결

## 호출 순서

1. 첫 화면 탭 시 익명 토큰을 발급하고, 세션을 생성한다. 자세한 순서는 `docs/concern-reminders-api.md`를 따른다.
2. 상황과 무게를 선택하고 고민을 입력한다. `PUT /api/v1/sessions/{sessionId}/concern/text`의 `guidanceRequired`가 `true`이면 상담 안내를 먼저 표시한다.
3. 정상 입력일 때 `POST /api/v1/sessions/{sessionId}/thought-cards`를 호출한다. `Authorization: Bearer <익명 토큰>`이 필요하며 요청 본문은 없다. 서버는 해당 사용자의 활성 세션에 저장한 고민을 읽는다.
4. 응답 예시: `{"cards":[{"id":"UUID","order":1,"text":"생각"}],"generationMethod":"ai","truncated":false}`. `generationMethod`는 `ai` 또는 `fallback`이다. `truncated=true`면 “생각이 많아 앞의 8장만 보여줘요. 빠진 내용이 없는지 확인하고 카드를 수정해 주세요.” 팝업을 띄운다.
5. 사용자가 카드를 확인하고 편집한다. 생성 응답에는 고민 원문이 포함되지 않으며 카드는 서버에 저장하지 않는다. 세션 취소 시 기존 취소 API가 원문을 삭제한다.

다른 사용자의 세션과 원문이 없는 세션은 404, 토큰 누락·오류는 401을 반환한다. 응답에는 `Cache-Control: no-store`를 설정한다.

## Upstage 설정

- `UPSTAGE_API_KEY`: 서버 전용 키. Git이나 문서에 실제 값을 넣지 않는다. 로컬 `.env` 또는 IntelliJ 실행 환경변수에 등록한다.
- `UPSTAGE_MODEL`: `solar-mini4`로 설정한다. Docker Compose에는 이 값이 기본값으로 지정되어 있다.
- `UPSTAGE_BASE_URL`: 기본값 `https://api.upstage.ai/v1`.

`ThoughtCardService`는 PR #17의 `AiCardGenerator`를 호출한다. 키 또는 모델 설정이 없거나 호출·결과 검증이 실패하면 문장부호와 줄바꿈으로 원문을 나눈다. 모델 호출은 최대 두 번 시도한다. 서비스는 반환된 카드의 빈 내용·중복·200자 초과를 검사하고, 9장 이상이면 앞의 8장만 반환한다. 모델 출력의 의미 보존은 서버 형식 검증만으로 보장되지 않으므로 사용자가 결과를 확인해야 한다.

현재 PR #17 호출기는 줄 단위 응답을 사용하고 프롬프트에 최대 6장을 지시한다. PRD의 JSON 응답 및 6~8장 목표·8장 초과 감지 기준은 PR #17 담당자와 별도로 맞춰야 한다. 이 통합 브랜치에서는 PR #17 코드를 수정하지 않는다.

프론트엔드에서는 외부 AI로 고민을 전송한다는 사실과 처리 범위를 입력 전에 고지해야 한다. 제공자의 데이터 처리 설정은 운영 전 검토한다. CD 배포 전 GitHub Actions 저장소 Secret `UPSTAGE_API_KEY`를 등록해야 한다. CD는 값이 없으면 배포를 중단한다. 실제 키는 `.env.example`에 넣지 않는다.

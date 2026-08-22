# 머물;경(慶) — Backend

경상북도 15개 인구감소지역 대상 **체류형 여행 추천 서비스** "머물;경"의 백엔드 레포지토리입니다.


---

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.x |
| Build | Gradle |
| Database | PostgreSQL (Docker Compose) |
| External API | 한국관광공사 TourAPI (실시간 호출) |

> ⚠️ **TourAPI 응답은 로컬 DB에 캐싱/저장하지 않고 실시간 호출**합니다. (공모전 규정)
> 출처 표기는 `출처: ⓒ한국관광공사` 텍스트 형태로만 사용합니다.

---

## 로컬 실행

DB는 Docker로 띄우고, 애플리케이션은 IDE에서 실행합니다.

```bash
# 1. PostgreSQL 기동 (DB만 도커로)
docker compose up -d

# 2. 애플리케이션 실행
#    IntelliJ에서 MeomulKyungApplication 실행
```

---

## 코드 관리 전략 (Git Flow)

브랜치 관리는 **Git Flow** 방식을 따릅니다.

### 브랜치 구조

| 브랜치 | 역할 | 분기 원본 | 병합 대상 |
| --- | --- | --- | --- |
| `main` | 제출/배포 가능한 안정 버전 | — | — |
| `develop` | 기능이 모이는 개발 통합 브랜치 | `main` | `main` (release 경유) |
| `feature/*` | 기능 단위 개발 | `develop` | `develop` |
| `release/*` | 배포/제출 직전 안정화 (버그픽스·문서 정리) | `develop` | `main` + `develop` |
| `hotfix/*` | 배포/제출 후 긴급 수정 | `main` | `main` + `develop` |

```
main     ●───────────────────────●────────●   (제출 가능 버전 / tag)
          \                      / \      /
develop    ●──●──●──●──●──●──●──●   ●────●     (개발 통합)
            \    /   \      /        (hotfix)
feature      ●──●     ●──●●
```

### 브랜치 네이밍

`feature/{이슈번호}-{간단한-설명}` 형식을 사용합니다.

```
feature/23-recommend-weighting     # 지역 추천 가중치 매칭
feature/31-itinerary-generation    # 체류형 일정 자동 생성
release/1.0.0                      # 1차 심사 제출 버전
hotfix/45-tourapi-timeout          # 배포 후 긴급 수정
```

### 작업 흐름

1. `develop`에서 `feature/*` 브랜치를 딴다.
2. 기능 작업 후 `develop`으로 **PR**을 올린다.
3. **최소 1인 리뷰 승인** 후 병합한다. (`main` 직접 push 금지)
4. 스프린트 종료 / 제출 시점에 `develop` → `release/*` → `main` 으로 정리하고 **tag**를 남긴다.
5. 제출 후 버그 발견 시 `hotfix/*`로 대응한다.

---

## 커밋 컨벤션

[Conventional Commits](https://www.conventionalcommits.org/) 형식을 따릅니다.

```
<type>: <설명>

feat: 지역 추천 가중치 매칭 로직 구현
fix: TourAPI 응답 null 처리 예외 수정
docs: README 브랜치 전략 추가
```

| type | 용도 |
| --- | --- |
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `refactor` | 리팩터링 (기능 변화 없음) |
| `test` | 테스트 코드 |
| `chore` | 빌드/설정/패키지 등 잡무 |
| `style` | 포맷팅, 세미콜론 등 (로직 변화 없음) |

---

## PR 규칙

- **대상 브랜치:** `develop` (기능), `main` (release/hotfix)
- **제목 예시:** `[FEAT] 지역 추천 알고리즘 구현`
- **리뷰어:** 최소 1인 승인 필수
- **병합 방식:** Squash Merge 권장 (`develop` 히스토리 정리)
- `main` 브랜치 직접 push 금지

---

> **📌 팀 참고 (4인 / 6주 스프린트 기준)**
> 정식 Git Flow의 `release`·`hotfix`는 실제로는 **제출(9/21) 전후에만** 필요합니다.
> 평상시에는 `feature → develop` 흐름만 돌려도 충분하며, `main` 병합은 스프린트 마감 또는 제출 시점에 몰아서 진행하는 걸 권장합니다.

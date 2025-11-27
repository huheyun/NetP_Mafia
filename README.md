# NetP_Mafia

## 1. 프로젝트 개요

Java TCP/IP 소켓과 Swing GUI를 활용한 5인 멀티플레이어 마피아 게임입니다. 서버가 직업을 배정하고 낮/밤 페이즈를 관리하며, 클라이언트는 GUI를 통해 채팅·투표·특수행동(마피아 살해 / 경찰 조사 / 의사 치료)을 수행합니다.

### 기반 오픈소스

- 원본 프로젝트: [Mafia_Game](https://github.com/moonhy7/Mafia_Game) by moonhy7

### 주요 개선 및 확장 사항

본 프로젝트는 원본 오픈소스를 기반으로 다음과 같은 기능을 개선 및 확장했습니다:

#### 1. **UI/UX 대폭 개선**

- ✅ **역할별 아이콘 시스템**: 마피아/경찰/의사/시민 각 역할에 맞는 이미지 아이콘 추가
- ✅ **버튼 아이콘 적용**: 투표/살해/조사/치료 버튼에 직관적인 아이콘 추가
- ✅ **역할 아이콘 캐싱**: HashMap을 통한 이미지 재사용으로 메모리 효율성 향상
- ✅ **배경색 구분**: 정보 라벨(플레이어/역할/페이즈/타이머/생존자)별 색상 구분으로 가독성 향상
- ✅ **역할별 색상 코딩**: 마피아(빨강), 경찰(파랑), 의사(초록), 시민(회색) 라벨 색상 자동 변경

#### 2. **게임 플레이 개선**

- ✅ **밤 능력 1회 제한**: 각 역할이 밤마다 능력을 1회만 사용하도록 `nightActionUsed` 플래그 추가
- ✅ **시민 밤 채팅 제한**: 밤 시간에 시민의 채팅 자동 비활성화로 게임 밸런스 강화
- ✅ **버튼 기반 명령 시스템**: 명령어 직접 입력 대신 버튼 UI로 사용성 개선
- ✅ **관찰자 모드 안내**: 사망 시 명확한 관찰자 모드 전환 메시지

#### 3. **오디오 시스템 추가**

- ✅ **BGM 플레이어**: 낮/밤 배경음악 자동 재생 (`BGMPlayer.java`)
  - `morning.wav`: 낮 페이즈 BGM
  - `night.wav`: 밤 페이즈 BGM
  - 무한 루프 재생 및 페이즈 전환 시 자동 교체
- ✅ **효과음 플레이어**: 게임 이벤트별 효과음 (`SFXPlayer.java`)
  - `pistol-shot.wav`: 플레이어 사망 시 효과음
- ✅ **리소스 관리 최적화**: try-with-resources를 통한 오디오 스트림 자동 해제

#### 4. **코드 품질 및 최적화**

- ✅ **메모리 누수 방지**: 오디오 리소스 자동 해제 구조 개선
- ✅ **코드 중복 제거**: 서버 메시지 전송 로직 `sendToAllClients()` 헬퍼 메서드로 통합
- ✅ **동기화 최적화**: 중복된 동기화 블록 4개 → 1개로 통합
- ✅ **이미지 캐싱**: 동일 아이콘 반복 로드 방지

#### 5. **실시간 게임 상태 동기화**

- ✅ **타이머 업데이트**: 1초마다 클라이언트에 남은 시간 전송 (`TIMER_UPDATE`)
- ✅ **플레이어 목록 동기화**: 생존 상태 및 투표 수 실시간 업데이트 (`PLAYER_LIST`)
- ✅ **게임 상태 브로드캐스트**: 페이즈/시간/생존자 수 실시간 전송 (`GAME_STATUS`)
- ✅ **UI 자동 갱신**: `refreshPlayerAndRoleInfo()` 메서드로 창 크기 변경 시에도 정보 유지

#### 6. **디버깅 및 개발 지원**

- ✅ **미리보기 모드**: 서버 연결 없이 UI만 테스트 가능한 프리뷰 모드 추가
- ✅ **상세 로깅**: 역할 배정, 명령 수신, UI 업데이트 등 주요 이벤트 콘솔 로그 출력
- ✅ **명확한 에러 메시지**: 사용자에게 친화적인 에러 안내

#### 7. **리소스 구조 정리**

```
NetP_Mafia/
├── src/mafia_game/sounds/    # 게임 사운드 파일
│   ├── morning.wav            # 낮 페이즈 BGM
│   ├── night.wav              # 밤 페이즈 BGM
│   ├── pistol-shot.wav        # 사망 효과음
│   ├── blade.wav              # 마피아 공격 효과음
│   ├── police.wav             # 경찰 조사 효과음
│   ├── heal-sound.wav         # 의사 치료 효과음
│   ├── button.wav             # 버튼 클릭 효과음
│   ├── reloading.wav          # 게임 준비 효과음
│   └── siren.wav              # 긴급 알림 효과음
└── resources/images/
    ├── roles/                 # 역할 아이콘 (80x80)
    │   ├── mafia.png
    │   ├── police.png
    │   ├── doctor.png
    │   └── citizen.png
    ├── icon/                  # 버튼 아이콘 (20x20)
    │   ├── vote.png
    │   ├── kill.png
    │   ├── check.png
    │   └── heal.png
    └── mafia_bg.png           # 게임 배경 이미지
```

### 원본 대비 개선 통계

- **추가된 Java 파일**: 3개 (`BGMPlayer.java`, `SFXPlayer.java`, `MafiaGameGUIPreview.java`)
- **추가된 메서드**: 약 15개 (UI 갱신, 오디오, 캐싱 등)
- **코드 최적화**: 약 30라인 중복 코드 제거
- **리소스 파일**:
  - 이미지: 9개 (역할 4개 + 버튼 4개 + 배경 1개)
  - 오디오: 9개 (BGM 2개 + 효과음 7개)
- **메모리 효율**: 이미지 캐싱으로 반복 로드 제거
- **사용자 경험**: 버튼 기반 UI로 명령어 학습 불필요

```
NetP_Mafia/
├── src/
│   └── mafia_game/
│       ├── TcpApplication.java       # 설정 로드/공통 추상 클래스 (IP, PORT, timeStamp)
│       ├── AppServer.java            # 서버 소켓 생성 및 접속 스레드 생성 루프
│       ├── TcpServer.java            # 서버 진입 메뉴(콘솔) + AppServer 구동 엔트리
│       ├── TcpServerHandler.java     # 게임 상태/직업 배정/투표/밤 행동/채팅 처리
│       ├── MafiaGameClientMain.java  # 클라이언트 GUI 실행 엔트리
│       ├── MafiaGameGUI.java         # Swing 기반 클라이언트(채팅/투표/밤 행동/표/타이머)
│       ├── MafiaGameGUIPreview.java  # GUI 미리보기 전용 실행 파일
│       ├── BGMPlayer.java            # 배경음악 재생 관리 (낮/밤 BGM 무한 루프)
│       ├── SFXPlayer.java            # 효과음 재생 관리 (이벤트별 사운드)
│       └── sounds/                   # 게임 사운드 파일
│           ├── morning.wav           # 낮 페이즈 BGM
│           ├── night.wav             # 밤 페이즈 BGM
│           ├── pistol-shot.wav       # 사망 효과음
│           ├── blade.wav             # 마피아 공격 효과음
│           ├── police.wav            # 경찰 조사 효과음
│           ├── heal-sound.wav        # 의사 치료 효과음
│           ├── button.wav            # 버튼 클릭 효과음
│           ├── reloading.wav         # 게임 준비 효과음
│           └── siren.wav             # 긴급 알림 효과음
├── resources/
│   └── images/
│       ├── roles/                    # 역할 아이콘 (80x80)
│       │   ├── mafia.png
│       │   ├── police.png
│       │   ├── doctor.png
│       │   └── citizen.png
│       ├── icon/                     # 버튼 아이콘 (20x20)
│       │   ├── vote.png
│       │   ├── kill.png
│       │   ├── check.png
│       │   └── heal.png
│       └── mafia_bg.png              # 게임 배경 이미지
├── config/
│   └── applicationcontext.ini        # 서버 설정(IP, PORT)
├── bin/                              # 컴파일 산출물 (.gitignore로 관리)
├── .gitignore                        # Git 제외 파일 설정
├── .vscode/                          # VS Code 설정
└── README.md
```

## 3. 주요 구성 요소 역할

| 구성                | 설명                                                                                                             |
| ------------------- | ---------------------------------------------------------------------------------------------------------------- |
| TcpApplication      | 설정 파일(`applicationcontext.ini`) 로드 및 공통 유틸 제공                                                       |
| AppServer           | 지정된 PORT로 `ServerSocket` 열고 클라이언트 연결을 `TcpServerHandler` 스레드로 분기                             |
| TcpServer           | 콘솔 메뉴(입장/퇴장) 후 서버 초기화 및 시작 호출(학습용 래퍼)                                                    |
| TcpServerHandler    | 게임 전체 상태(static): 직업 배정, 낮/밤 페이즈, 투표/처형, 마피아/경찰/의사 밤 행동, 생존 관리, 승리 조건 판단  |
| MafiaGameClientMain | GUI 초기화 엔트리 (현재 클라이언트는 서버 설정을 하드코딩 localhost 사용)                                        |
| MafiaGameGUI        | 채팅, 타이머 수신(`TIMER_UPDATE`), 플레이어 목록(`PLAYER_LIST`), 게임 상태(`GAME_STATUS`) 표시 및 행동 명령 전송 |
| MafiaGameGUIPreview | 서버 연결 없이 GUI만 테스트할 수 있는 미리보기 모드 실행 파일                                                    |
| BGMPlayer           | 낮/밤 배경음악 재생 및 페이즈 전환 시 자동 교체, 무한 루프 재생 관리                                             |
| SFXPlayer           | 게임 이벤트별 효과음 재생 (사망, 공격, 조사, 치료 등), try-with-resources로 리소스 관리                          |

## 4. 실행 방법

### 4.1 컴파일

```bash
javac -encoding UTF-8 -d bin -cp src src/mafia_game/*.java
```

### 4.2 서버 실행 (콘솔 메뉴 → 1 입력 후 시작)

```bash
java -cp bin mafia_game.TcpServer
```

### 4.3 클라이언트 실행 (각 사용자 PC/탭에서 반복)

```bash
java -cp bin mafia_game.MafiaGameClientMain
```

> 5명이 접속하면 자동으로 직업 배정 후 게임이 시작됩니다.

## 5. 게임 진행 흐름

1. 대기: 플레이어 이름 입력 후 5명 모이면 시작.
2. 직업 배정: [마피아 1 / 경찰 1 / 의사 1 / 시민 2] 무작위 배정 → 개별 귓속말(`/to`)로 역할 통지.
3. 낮(기본 30초): 토론 및 `/vote 대상` 명령을 통한 투표. 최다 득표자 처형 (동점 시 무효).
4. 밤(기본 15초):
   - 마피아: `/kill 대상`
   - 경찰: `/check 대상` → 마피아 여부 정보
   - 의사: `/heal 대상` → 마피아 타겟과 동일 시 생존 유지
5. 사망자 처리: 죽은 플레이어는 관찰자 모드(채팅만 가능, 투표/행동 불가).
6. 승리 조건:
   - 시민팀 승리: 모든 마피아 제거
   - 마피아팀 승리: 마피아 수 >= 생존 시민 수

## 6. 클라이언트 명령어 요약

| 명령                | 용도                         |
| ------------------- | ---------------------------- |
| 일반 채팅           | 그냥 문자열 입력             |
| `/to 닉네임 메시지` | 귓속말                       |
| `/vote 닉네임`      | 낮 투표                      |
| `/kill 닉네임`      | 밤 마피아 살해 (마피아 전용) |
| `/check 닉네임`     | 밤 조사 (경찰 전용)          |
| `/heal 닉네임`      | 밤 치료 (의사 전용)          |

## 7. 설정 파일 (`config/applicationcontext.ini`)

```ini
IP = localhost
PORT = 5592
```

서버는 `TcpApplication.init()` 단계에서 위 값을 로드합니다. 현재 클라이언트(`MafiaGameClientMain`)는 별도 설정 파싱 없이 `localhost:5592` 하드코딩이므로 추후 설정 동기화 개선이 필요합니다.

## 8. 현재 코드 상태 & 개선 필요 항목

### 구현됨

- 직업 배정(중복 없는 랜덤 순서)
- 낮/밤 페이즈 전환 및 중앙 타이머
- 투표 집계(동점 처리, 처형, 승리 조건 판단)
- 밤 행동(마피아 살해 / 경찰 조사 / 의사 치료) 및 치료 성공/실패 처리
- GUI: 플레이어 테이블/페이즈/타이머/역할 색상/행동 버튼 동적 활성화
- Whisper(`/to`) 및 일반 채팅 브로드캐스트
- 역할별 아이콘 시스템 및 이미지 캐싱
- BGM/SFX 오디오 시스템 (9개 사운드 파일)
- 미리보기 모드 (`MafiaGameGUIPreview.java`)
- 실시간 게임 상태 동기화 (타이머, 플레이어 목록, 게임 상태)
- 메모리 누수 방지 및 코드 최적화

### 발견된 개선 포인트

| 항목                          | 설명                                             | 제안/상태                          |
| ----------------------------- | ------------------------------------------------ | ---------------------------------- |
| 하드코딩 클라이언트 서버 정보 | 클라이언트가 설정 파일을 읽지 않음               | 설정 파일 공유 또는 인자 전달 지원 |
| 스레드 관리                   | 클라이언트마다 new Thread 생성                   | ExecutorService 기반 풀 적용 검토  |
| 메시지 프로토콜 혼합          | 채팅/시스템/타이머/목록이 문자열 파싱 혼합       | 명확한 prefix + JSON 구조 표준화   |
| 사용되지 않는 필드            | `TcpServerHandler.cAddr` 선언 후 미사용          | 제거하여 가독성 향상               |
| 종료 처리 부족                | 서버 정상 종료 로직 없음                         | 종료 명령/자원 정리 추가           |
| bin 폴더 추적                 | 컴파일 산출물 Git 관리 위험                      | ✅ `.gitignore` 추가 완료          |
| 예외 처리                     | 일부 InterruptedException, IOException 단순 무시 | 로깅/유저 안내 메시지 강화         |
| 사운드 파일 경로              | sounds 폴더가 src 내부에 위치                    | resources로 이동 검토              |

## 9. 향후 개발 계획 (로드맵)

- (단기)
  - 사운드 파일 경로 resources로 통합
  - 추가 효과음 활용 (blade, police, heal-sound 등)
  - 클라이언트 설정 파싱 / 미사용 코드 정리
- (중기)
  - 상태/이벤트 메시지 JSON 표준화
  - 밤 행동 UI 개선(선택 다이얼로그)
  - 재접속 처리
  - 스레드풀 전환 실험
- (중장기)
  - 애니메이션(역할 배정/처형)
  - 리플레이 로그 저장
  - 확장 직업(연구자 등)
  - 멀티룸 지원

## 10. 개발/운영 권장 사항

1. `.gitignore`에 `bin/` 및 OS 특수 파일 추가 (`bin/`, `*.log`, `.DS_Store` 등)
2. 서버/클라이언트 모두 SLF4J 같은 경량 로거 도입 고려
3. 메시지 스냅샷/리플레이를 위한 로그 버퍼 구조 초안 작성
4. 역할/행동 검증을 위한 JUnit 단위 테스트(직업 배정·승리 조건·투표 동점) 설계 권장

## 11. 기술 스택

- 언어: Java
- 네트워킹: TCP/IP Socket
- GUI: Swing
- 동시성: Thread (향후 ExecutorService 검토)

---

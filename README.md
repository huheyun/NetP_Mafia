# NetP_Mafia

## 프로젝트 개요

네트워킹을 활용한 멀티플레이어 마피아 게임 프로젝트입니다.

## 프로젝트 구조

```
NetP_Mafia/
├── src/
│   └── mafia_game/
│       ├── TcpApplication.java       # 추상 TCP 애플리케이션 클래스
│       ├── TcpServer.java           # 서버 메인 클래스
│       ├── AppServer.java           # 실제 서버 구현
│       ├── TcpServerHandler.java    # 클라이언트 연결 처리
│       ├── ClientGui.java           # 클라이언트 GUI
│       ├── ClientGuiMain.java       # 클라이언트 메인
│       ├── Timer2.java              # 게임 타이머
│       └── voting.java              # 투표 시스템
├── config/
│   └── applicationcontext.ini       # 서버 설정 파일
├── bin/
│   └── mafia_game/                  # 컴파일된 클래스 파일
└── README.md
```

## 게임 특징

- **멀티플레이어**: 최대 5명까지 동시 플레이
- **실시간 채팅**: 플레이어 간 실시간 소통
- **직업 시스템**: 마피아, 경찰, 의사, 시민 역할
- **투표 시스템**: GUI 기반 투표 인터페이스
- **타이머**: 게임 진행 시간 관리

## 실행 방법

### 1. 컴파일

```bash
# 프로젝트 디렉토리에서
javac -encoding UTF-8 -d bin -cp src src/mafia_game/*.java
```

### 2. 서버 실행

```bash
java -cp bin mafia_game.TcpServer
```

### 3. 클라이언트 실행

```bash
java -cp bin mafia_game.MafiaGameClientMain
```

## 설정

`config/applicationcontext.ini` 파일에서 서버 IP와 포트를 설정할 수 있습니다:

```ini
IP = localhost
PORT = 5592
```

## 게임 규칙

1. 5명의 플레이어가 접속하면 게임이 시작됩니다
2. 각 플레이어에게 랜덤으로 직업이 배정됩니다:
   - 마피아 1명
   - 경찰 1명
   - 의사 1명
   - 시민 2명
3. 채팅을 통해 소통하며 투표로 마피아를 찾아냅니다
4. 귓속말 기능: `/to [닉네임] [메시지]`

## 개선 계획

- [ ] 게임 로직 완성
- [ ] 밤/낮 사이클 구현
- [ ] 마피아 특수 능력 추가
- [ ] 경찰 수사 기능
- [ ] 의사 치료 기능
- [ ] 승리 조건 구현
- [ ] UI/UX 개선
- [ ] 사운드 효과 추가

## 기술 스택

- **언어**: Java
- **네트워킹**: TCP/IP Socket
- **GUI**: Java Swing
- **멀티스레딩**: Java Thread

package mafia_game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TcpServerHandler implements Runnable {

    // 총 인원 제한 수
    static final int NUMBER = 5;

    /*
     * 클라이언트 ID를 키(K)로 하는 값(V)을 갖는 맵 자료구조
     * 
     */
    static HashMap<String, PrintWriter> sendMap = new HashMap<>();

    static HashMap<String, String> userMap = new HashMap<String, String>();

    // 게임 상태 관리
    static boolean gameStarted = false;
    static boolean isDay = true; // true: 낮, false: 밤
    static boolean votingTime = false;
    static HashMap<String, String> playerJobs = new HashMap<>(); // 플레이어 직업 저장
    static HashMap<String, String> votes = new HashMap<>(); // 투표 저장
    static java.util.Timer gameTimer; // 게임 타이머
    static int remainingTime = 30; // 남은 시간
    static HashMap<String, Boolean> playerAlive = new HashMap<>(); // 플레이어 생존 상태
    static int dayCount = 1; // 현재 날짜
    static HashMap<String, String> nightActions = new HashMap<>(); // 밤 행동 저장 (플레이어ID -> 타겟ID)
    static String mafiaTarget = null; // 마피아가 선택한 타겟

    // 클라이언트와 연결된 소켓 객체
    private Socket sock;

    // 클라이언트 IP 주소
    private String cAddr;

    // 클라이언트 ID
    private String id;

    /*
     * 생성자
     * 받아 온 소켓을 멤버 변수로 저장
     */

    public TcpServerHandler(Socket socket) {
        sock = socket;
    }

    @Override
    public void run() {

        try {
            // 1. 송신 스트림 생성
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(sock.getOutputStream()));

            // 2. 수신 스트림 생성
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            sock.getInputStream()));

            // 클라이언트 접속정보 읽기
            id = br.readLine();

            // 접속 맵에 사용자 추가
            sendMap.put(id, pw);

            System.out.println(TcpApplication.timeStamp() + id + " <- connected");
            System.out.println(TcpApplication.timeStamp() + "접속인원: " + sendMap.size() + "명");

            // 5명이 되면 게임 시작
            if (sendMap.size() == NUMBER && !gameStarted) {
                initializeGame();
            }

            // 메시지 처리 루프
            String line = null;
            while ((line = br.readLine()) != null) {

                // 사망자 채팅 제한
                if (!playerAlive.getOrDefault(id, true)) {
                    PrintWriter deadPw = sendMap.get(id);
                    if (deadPw != null) {
                        deadPw.println("관찰자 모드입니다. 채팅을 보낼 수 없습니다.");
                        deadPw.flush();
                    }
                    continue;
                }

                boolean isCitizen = "시민".equals(playerJobs.get(id));
                boolean isAbilityCommand = line.startsWith("/kill") || line.startsWith("/check")
                        || line.startsWith("/heal") || line.startsWith("/vote") || line.startsWith("/to");

                // 밤 시간 시민 채팅 제한 (능력/귓속말 제외)
                if (gameStarted && !isDay && isCitizen && !isAbilityCommand) {
                    PrintWriter nightPw = sendMap.get(id);
                    if (nightPw != null) {
                        nightPw.println("밤에는 시민은 채팅할 수 없습니다.");
                        nightPw.flush();
                    }
                    continue;
                }

                if (gameStarted) {
                    if (line.startsWith("/vote ") && votingTime) {
                        handleVote(id, line);
                        sendPlayerList();
                    } else if (line.startsWith("/kill ") && !isDay && "마피아".equals(playerJobs.get(id))
                            && playerAlive.getOrDefault(id, false)) {
                        handleMafiaKill(id, line);
                    } else if (line.startsWith("/check ") && !isDay && "경찰".equals(playerJobs.get(id))
                            && playerAlive.getOrDefault(id, false)) {
                        handlePoliceCheck(id, line);
                    } else if (line.startsWith("/heal ") && !isDay && "의사".equals(playerJobs.get(id))
                            && playerAlive.getOrDefault(id, false)) {
                        handleDoctorHeal(id, line);
                    } else if (line.indexOf("/to") > -1) {
                        whisper(id, line);
                    } else {
                        String msg = "[" + id + "] " + line;
                        TcpServerHandler.broadCast(msg);
                    }
                } else {
                    String msg = "[" + id + "] " + line;
                    TcpServerHandler.broadCast(msg);
                }
            }

            // 맵 제거
            // TcpServerHandler.sendMap.remove(id);
            // System.out.println(TcpApplication.timeStamp()+
            // "접속인원: " + sendMap.size() + "명");

            pw.close();
            br.close();
            // sock.close();

            // voting();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * 귓속말 전송 메서드
     * name : 보내는 클라이언트 id
     * msg : 받을 메시지 (/to ID msg )
     * 
     */
    private void whisper(String name, String msg) {
        int start = msg.indexOf(" ") + 1; // 공백 위치의 첫 번째에 +1
        int end = msg.indexOf(" ", start); // start 위치부터 공백 문자를 찾아서 위치

        if (end != -1) {
            // id : 받을 클라이언트 id
            String id = msg.substring(start, end);
            String secret = msg.substring(end + 1);

            // sendMap으로부터 키<id>에 해당하는 PrintWriter 객체를 가져온다.
            PrintWriter pw = TcpServerHandler.sendMap.get(id);
            // 귓속 메시지 전송
            if (pw != null) {
                pw.println(name + "님의 귓속말 : " + secret);
                pw.flush();
            }
        }

    }

    /*
     * 메시지 일괄 전송 메서드
     * : 모든 사용자에게 일괄적으로 전송
     */
    public static void broadCast(String message) {
        // sendMap은 여러 쓰레드가 공유하므로 동기화(synchronized) 처리 필요
        synchronized (sendMap) {

            // 접속한 모든 클라이언트들에게 메시지 전송
            for (PrintWriter cpw : TcpServerHandler.sendMap.values()) {
                cpw.println(message);
                cpw.flush();
            }
        }

    }

    public static String timeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("[hh:mm:ss]");
        return sdf.format(new Date());
    }

    // 접속인원 직업을 알려주는 메서드
    public void notice(String msg) {
        int start = msg.indexOf(" ") + 1;
        int end = msg.indexOf(" ", start);

        if (end != -1) {
            String id = msg.substring(start, end);
            String secret = msg.substring(end + 1);

            PrintWriter pw = TcpServerHandler.sendMap.get(id);

            if (pw != null) {
                pw.println("당신의 직업은 " + secret + "입니다.");
                pw.flush();
            }
        }
    }

    // 게임 초기화 메서드
    private void initializeGame() {
        broadCast("=== 직업 배정 중 ===");

        /* 1. 직업 리스트 설정 */
        List<String> jobList = Arrays.asList("마피아", "경찰", "의사", "시민", "시민");
        List<String> playerList = new ArrayList<>();

        Set<Map.Entry<String, PrintWriter>> set = sendMap.entrySet();
        Iterator<Map.Entry<String, PrintWriter>> it = set.iterator();

        while (it.hasNext()) {
            Map.Entry<String, PrintWriter> entry = it.next();
            String playerId = entry.getKey();
            playerList.add(playerId);
        }

        broadCast("직업 리스트 : " + jobList);
        broadCast("플레이어 리스트 : " + playerList);
        broadCast(" ");

        // 플레이어 인덱스 매핑 및 생존 상태 초기화
        for (int i = 0; i < playerList.size(); i++) {
            userMap.put("" + i, playerList.get(i));
            playerAlive.put(playerList.get(i), true); // 모든 플레이어 생존 상태로 초기화
        }

        /* 2. 0 ~ 4 사이 랜덤 배열 생성 (중복X) */
        int randomList[] = new int[5];
        int index = 0;
        while (index != 5) {
            // random() 이용하여 5개 랜덤 번호 (중복X)
            randomList[index] = (int) (Math.random() * 5);
            for (int i = 0; i < index; i++) {
                if (randomList[i] == randomList[index]) {
                    index--;
                    break; // 중복 제거 코드
                }
            }
            index++;
        }

        /* 3. 플레이어 순서 랜덤 재배치 */
        List<String> newPlayerList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            newPlayerList.add(playerList.get(randomList[i]));
        }

        // 시스템에서 귓속말로 각각 유저에게 직업 알려주기
        for (int i = 0; i < 5; i++) {
            String player = newPlayerList.get(i);
            String job = jobList.get(i);
            playerJobs.put(player, job); // 직업 저장
            notice("/to " + player + " " + job);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        // 첫 번째 낮 시작
        startDayPhase();
        sendPlayerList();
        sendGameStatus();
    }

    // 낮 단계 시작
    private static void startDayPhase() {
        isDay = true;
        votingTime = true;
        votes.clear(); // 투표 초기화
        remainingTime = 40; // 낮은 40초

        broadCast(" ");
        broadCast("🌞 === " + dayCount + "일차 낮이 되었습니다 ===");
        broadCast("모든 플레이어가 토론하고 마피아를 찾아 투표하세요!");
        broadCast("투표 명령어: /vote [플레이어명]");
        broadCast("생존 플레이어: " + getAlivePlayers());

        // 생존자 수 표시
        int aliveCount = 0;
        for (Boolean alive : playerAlive.values()) {
            if (alive)
                aliveCount++;
        }
        broadCast("생존자: " + aliveCount + "명");
        broadCast(" ");

        // 서버 중앙 타이머 시작
        startVotingTimer();
        sendPlayerList();
        sendGameStatus();
    }

    // 중앙 타이머 시작
    private static void startVotingTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        gameTimer = new java.util.Timer();
        gameTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                remainingTime--;
                // 타이머 정보를 별도 명령어로 전송 (채팅창에 표시하지 않음)
                sendTimerUpdate(remainingTime);

                if (remainingTime <= 0) {
                    gameTimer.cancel();
                    votingTime = false;
                    broadCast("투표 시간이 끝났습니다.");
                    processVoteResults();
                }
            }
        }, 1000, 1000);
    }

    // 타이머 업데이트를 모든 클라이언트에 전송
    private static void sendTimerUpdate(int time) {
        String timerMessage = "TIMER_UPDATE:" + time;
        synchronized (sendMap) {
            for (PrintWriter pw : sendMap.values()) {
                pw.println(timerMessage);
                pw.flush();
            }
        }
    }

    // 투표 결과 처리
    private static void processVoteResults() {
        broadCast(" ");
        broadCast("=== 투표 결과 ===");

        if (votes.isEmpty()) {
            broadCast("아무도 투표하지 않았습니다.");
        } else {
            // 과반수(현재 생존자 기준) 이상이 투표해야만 결과를 적용
            int aliveTotal = 0;
            for (Boolean alive : playerAlive.values()) {
                if (alive)
                    aliveTotal++;
            }
            int required = (aliveTotal / 2) + 1; // 과반수 기준

            if (votes.size() < required) {
                broadCast("과반수(" + required + "/" + aliveTotal + ") 이상이 투표하지 않아 처형이 진행되지 않습니다.");
                broadCast("투표 참여 인원: " + votes.size() + "명");
            } else {
                // 투표 집계
                HashMap<String, Integer> voteCount = new HashMap<>();
                for (String target : votes.values()) {
                    voteCount.put(target, voteCount.getOrDefault(target, 0) + 1);
                }

                // 결과 출력
                for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
                    broadCast(entry.getKey() + ": " + entry.getValue() + "표");
                }

                // 최다 득표자 찾기
                String maxVotedPlayer = null;
                int maxVotes = 0;
                boolean tie = false;

                for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
                    if (entry.getValue() > maxVotes) {
                        maxVotes = entry.getValue();
                        maxVotedPlayer = entry.getKey();
                        tie = false;
                    } else if (entry.getValue() == maxVotes && maxVotes > 0) {
                        tie = true;
                    }
                }

                if (tie || maxVotedPlayer == null) {
                    broadCast("동점으로 아무도 처형되지 않았습니다.");
                } else {
                    broadCast(maxVotedPlayer + "님이 처형됩니다.");
                    eliminatePlayer(maxVotedPlayer);
                    sendPlayerList();
                    sendGameStatus();
                    if (checkGameEnd()) {
                        return;
                    }
                }
            }
        }

        broadCast(" ");
        // 밤 시간으로 전환
        startNightPhase();
        sendGameStatus();
    }

    // 플레이어 제거 메서드
    private static void eliminatePlayer(String playerId) {
        playerAlive.put(playerId, false);
        String job = playerJobs.get(playerId);
        broadCast("💀 " + playerId + "님이 제거되었습니다. (직업: " + job + ")");
        sendPlayerList();

        // 해당 플레이어의 연결을 종료하지 않고 관찰자 모드로 전환
        PrintWriter pw = sendMap.get(playerId);
        if (pw != null) {
            pw.println("당신은 제거되었습니다. 이제 관찰자로 게임을 지켜볼 수 있습니다.");
            pw.flush();
        }
    }

    // 게임 종료 조건 확인
    private static boolean checkGameEnd() {
        int aliveCount = 0;
        int mafiaCount = 0;

        for (String player : playerAlive.keySet()) {
            if (playerAlive.get(player)) {
                aliveCount++;
                if ("마피아".equals(playerJobs.get(player))) {
                    mafiaCount++;
                }
            }
        }

        if (mafiaCount == 0) {
            broadCast(" ");
            broadCast("🎉 시민팀 승리! 마피아가 모두 제거되었습니다!");
            endGame();
            return true;
        } else if (mafiaCount >= aliveCount - mafiaCount) {
            broadCast(" ");
            broadCast("🎉 마피아팀 승리! 마피아가 과반수를 차지했습니다!");
            endGame();
            return true;
        }

        return false;
    }

    // 게임 종료
    private static void endGame() {
        gameStarted = false;
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        broadCast(" ");
        broadCast("=== 게임 결과 ===");
        for (String player : playerJobs.keySet()) {
            String status = playerAlive.get(player) ? "생존" : "사망";
            broadCast(player + " - " + playerJobs.get(player) + " (" + status + ")");
        }
        broadCast(" ");
        broadCast("게임이 종료되었습니다. 다시 게임하려면 서버를 재시작하세요.");
    }

    // 밤 단계 시작 (static으로 변경)
    private static void startNightPhase() {
        isDay = false;
        votingTime = false;
        remainingTime = 20; // 밤은 20초

        broadCast(" ");
        broadCast("🌙 === 밤이 되었습니다 ===");
        broadCast("마피아가 한 명을 제거합니다...");
        broadCast("시민들은 잠들어주세요.");
        broadCast(" ");
        sendGameStatus();

        // 밤 시간 타이머
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        gameTimer = new java.util.Timer();
        gameTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                remainingTime--;
                // 타이머 정보를 별도 명령어로 전송 (채팅창에 표시하지 않음)
                sendTimerUpdate(remainingTime);

                if (remainingTime <= 0) {
                    gameTimer.cancel();
                    broadCast("밤이 끝났습니다.");
                    processNightResults();
                    // 다시 낮으로 전환
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    dayCount++; // 날짜 증가
                    startDayPhase();
                }
            }
        }, 1000, 1000);
    }

    // 밤 행동 결과 처리
    private static void processNightResults() {
        broadCast(" ");
        broadCast("=== 밤이 지나갔습니다 ===");

        // 마피아의 타겟 처리
        if (mafiaTarget != null && playerAlive.getOrDefault(mafiaTarget, false)) {
            // 의사가 같은 사람을 치료했는지 확인
            boolean healed = false;
            for (String playerId : nightActions.keySet()) {
                if ("의사".equals(playerJobs.get(playerId))) {
                    String healTarget = nightActions.get(playerId);
                    if (mafiaTarget.equals(healTarget)) {
                        healed = true;
                        break;
                    }
                }
            }

            if (healed) {
                broadCast("💊 의사가 누군가를 살렸습니다!");
            } else {
                playerAlive.put(mafiaTarget, false);
                String job = playerJobs.get(mafiaTarget);
                broadCast("💀 " + mafiaTarget + "님이 마피아에게 살해되었습니다. (직업: " + job + ")");

                // 제거된 플레이어에게 알림
                PrintWriter pw = sendMap.get(mafiaTarget);
                if (pw != null) {
                    pw.println("당신은 마피아에게 살해되었습니다. 이제 관찰자로 게임을 지켜볼 수 있습니다.");
                    pw.flush();
                }
            }
        } else {
            broadCast("밤 동안 아무 일도 일어나지 않았습니다.");
        }

        // 밤 행동 초기화
        nightActions.clear();
        mafiaTarget = null;

        // 게임 종료 조건 확인
        checkGameEnd();
        broadCast(" ");
        sendPlayerList();
        sendGameStatus();
    }

    // 투표 처리
    private void handleVote(String voter, String voteCommand) {
        // 생존한 플레이어만 투표 가능
        if (!playerAlive.getOrDefault(voter, false)) {
            PrintWriter pw = sendMap.get(voter);
            if (pw != null) {
                pw.println("사망한 플레이어는 투표할 수 없습니다.");
                pw.flush();
            }
            return;
        }

        // /vote [플레이어명] 파싱
        String[] parts = voteCommand.split(" ");
        if (parts.length >= 2) {
            String target = parts[1];

            // 생존한 플레이어에게만 투표 가능
            if (sendMap.containsKey(target) && playerAlive.getOrDefault(target, false)) {
                votes.put(voter, target);
                broadCast("[투표] " + voter + "님이 " + target + "님에게 투표했습니다.");

                // 현재 투표 현황 표시
                int totalVoters = 0;
                for (String player : playerAlive.keySet()) {
                    if (playerAlive.get(player))
                        totalVoters++;
                }
                broadCast("현재 투표: " + votes.size() + "/" + totalVoters + "명");

            } else {
                PrintWriter pw = sendMap.get(voter);
                if (pw != null) {
                    if (!sendMap.containsKey(target)) {
                        pw.println("존재하지 않는 플레이어입니다: " + target);
                    } else {
                        pw.println("사망한 플레이어에게는 투표할 수 없습니다: " + target);
                    }
                    pw.flush();
                }
            }
        }
    }

    // 생존 플레이어 목록 반환 (static으로 변경)
    private static String getAlivePlayers() {
        StringBuilder sb = new StringBuilder();
        for (String player : sendMap.keySet()) {
            if (playerAlive.getOrDefault(player, true)) { // 기본값은 true (아직 초기화되지 않은 경우)
                sb.append(player).append(" ");
            }
        }
        return sb.toString();
    }

    // 플레이어 상태 정보를 JSON 형태로 전송
    private static void sendPlayerList() {
        StringBuilder playerInfo = new StringBuilder("PLAYER_LIST:");
        for (String playerId : sendMap.keySet()) {
            boolean alive = playerAlive.getOrDefault(playerId, true);
            String status = alive ? "생존" : "사망";
            int voteCount = 0;

            // 투표 수 계산
            for (String vote : votes.values()) {
                if (vote.equals(playerId)) {
                    voteCount++;
                }
            }

            playerInfo.append(playerId).append(",").append(status).append(",").append(voteCount).append(";");
        }

        // 모든 클라이언트에게 플레이어 목록 전송
        synchronized (sendMap) {
            for (PrintWriter pw : sendMap.values()) {
                pw.println(playerInfo.toString());
                pw.flush();
            }
        }
    }

    // 게임 상태 정보 전송
    private static void sendGameStatus() {
        String phase = isDay ? "낮" : "밤";
        String gameStatus = "GAME_STATUS:" + phase + "," + remainingTime + "," + getAlivePlayerCount() + "/"
                + sendMap.size();

        synchronized (sendMap) {
            for (PrintWriter pw : sendMap.values()) {
                pw.println(gameStatus);
                pw.flush();
            }
        }
    }

    private static int getAlivePlayerCount() {
        int count = 0;
        for (Boolean alive : playerAlive.values()) {
            if (alive)
                count++;
        }
        return count;
    }

    // 마피아 킬 처리
    private void handleMafiaKill(String mafiaId, String killCommand) {
        String[] parts = killCommand.split(" ");
        if (parts.length >= 2) {
            String target = parts[1];

            if (sendMap.containsKey(target) && playerAlive.getOrDefault(target, false)) {
                if (!target.equals(mafiaId)) { // 자기 자신은 타겟할 수 없음
                    mafiaTarget = target;
                    nightActions.put(mafiaId, target);

                    PrintWriter pw = sendMap.get(mafiaId);
                    if (pw != null) {
                        pw.println("🔪 " + target + "님을 타겟으로 선택했습니다.");
                        pw.flush();
                    }

                    // 다른 마피아에게도 알림 (여러 마피아가 있는 경우)
                    for (String playerId : playerJobs.keySet()) {
                        if ("마피아".equals(playerJobs.get(playerId)) && !playerId.equals(mafiaId)
                                && playerAlive.getOrDefault(playerId, false)) {
                            PrintWriter mafiaMessage = sendMap.get(playerId);
                            if (mafiaMessage != null) {
                                mafiaMessage.println("🔪 " + mafiaId + "님이 " + target + "님을 타겟으로 선택했습니다.");
                                mafiaMessage.flush();
                            }
                        }
                    }
                } else {
                    PrintWriter pw = sendMap.get(mafiaId);
                    if (pw != null) {
                        pw.println("자기 자신을 타겟할 수 없습니다.");
                        pw.flush();
                    }
                }
            } else {
                PrintWriter pw = sendMap.get(mafiaId);
                if (pw != null) {
                    pw.println("존재하지 않거나 이미 사망한 플레이어입니다: " + target);
                    pw.flush();
                }
            }
        }
    }

    // 경찰 조사 처리
    private void handlePoliceCheck(String policeId, String checkCommand) {
        String[] parts = checkCommand.split(" ");
        if (parts.length >= 2) {
            String target = parts[1];

            if (sendMap.containsKey(target) && playerAlive.getOrDefault(target, false)) {
                if (!target.equals(policeId)) {
                    nightActions.put(policeId, target);

                    String targetJob = playerJobs.get(target);
                    boolean isMafia = "마피아".equals(targetJob);

                    PrintWriter pw = sendMap.get(policeId);
                    if (pw != null) {
                        if (isMafia) {
                            pw.println("🚔 조사 결과: " + target + "님은 마피아입니다!");
                        } else {
                            pw.println("🚔 조사 결과: " + target + "님은 마피아가 아닙니다.");
                        }
                        pw.flush();
                    }
                } else {
                    PrintWriter pw = sendMap.get(policeId);
                    if (pw != null) {
                        pw.println("자기 자신을 조사할 수 없습니다.");
                        pw.flush();
                    }
                }
            } else {
                PrintWriter pw = sendMap.get(policeId);
                if (pw != null) {
                    pw.println("존재하지 않거나 이미 사망한 플레이어입니다: " + target);
                    pw.flush();
                }
            }
        }
    }

    // 의사 치료 처리
    private void handleDoctorHeal(String doctorId, String healCommand) {
        String[] parts = healCommand.split(" ");
        if (parts.length >= 2) {
            String target = parts[1];

            if (sendMap.containsKey(target) && playerAlive.getOrDefault(target, false)) {
                nightActions.put(doctorId, target);

                PrintWriter pw = sendMap.get(doctorId);
                if (pw != null) {
                    pw.println("💉 " + target + "님을 치료했습니다.");
                    pw.flush();
                }
            } else {
                PrintWriter pw = sendMap.get(doctorId);
                if (pw != null) {
                    pw.println("존재하지 않거나 이미 사망한 플레이어입니다: " + target);
                    pw.flush();
                }
            }
        }
    }

    /*
     * =============================
     * 로비 시스템 명령어 핸들러
     * =============================
     */
}
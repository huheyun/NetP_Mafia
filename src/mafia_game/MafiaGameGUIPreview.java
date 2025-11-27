package mafia_game;

import javax.swing.*;

/**
 * 마피아 게임 GUI 미리보기 - 서버 연결 없이 UI만 확인
 */
public class MafiaGameGUIPreview {

    public static void main(String[] args) {
        // Look and Feel 설정
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 기본 Look and Feel 사용
        }

        SwingUtilities.invokeLater(() -> {
            MafiaGameGUI gui = new MafiaGameGUI("localhost", 5592, true); // 미리보기 모드

            // 테스트 데이터 시뮬레이션
            simulateGameData(gui);
        });
    }

    private static void simulateGameData(MafiaGameGUI gui) {
        // 2초 후 역할 배정 시뮬레이션
        Timer timer = new Timer(2000, e -> {
            // 랜덤으로 역할 선택
            String[] roles = { "마피아", "경찰", "의사", "시민" };
            String randomRole = roles[(int) (Math.random() * roles.length)];

            // 서버 메시지 시뮬레이션
            gui.simulateServerMessage("🎮 게임에 접속했습니다. 플레이어: TestPlayer");
            gui.simulateServerMessage("=== 직업 배정 중 ===");
            gui.simulateServerMessage("당신의 역할: " + randomRole);
            gui.simulateServerMessage("");
            gui.simulateServerMessage("🌞 === 1일차 낮이 되었습니다 ===");
            gui.simulateServerMessage("모든 플레이어가 토론하고 마피아를 찾아 투표하세요!");
            gui.simulateServerMessage("생존 플레이어: Player1 Player2 Player3 Player4 TestPlayer");
            gui.simulateServerMessage("생존자: 5명");

            // 플레이어 리스트 시뮬레이션
            String playerList = "PLAYER_LIST:Player1,생존,0;Player2,생존,0;Player3,생존,1;Player4,생존,0;TestPlayer,생존,0;";
            gui.simulateServerMessage(playerList);

            // 타이머 시작 시뮬레이션
            startTimerSimulation(gui);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void startTimerSimulation(MafiaGameGUI gui) {
        // 30초 카운트다운 시뮬레이션
        Timer countdownTimer = new Timer(1000, null);
        final int[] timeLeft = { 30 };

        countdownTimer.addActionListener(e -> {
            gui.simulateServerMessage("TIMER_UPDATE:" + timeLeft[0]);
            timeLeft[0]--;

            if (timeLeft[0] < 0) {
                ((Timer) e.getSource()).stop();
                gui.simulateServerMessage("투표 시간이 끝났습니다.");

                // 밤으로 전환
                Timer nightTimer = new Timer(2000, evt -> {
                    gui.simulateServerMessage("");
                    gui.simulateServerMessage("🌙 === 밤이 되었습니다 ===");
                    gui.simulateServerMessage("마피아가 한 명을 제거합니다...");
                    gui.simulateServerMessage("시민들은 잠들어주세요.");
                });
                nightTimer.setRepeats(false);
                nightTimer.start();
            }
        });
        countdownTimer.start();
    }
}

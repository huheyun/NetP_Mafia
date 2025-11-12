package mafia_game;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 개선된 마피아 게임 클라이언트 메인 클래스
 */
public class MafiaGameClientMain {

    public static void main(String[] args) {
        // Look and Feel 설정 (시스템 기본값 사용)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 기본 Look and Feel 사용
        }

        // Swing EDT에서 GUI 실행
        SwingUtilities.invokeLater(() -> {
            try {
                // 서버 정보
                String serverIP = "localhost";
                int serverPort = 5592; // 설정 파일과 동일한 포트

                // GUI 생성 및 표시
                new MafiaGameGUI(serverIP, serverPort);

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("클라이언트 시작 실패: " + e.getMessage());
            }
        });
    }
}
package mafia_game;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import mafia_game.SFXPlayer;

/**
 * 개선된 마피아 게임 GUI
 */
public class MafiaGameGUI extends JFrame implements ActionListener, Runnable {

    // 네트워크 관련
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName;

    // GUI 컴포넌트
    private JPanel mainPanel;
    private JPanel gameInfoPanel;
    private JPanel chatPanel;
    private JPanel controlPanel;

    // 게임 정보 표시
    private JLabel playerInfoLabel;
    private JLabel roleLabel;
    private JLabel phaseLabel;
    private JLabel timerLabel;
    private JLabel alivePlayersLabel;

    // 채팅 및 메시지
    private JTextArea chatArea;
    private JTextField chatInput;
    private JScrollPane chatScrollPane;

    // 플레이어 목록 테이블
    private JTable playersTable;
    private PlayerTableModel playersTableModel;
    private JScrollPane tableScrollPane;

    // 제어 버튼
    private JButton voteButton;
    private JButton nightActionButton;
    private JButton sendChatButton;

    // 게임 상태
    private String currentRole = "시민";
    private String currentPhase = "대기중";
    private boolean isAlive = true;

    public MafiaGameGUI(String ip, int port) {
        super("마피아 게임");
        initializeGUI();
        connectToServer(ip, port);
    }

    private void initializeGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // 메인 패널 설정
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createGameInfoPanel();
        createChatPanel();
        createPlayersPanel();
        createControlPanel();

        // 패널 조립
        mainPanel.add(gameInfoPanel, BorderLayout.NORTH);
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        mainPanel.add(createEastPanel(), BorderLayout.EAST);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
    }

    private void createGameInfoPanel() {
        gameInfoPanel = new JPanel(new GridLayout(2, 3, 10, 5));
        gameInfoPanel.setBorder(new TitledBorder("게임 정보"));
        gameInfoPanel.setPreferredSize(new Dimension(0, 80));

        playerInfoLabel = new JLabel("플레이어: 대기중", SwingConstants.CENTER);
        roleLabel = new JLabel("역할: 미배정", SwingConstants.CENTER);
        phaseLabel = new JLabel("페이즈: 대기중", SwingConstants.CENTER);
        timerLabel = new JLabel("시간: --:--", SwingConstants.CENTER);
        alivePlayersLabel = new JLabel("생존자: 0/0", SwingConstants.CENTER);
        JLabel gameTitle = new JLabel("🎭 MAFIA GAME", SwingConstants.CENTER);

        // 폰트 설정
        Font infoFont = new Font("맑은 고딕", Font.BOLD, 12);
        Font titleFont = new Font("맑은 고딕", Font.BOLD, 16);

        playerInfoLabel.setFont(infoFont);
        roleLabel.setFont(infoFont);
        phaseLabel.setFont(infoFont);
        timerLabel.setFont(infoFont);
        alivePlayersLabel.setFont(infoFont);
        gameTitle.setFont(titleFont);

        // 역할별 색상
        roleLabel.setOpaque(true);
        updateRoleDisplay();

        gameInfoPanel.add(gameTitle);
        gameInfoPanel.add(playerInfoLabel);
        gameInfoPanel.add(roleLabel);
        gameInfoPanel.add(phaseLabel);
        gameInfoPanel.add(timerLabel);
        gameInfoPanel.add(alivePlayersLabel);
    }

    private void createChatPanel() {
        chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(new TitledBorder("채팅 & 게임 로그"));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        chatArea.setBackground(new Color(248, 248, 248));

        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 채팅 입력
        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        chatInput = new JTextField();
        chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        chatInput.addActionListener(this);

        sendChatButton = new JButton("전송");
        sendChatButton.addActionListener(this);
        sendChatButton.setPreferredSize(new Dimension(60, 25));

        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);

        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
    }

    private void createPlayersPanel() {
        playersTableModel = new PlayerTableModel();
        playersTable = new JTable(playersTableModel);
        playersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playersTable.setRowHeight(25);
        playersTable.getTableHeader().setReorderingAllowed(false);

        // 열 너비 설정
        playersTable.getColumnModel().getColumn(0).setPreferredWidth(80); // 이름
        playersTable.getColumnModel().getColumn(1).setPreferredWidth(60); // 상태
        playersTable.getColumnModel().getColumn(2).setPreferredWidth(40); // 투표

        tableScrollPane = new JScrollPane(playersTable);
        tableScrollPane.setPreferredSize(new Dimension(200, 0));
        tableScrollPane.setBorder(new TitledBorder("플레이어 목록"));
    }

    private JPanel createEastPanel() {
        JPanel eastPanel = new JPanel(new BorderLayout(5, 5));
        eastPanel.add(tableScrollPane, BorderLayout.CENTER);
        return eastPanel;
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setPreferredSize(new Dimension(0, 50));

        voteButton = new JButton("🗳️ 투표하기");
        nightActionButton = new JButton("🌙 밤 행동");

        voteButton.addActionListener(this);
        nightActionButton.addActionListener(this);

        // 버튼 스타일
        voteButton.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        nightActionButton.setFont(new Font("맑은 고딕", Font.BOLD, 12));

        voteButton.setPreferredSize(new Dimension(120, 35));
        nightActionButton.setPreferredSize(new Dimension(120, 35));

        controlPanel.add(voteButton);
        controlPanel.add(nightActionButton);

        // 초기에는 버튼 비활성화
        updateControlButtons();
    }

    private void updateRoleDisplay() {
        switch (currentRole) {
            case "마피아":
                roleLabel.setBackground(Color.RED);
                roleLabel.setForeground(Color.WHITE);
                break;
            case "경찰":
                roleLabel.setBackground(Color.BLUE);
                roleLabel.setForeground(Color.WHITE);
                break;
            case "의사":
                roleLabel.setBackground(Color.GREEN);
                roleLabel.setForeground(Color.WHITE);
                break;
            default: // 시민
                roleLabel.setBackground(Color.LIGHT_GRAY);
                roleLabel.setForeground(Color.BLACK);
        }
        roleLabel.setText("역할: " + currentRole);
    }

    private void updateControlButtons() {
        boolean isDayPhase = "낮".equals(currentPhase);
        boolean isNightPhase = "밤".equals(currentPhase);
        boolean hasNightAction = !"시민".equals(currentRole);

        voteButton.setEnabled(isDayPhase && isAlive);
        nightActionButton.setEnabled(isNightPhase && hasNightAction && isAlive);

        // 버튼 텍스트 업데이트
        if (isNightPhase) {
            switch (currentRole) {
                case "마피아":
                    nightActionButton.setText("🔪 살해");
                    break;
                case "경찰":
                    nightActionButton.setText("🔍 조사");
                    break;
                case "의사":
                    nightActionButton.setText("💉 치료");
                    break;
            }
        }
    }

    private void connectToServer(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // 사용자 이름 입력
            playerName = JOptionPane.showInputDialog(this, "사용자 이름을 입력하세요:", "마피아 게임 접속", JOptionPane.PLAIN_MESSAGE);
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Player" + System.currentTimeMillis() % 1000;
            }

            out.println(playerName);
            playerInfoLabel.setText("플레이어: " + playerName);

            // 메시지 수신 스레드 시작
            Thread thread = new Thread(this);
            thread.start();

            appendToChat("🎮 게임에 접속했습니다. 플레이어: " + playerName);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "서버 접속 실패: " + e.getMessage(), "연결 오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void appendToChat(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    @Override
    public void run() {
        System.out.println("[DEBUG] run() 시작됨");
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[DEBUG] 받은 메시지: " + message);
                processServerMessage(message);
            }
        } catch (IOException e) {
            appendToChat("❌ 서버 연결이 끊어졌습니다.");
            e.printStackTrace();
        }
    }

    private void processServerMessage(String message) {
        // 특별한 게임 정보 메시지 처리 (채팅창에 표시하지 않음)
        if (message.startsWith("TIMER_UPDATE:")) {
            updateTimer(message);
            return;
        } else if (message.startsWith("PLAYER_LIST:")) {
            updatePlayerList(message);
            return;
        } else if (message.startsWith("GAME_STATUS:")) {
            updateGameStatus(message);
            return;
        }

        // 일반 메시지는 채팅창에 표시
        appendToChat(message);

        // 게임 상태 메시지 파싱
        if (message.contains("당신의 역할:")) {
            String role = message.substring(message.indexOf(":") + 1).trim();
            currentRole = role;
            SwingUtilities.invokeLater(() -> updateRoleDisplay());


        } else if (message.contains("낮이") && message.contains("되었")) {
            System.out.println("[DEBUG] 낮 감지 성공!");
             currentPhase = "낮";
             BGMPlayer.playBGM("src/mafia_game/sounds/morning.wav");
             SwingUtilities.invokeLater(() -> {
                  phaseLabel.setText("페이즈: 낮 (토론)");
                 updateControlButtons();
             });

        } else if (message.contains("밤이") && message.contains("되었")) {
            System.out.println("[DEBUG]낮 감지 성공!");
             currentPhase = "밤";
             BGMPlayer.playBGM("src/mafia_game/sounds/night.wav");
             SwingUtilities.invokeLater(() -> {
                  phaseLabel.setText("페이즈: 밤 (특수행동)");
                 updateControlButtons();
             });
        } else if (message.contains("님이 제거되었습니다") && message.contains(playerName)) {
            isAlive = false;
            SFXPlayer.playSound("src/mafia_game/sounds/pistol-shot.wav");
            SwingUtilities.invokeLater(() -> {
                playerInfoLabel.setText("플레이어: " + playerName + " (사망)");
                updateControlButtons();
            });
        } else if (message.contains("살해되었습니다") && message.contains(playerName)) {
            isAlive = false;
            SFXPlayer.playSound("src/mafia_game/sounds/pistol-shot.wav");
            SwingUtilities.invokeLater(() -> {
                playerInfoLabel.setText("플레이어: " + playerName + " (사망)");
                updateControlButtons();
            });
        }
    }

    // 타이머 업데이트 처리
    private void updateTimer(String message) {
        // TIMER_UPDATE:25 형태 파싱
        String timeStr = message.substring("TIMER_UPDATE:".length());
        try {
            int time = Integer.parseInt(timeStr);
            SwingUtilities.invokeLater(() -> {
                timerLabel.setText("시간: " + time + "초");
            });
        } catch (NumberFormatException e) {
            // 파싱 실패 무시
        }
    }

    private void updatePlayerList(String message) {
        // PLAYER_LIST:player1,생존,2;player2,사망,0;player3,생존,1; 형태 파싱
        String data = message.substring("PLAYER_LIST:".length());
        String[] players = data.split(";");

        Object[][] tableData = new Object[players.length][3];
        for (int i = 0; i < players.length; i++) {
            if (!players[i].trim().isEmpty()) {
                String[] playerInfo = players[i].split(",");
                if (playerInfo.length >= 3) {
                    tableData[i][0] = playerInfo[0]; // 이름
                    tableData[i][1] = playerInfo[1]; // 상태
                    tableData[i][2] = playerInfo[2]; // 투표수
                }
            }
        }

        SwingUtilities.invokeLater(() -> {
            playersTableModel.updateData(tableData);
        });
    }

    private void updateGameStatus(String message) {
        // GAME_STATUS:낮,25,4/5 형태 파싱
        String data = message.substring("GAME_STATUS:".length());
        String[] status = data.split(",");

        if (status.length >= 3) {
            String phase = status[0];
            String time = status[1];
            String playerCount = status[2];

            SwingUtilities.invokeLater(() -> {
                phaseLabel.setText("페이즈: " + phase);
                timerLabel.setText("시간: " + time + "초");
                alivePlayersLabel.setText("생존자: " + playerCount);

                currentPhase = phase;
                updateControlButtons();
            });
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == chatInput || e.getSource() == sendChatButton) {
            sendChatMessage();
        } else if (e.getSource() == voteButton) {
            showVoteDialog();
        } else if (e.getSource() == nightActionButton) {
            showNightActionDialog();
        }
    }

    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            chatInput.setText("");
        }
    }

    private void showVoteDialog() {
        // 플레이어 목록에서 투표 대상 선택
        int selectedRow = playersTable.getSelectedRow();
        if (selectedRow >= 0) {
            String targetPlayer = (String) playersTableModel.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    targetPlayer + "님에게 투표하시겠습니까?",
                    "투표 확인",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                out.println("/vote " + targetPlayer);
                appendToChat("🗳️ " + targetPlayer + "님에게 투표했습니다.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "투표할 플레이어를 선택해주세요.", "투표", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showNightActionDialog() {
        int selectedRow = playersTable.getSelectedRow();
        if (selectedRow >= 0) {
            String targetPlayer = (String) playersTableModel.getValueAt(selectedRow, 0);
            String action = "";
            String actionText = "";

            switch (currentRole) {
                case "마피아":
                    action = "/kill";
                    actionText = "살해";
                    break;
                case "경찰":
                    action = "/check";
                    actionText = "조사";
                    break;
                case "의사":
                    action = "/heal";
                    actionText = "치료";
                    break;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    targetPlayer + "님을 " + actionText + "하시겠습니까?",
                    actionText + " 확인",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                out.println(action + " " + targetPlayer);
                appendToChat("🌙 " + targetPlayer + "님을 " + actionText + "했습니다.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "대상을 선택해주세요.", "밤 행동", JOptionPane.WARNING_MESSAGE);
        }
    }

    // 플레이어 테이블 모델 (내부 클래스)
    private class PlayerTableModel extends javax.swing.table.AbstractTableModel {
        private String[] columnNames = { "플레이어", "상태", "투표수" };
        private Object[][] data = {};

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data[rowIndex][columnIndex];
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        public void updateData(Object[][] newData) {
            this.data = newData;
            fireTableDataChanged();
        }
    }
}
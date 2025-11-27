package mafia_game;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.TitledBorder;

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
    private String currentRole = ""; // 빈 문자열로 초기화
    private String currentPhase = "대기중";
    private boolean isAlive = true;
    private boolean previewMode = false; // 미리보기 모드
    private boolean nightActionUsed = false; // 밤 능력 사용 여부

    // 역할 이미지
    private JLabel roleIconLabel;
    private ImageIcon mafiaIcon;
    private ImageIcon policeIcon;
    private ImageIcon doctorIcon;
    private ImageIcon citizenIcon;

    // 버튼 아이콘 캠시 (재사용)
    private HashMap<String, ImageIcon> buttonIconCache = new HashMap<>();

    // 배경 이미지
    private Image backgroundImage;

    public MafiaGameGUI(String ip, int port) {
        super("마피아 게임");
        initializeGUI();
        connectToServer(ip, port);
    }

    // 미리보기 모드 생성자
    public MafiaGameGUI(String ip, int port, boolean previewMode) {
        super("마피아 게임 - 미리보기 모드");
        this.previewMode = previewMode;
        this.playerName = "TestPlayer";
        initializeGUI();
        if (!previewMode) {
            connectToServer(ip, port);
        } else {
            appendToChat("🎨 미리보기 모드로 실행 중입니다.");
            appendToChat("서버 연결 없이 UI만 확인할 수 있습니다.");
            playerInfoLabel.setText("플레이어: TestPlayer");
        }
    }

    private void initializeGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(20, 20, 20));

        // 역할 이미지 로드
        loadRoleImages();

        // 배경 이미지 로드
        loadBackgroundImage();

        // 메인 패널 설정 (다크 테마)
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(20, 20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

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

    private JLabel createDarkInfoLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        return label;
    }

    private void createGameInfoPanel() {
        gameInfoPanel = new JPanel(new BorderLayout(10, 5));
        gameInfoPanel.setBackground(new Color(30, 30, 30));
        gameInfoPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        gameInfoPanel.setPreferredSize(new Dimension(0, 100));

        // 좌측: 역할 정보 + 아이콘
        JPanel leftPanel = new JPanel(new BorderLayout(10, 5));
        leftPanel.setBackground(new Color(30, 30, 30));

        JPanel roleInfoPanel = new JPanel();
        roleInfoPanel.setBackground(new Color(30, 30, 30));
        roleInfoPanel.setLayout(new BoxLayout(roleInfoPanel, BoxLayout.Y_AXIS));

        JLabel roleTitle = new JLabel("내 역할");
        roleTitle.setForeground(Color.LIGHT_GRAY);
        roleTitle.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        roleLabel = new JLabel("미배정");
        roleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        roleLabel.setForeground(new Color(240, 200, 80));

        roleInfoPanel.add(roleTitle);
        roleInfoPanel.add(roleLabel);

        // 역할 아이콘 (우측에 표시)
        roleIconLabel = new JLabel("", SwingConstants.CENTER);
        roleIconLabel.setPreferredSize(new Dimension(60, 60));

        leftPanel.add(roleInfoPanel, BorderLayout.WEST);
        leftPanel.add(roleIconLabel, BorderLayout.EAST);

        // 중앙: 게임 정보 (Day, Phase, Timer)
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        centerPanel.setBackground(new Color(30, 30, 30));

        playerInfoLabel = createDarkInfoLabel("플레이어: 대기중");
        phaseLabel = createDarkInfoLabel("페이즈: 대기중");
        timerLabel = createDarkInfoLabel("시간: --:--");

        centerPanel.add(playerInfoLabel);
        centerPanel.add(phaseLabel);
        centerPanel.add(timerLabel);

        // 우측: 생존자 수
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(new Color(30, 30, 30));
        alivePlayersLabel = new JLabel("생존자: 0/0");
        alivePlayersLabel.setForeground(Color.WHITE);
        alivePlayersLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        rightPanel.add(alivePlayersLabel);

        gameInfoPanel.add(leftPanel, BorderLayout.WEST);
        gameInfoPanel.add(centerPanel, BorderLayout.CENTER);
        gameInfoPanel.add(rightPanel, BorderLayout.EAST);

        updateRoleDisplay();
    }

    private void createChatPanel() {
        chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setOpaque(true);
        chatPanel.setBackground(new Color(25, 25, 25));
        TitledBorder border = new TitledBorder("채팅 & 게임 로그");
        border.setTitleColor(Color.LIGHT_GRAY);
        chatPanel.setBorder(border);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        chatArea.setOpaque(false); // 투명하게 설정
        chatArea.setForeground(Color.white);
        chatArea.setCaretColor(Color.WHITE);

        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setOpaque(false); // 투명하게 설정
        chatScrollPane.setBorder(null); // 테두리 제거
        chatScrollPane.getViewport().setOpaque(false); // 뷰포트도 투명하게 // 스크롤 팬 자체에 배경 이미지 그리기
        JPanel backgroundPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        backgroundPanel.setBackground(new Color(25, 25, 25));
        backgroundPanel.add(chatScrollPane, BorderLayout.CENTER);

        // 채팅 입력
        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 0));
        chatInputPanel.setOpaque(true);
        chatInputPanel.setBackground(new Color(25, 25, 25));

        chatInput = new JTextField();
        chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        chatInput.setBackground(new Color(50, 50, 50));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.WHITE);
        chatInput.addActionListener(this);

        sendChatButton = new JButton("전송");
        sendChatButton.setBackground(new Color(70, 70, 70));
        sendChatButton.setForeground(Color.BLACK);
        sendChatButton.setFocusPainted(false);
        sendChatButton.addActionListener(this);
        sendChatButton.setPreferredSize(new Dimension(80, 30));

        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);

        chatPanel.add(backgroundPanel, BorderLayout.CENTER);
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
    }

    private void createPlayersPanel() {
        playersTableModel = new PlayerTableModel();
        playersTable = new JTable(playersTableModel);
        playersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playersTable.setRowHeight(30);
        playersTable.getTableHeader().setReorderingAllowed(false);

        // 다크 테마 적용
        playersTable.setBackground(new Color(35, 35, 35));
        playersTable.setForeground(Color.WHITE);
        playersTable.setSelectionBackground(new Color(100, 60, 20));
        playersTable.setSelectionForeground(Color.WHITE);
        playersTable.setGridColor(new Color(60, 60, 60));
        playersTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        // 헤더 스타일
        playersTable.getTableHeader().setBackground(new Color(45, 45, 45));
        playersTable.getTableHeader().setForeground(Color.BLACK);
        playersTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));

        // 열 너비 설정
        playersTable.getColumnModel().getColumn(0).setPreferredWidth(80); // 이름
        playersTable.getColumnModel().getColumn(1).setPreferredWidth(60); // 상태
        playersTable.getColumnModel().getColumn(2).setPreferredWidth(40); // 투표

        tableScrollPane = new JScrollPane(playersTable);
        tableScrollPane.setPreferredSize(new Dimension(250, 0));
        tableScrollPane.setBackground(new Color(25, 25, 25));
        TitledBorder border = new TitledBorder("플레이어 목록");
        border.setTitleColor(Color.LIGHT_GRAY);
        tableScrollPane.setBorder(border);
    }

    private JPanel createEastPanel() {
        JPanel eastPanel = new JPanel(new BorderLayout(5, 5));
        eastPanel.setBackground(new Color(20, 20, 20));
        eastPanel.add(tableScrollPane, BorderLayout.CENTER);
        return eastPanel;
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(50, 50, 50));
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        // 호버 효과
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(70, 70, 70));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(50, 50, 50));
            }
        });
        return btn;
    }

    private void createControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controlPanel.setBackground(new Color(30, 30, 30));
        controlPanel.setPreferredSize(new Dimension(0, 60));

        voteButton = createStyledButton("투표하기");
        nightActionButton = createStyledButton("밤 행동");

        voteButton.addActionListener(this);
        nightActionButton.addActionListener(this);

        voteButton.setPreferredSize(new Dimension(140, 40));
        nightActionButton.setPreferredSize(new Dimension(140, 40));

        controlPanel.add(voteButton);
        controlPanel.add(nightActionButton);

        // 초기에는 버튼 비활성화
        updateControlButtons();
    }

    private void updateRoleDisplay() {
        // 역할이 아직 배정되지 않은 경우
        if (currentRole == null || currentRole.isEmpty()) {
            System.out.println("[DEBUG] updateRoleDisplay() - 역할 미배정");
            roleLabel.setText("미배정");
            roleLabel.setForeground(new Color(240, 200, 80));
            if (roleIconLabel != null) {
                roleIconLabel.setIcon(null);
            }
            return;
        }

        System.out.println("[DEBUG] updateRoleDisplay() - 역할: " + currentRole);
        // 역할 아이콘 업데이트
        ImageIcon currentIcon = null;
        switch (currentRole) {
            case "마피아":
                roleLabel.setForeground(new Color(255, 100, 100));
                currentIcon = mafiaIcon;
                break;
            case "경찰":
                roleLabel.setForeground(new Color(100, 150, 255));
                currentIcon = policeIcon;
                break;
            case "의사":
                roleLabel.setForeground(new Color(100, 255, 150));
                currentIcon = doctorIcon;
                break;
            default: // 시민
                roleLabel.setForeground(new Color(240, 200, 80));
                currentIcon = citizenIcon;
        }
        roleLabel.setText(currentRole);

        // 역할 아이콘 표시
        if (currentIcon != null && roleIconLabel != null) {
            roleIconLabel.setIcon(currentIcon);
        }
    }

    private void updateControlButtons() {
        boolean isDayPhase = "낮".equals(currentPhase);
        boolean isNightPhase = "밤".equals(currentPhase);
        boolean hasNightAction = !"시민".equals(currentRole);

        voteButton.setEnabled(isDayPhase && isAlive);
        // 밤 능력은 생존하고, 밤 페이즈이고, 능력이 있고, 아직 사용하지 않은 경우에만 활성화
        nightActionButton.setEnabled(isNightPhase && hasNightAction && isAlive && !nightActionUsed);

        // 버튼 텍스트 및 아이콘 업데이트
        if (isNightPhase) {
            ImageIcon actionIcon = null;
            switch (currentRole) {
                case "마피아":
                    nightActionButton.setText("살해");
                    actionIcon = loadButtonIcon("kill");
                    break;
                case "경찰":
                    nightActionButton.setText("조사");
                    actionIcon = loadButtonIcon("check");
                    break;
                case "의사":
                    nightActionButton.setText("치료");
                    actionIcon = loadButtonIcon("heal");
                    break;
            }
            if (actionIcon != null) {
                nightActionButton.setIcon(actionIcon);
            }
        } else {
            nightActionButton.setIcon(null);
        }

        // 투표 버튼 아이콘
        ImageIcon voteIcon = loadButtonIcon("vote");
        if (voteIcon != null) {
            voteButton.setIcon(voteIcon);
        }
        updateChatAvailability();
    }

    private void updateChatAvailability() {
        boolean isNightPhase = "밤".equals(currentPhase);
        boolean citizenNightSilent = isNightPhase && "시민".equals(currentRole);
        boolean enabled = isAlive && !citizenNightSilent;
        chatInput.setEnabled(enabled);
        sendChatButton.setEnabled(enabled);
        if (!enabled) {
            chatInput.setText("");
        }
    }

    // 플레이어 정보와 역할 정보를 갱신 (창 크기 변경 시에도 유지)
    private void refreshPlayerAndRoleInfo() {
        if (playerName != null && !playerName.isEmpty()) {
            if (isAlive) {
                playerInfoLabel.setText("플레이어: " + playerName);
            } else {
                playerInfoLabel.setText("플레이어: " + playerName + " (사망)");
            }
        }

        if (currentRole != null && !currentRole.isEmpty()) {
            roleLabel.setText("역할: " + currentRole);
        }

        // UI 강제 갱신
        if (gameInfoPanel != null) {
            gameInfoPanel.revalidate();
            gameInfoPanel.repaint();
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

            appendToChat("게임에 접속했습니다. 플레이어: " + playerName);

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
            appendToChat("서버 연결이 끊어졌습니다.");
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
        } else if (message.startsWith("SOUND_TRIGGER:")) {
            // 사운드 트리거 처리
            String soundName = message.substring("SOUND_TRIGGER:".length());
            switch (soundName) {
                case "siren":
                    SFXPlayer.playSound("src/mafia_game/sounds/siren.wav");
                    break;
                case "reloading":
                    SFXPlayer.playSound("src/mafia_game/sounds/reloading.wav");
                    break;
            }
            return;
        }

        // 명령어 에코 필터링 (채팅창에 표시하지 않음)
        if (message.startsWith("[" + playerName + "] /vote") ||
                message.startsWith("[" + playerName + "] /kill") ||
                message.startsWith("[" + playerName + "] /check") ||
                message.startsWith("[" + playerName + "] /heal")) {
            return; // 자신의 명령어는 채팅창에 표시하지 않음
        }

        // 게임 상태 메시지 파싱 (채팅창 출력 전에 먼저 처리)
        if (message.contains("당신의 역할:")) {
            String role = message.substring(message.indexOf(":") + 1).trim();
            currentRole = role;
            System.out.println("[DEBUG] 역할 배정 - playerName: " + playerName + ", role: " + role);
            SwingUtilities.invokeLater(() -> {
                playerInfoLabel.setText("플레이어: " + playerName);
                System.out.println("[DEBUG] playerInfoLabel 설정 완료: " + playerInfoLabel.getText());
                updateRoleDisplay(); // roleLabel.setText는 여기서 처리됨
                System.out.println("[DEBUG] roleLabel 설정 완료: " + roleLabel.getText());
                updateControlButtons();
            });
            appendToChat(message);
            return;
        } else if (message.contains("낮이") && message.contains("되었")) {
            System.out.println("[DEBUG] 낮 감지 성공!");
            BGMPlayer.playBGM("src/mafia_game/sounds/morning.wav");
            SwingUtilities.invokeLater(() -> {
                currentPhase = "낮";
                phaseLabel.setText("페이즈: 낮 (토론)");
                updateControlButtons();
            });
        } else if (message.contains("밤이") && message.contains("되었")) {
            System.out.println("[DEBUG] 밤 감지 성공!");
            BGMPlayer.playBGM("src/mafia_game/sounds/night.wav");
            SwingUtilities.invokeLater(() -> {
                currentPhase = "밤";
                nightActionUsed = false; // 밤이 시작되면 능력 사용 초기화
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

        // 일반 채팅 메시지는 채팅창에 표시
        appendToChat(message);
    }

    // 타이머 업데이트 처리
    private void updateTimer(String message) {
        // TIMER_UPDATE:25 형태 파싱
        String timeStr = message.substring("TIMER_UPDATE:".length());
        try {
            int time = Integer.parseInt(timeStr);
            SwingUtilities.invokeLater(() -> {
                timerLabel.setText("시간: " + time + "초");
                // 창 크기 변경 시에도 플레이어 정보와 역할 정보가 유지되도록 갱신
                refreshPlayerAndRoleInfo();
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
                // 창 크기 변경 시에도 플레이어 정보와 역할 정보가 유지되도록 갱신
                refreshPlayerAndRoleInfo();
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
            // 버튼 사용 유도: 능력/투표 명령 직접 입력 차단
            if (message.startsWith("/vote") || message.startsWith("/kill") || message.startsWith("/check")
                    || message.startsWith("/heal")) {
                appendToChat("[알림] 해당 기능은 하단 버튼을 사용하세요.");
                chatInput.setText("");
                return;
            }
            if (!isAlive) {
                appendToChat("관찰자(사망) 상태에서는 채팅할 수 없습니다.");
                chatInput.setText("");
                return;
            }
            if ("밤".equals(currentPhase) && "시민".equals(currentRole)) {
                appendToChat("밤에는 시민은 채팅할 수 없습니다.");
                chatInput.setText("");
                return;
            }
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
                SFXPlayer.playSound("src/mafia_game/sounds/button.wav");
                out.println("/vote " + targetPlayer);
                out.flush();
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
                // 역할별 사운드 재생
                switch (currentRole) {
                    case "마피아":
                        SFXPlayer.playSound("src/mafia_game/sounds/blade.wav");
                        break;
                    case "경찰":
                        SFXPlayer.playSound("src/mafia_game/sounds/police.wav");
                        break;
                    case "의사":
                        SFXPlayer.playSound("src/mafia_game/sounds/heal-sound.wav");
                        break;
                }
                out.println(action + " " + targetPlayer);
                out.flush();
                // 밤 능력 사용 표시 및 버튼 비활성화
                nightActionUsed = true;
                updateControlButtons();
                appendToChat("[완료] " + actionText + " 능력을 사용했습니다. (이번 밤에는 더 이상 사용할 수 없습니다)");
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

    /*
     * =============================
     * 미리보기 모드 지원
     * =============================
     */
    public void simulateServerMessage(String message) {
        if (previewMode) {
            processServerMessage(message);
        }
    }

    /*
     * =============================
     * 역할 이미지 로드
     * =============================
     */
    private void loadRoleImages() {
        try {
            // 이미지 파일 경로
            String basePath = "resources/images/roles/";

            // 각 역할별 이미지 로드 (60x60 크기로 스케일링)
            mafiaIcon = loadAndScaleImage(basePath + "mafia.png", 60, 60);
            policeIcon = loadAndScaleImage(basePath + "police.png", 60, 60);
            doctorIcon = loadAndScaleImage(basePath + "doctor.png", 60, 60);
            citizenIcon = loadAndScaleImage(basePath + "citizen.png", 60, 60);

        } catch (Exception e) {
            System.err.println("역할 이미지 로드 실패: " + e.getMessage());
            // 이미지 로드 실패 시 기본 아이콘 사용 (null)
        }
    }

    private void loadBackgroundImage() {
        try {
            File bgFile = new File("resources/images/mafia_bg.png");
            if (bgFile.exists()) {
                backgroundImage = new ImageIcon(bgFile.getAbsolutePath()).getImage();
                System.out.println("배경 이미지 로드 성공: " + bgFile.getAbsolutePath());
            } else {
                System.out.println("배경 이미지 파일을 찾을 수 없습니다: " + bgFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("배경 이미지 로드 실패: " + e.getMessage());
        }
    }

    private ImageIcon loadAndScaleImage(String path, int width, int height) {
        try {
            File imageFile = new File(path);
            if (imageFile.exists()) {
                ImageIcon icon = new ImageIcon(path);
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImg);
            } else {
                System.err.println("이미지 파일을 찾을 수 없습니다: " + path);
                return null;
            }
        } catch (Exception e) {
            System.err.println("이미지 로드 실패 (" + path + "): " + e.getMessage());
            return null;
        }
    }

    /*
     * =============================
     * 버튼 아이콘 로드 (추후 이미지 추가)
     * =============================
     */
    private ImageIcon loadButtonIcon(String iconName) {
        // 캐시 확인
        if (buttonIconCache.containsKey(iconName)) {
            return buttonIconCache.get(iconName);
        }

        try {
            String iconPath = "resources/images/icon/" + iconName + ".png";
            File iconFile = new File(iconPath);
            if (iconFile.exists()) {
                ImageIcon icon = new ImageIcon(iconPath);
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImg);
                // 캐시에 저장
                buttonIconCache.put(iconName, scaledIcon);
                return scaledIcon;
            }
        } catch (Exception e) {
            System.err.println("아이콘 로드 실패 (" + iconName + "): " + e.getMessage());
        }
        return null;
    }
}
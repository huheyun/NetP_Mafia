package mafia_game;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public abstract class TcpApplication {

    /* 서버에 필요한 필드 정보 */
    public static String IP;
    public static int PORT;
    public static String CONFIG = "config/applicationcontext.ini";

    /* 애플리케이션 초기화 */
    public void init() {
        // 설정 파일로부터 필요한 정보를 로드
        Properties settings = new Properties();

        try {
            settings.load(new FileInputStream(CONFIG));
            IP = settings.getProperty("IP");
            PORT = Integer.parseInt(settings.getProperty("PORT"));

            System.out.println("IPAddress : " + IP);
            System.out.println("     PORT : " + PORT);

            System.out.println(TcpApplication.timeStamp());

        } catch (IOException e) {
            System.out.println("설정파일(applicationcontext.ini)을 찾을 수 없습니다.");
            System.out.println("프로그램을 종료합니다.");
            System.exit(0);
        }
    }

    /* 서브클래스에서 구현해야 할 추상메소드 */
    public abstract void start();

    /* 현재 시간을 가져오는 메소드 / 반환타입 : String */
    public static String timeStamp() {
        SimpleDateFormat format = new SimpleDateFormat("[hh:mm:ss]");
        return format.format(new Date());
    }
}
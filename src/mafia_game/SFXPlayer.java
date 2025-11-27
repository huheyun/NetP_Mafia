package mafia_game;

import javax.sound.sampled.*;
import java.io.File;

public class SFXPlayer {

    public static void playSound(String path) {

        System.out.println("[SFX] 재생 요청: " + path);

        new Thread(() -> {
            File audioFile = new File(path);

            if (!audioFile.exists()) {
                System.out.println("[SFX] 파일 없음: " + audioFile.getAbsolutePath());
                return;
            }

            System.out.println("[SFX] 파일 로드 성공");

            // try-with-resources로 자동 리소스 해제
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile)) {
                AudioFormat baseFormat = audioStream.getFormat();

                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false);

                try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream)) {
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

                    line.open(decodedFormat);
                    line.start();

                    System.out.println("[SFX] 재생 시작");

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = decodedStream.read(buffer)) != -1) {
                        line.write(buffer, 0, bytesRead);
                    }

                    line.drain();
                    line.stop();
                    line.close();

                    System.out.println("[SFX] 🔫 재생 완료");
                }
            } catch (Exception e) {
                System.out.println("[SFX] ⚠ 오류 발생: " + e.getMessage());
            }
        }).start();
    }
}

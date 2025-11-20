package mafia_game;

import javax.sound.sampled.*;
import java.io.File;

public class BGMPlayer {

    private static Thread bgmThread;
    private static boolean isPlaying = false;

    public static void playBGM(String path) {
        stopBGM(); // 기존 BGM 정지
        isPlaying = true;

        bgmThread = new Thread(() -> {
            try {
                File audioFile = new File(path);
                if (!audioFile.exists()) {
                    System.err.println("❌ BGM 파일 없음: " + audioFile.getAbsolutePath());
                    return;
                }

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

                AudioFormat baseFormat = audioStream.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );

                AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(decodedFormat);
                line.start();

                System.out.println("▶ BGM 재생 시작: " + path);

                byte[] buffer = new byte[4096];
                int bytesRead;

                // 무한 반복 (isPlaying이 false 될 때까지)
                while (isPlaying) {
                    while ((bytesRead = decodedStream.read(buffer, 0, buffer.length)) != -1 && isPlaying) {
                        line.write(buffer, 0, bytesRead);
                    }

                    // 다시 처음으로(루프)
                    decodedStream.close();
                    audioStream.close();

                    audioStream = AudioSystem.getAudioInputStream(audioFile);
                    decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);
                }

                // 종료 처리
                line.drain();
                line.stop();
                line.close();
                decodedStream.close();
                audioStream.close();

                System.out.println("⏹ BGM 재생 종료");

            } catch (Exception e) {
                System.err.println("BGM 오류: " + e.getMessage());
                e.printStackTrace();
            }
        });

        bgmThread.start();
    }

    public static void stopBGM() {
        if (isPlaying) {
            System.out.println("⏹ BGM 중지");
            isPlaying = false;
        }

        if (bgmThread != null && bgmThread.isAlive()) {
            try {
                bgmThread.join(200); // 잠깐 기다림
            } catch (InterruptedException ignored) {}
        }
    }
}

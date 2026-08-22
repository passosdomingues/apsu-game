package br.apsu.audio;

import javafx.scene.media.AudioClip;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador desacoplado de efeitos sonoros e áudio.
 */
public class AudioManager {
    private static final AudioManager INSTANCE = new AudioManager();
    private final Map<String, AudioClip> sounds = new HashMap<>();
    private final boolean enabled = Boolean.parseBoolean(System.getProperty("apsu.audio.enabled", "true"));
    private AudioClip music;

    private AudioManager() {
        if (enabled) loadSounds();
    }

    public static AudioManager getInstance() {
        return INSTANCE;
    }

    private void loadSounds() {
        for (String soundName : new String[]{"shoot", "collect", "hurt", "boss-hit", "victory"}) {
            try {
                URL resource = getClass().getResource("/audio/" + soundName + ".wav");
                if (resource != null) {
                    sounds.put(soundName, new AudioClip(resource.toExternalForm()));
                }
            } catch (Exception ignored) {}
        }
        try {
            URL musicResource = getClass().getResource("/audio/apsu-theme.wav");
            if (musicResource != null) {
                music = new AudioClip(musicResource.toExternalForm());
                music.setCycleCount(AudioClip.INDEFINITE);
                music.setVolume(0.18);
            }
        } catch (Exception ignored) {}
    }

    public void playSound(String soundName) {
        if (!enabled) return;
        AudioClip clip = sounds.get(soundName);
        if (clip != null) {
            try {
                clip.setVolume("hurt".equals(soundName) ? 0.4 : 0.26);
                clip.play();
            } catch (Throwable ignored) {}
        }
    }

    public void playMusic() {
        if (!enabled) return;
        if (music != null) {
            try {
                music.play();
            } catch (Throwable ignored) {}
        }
    }

    public void stopMusic() {
        if (!enabled) return;
        if (music != null) {
            try {
                music.stop();
            } catch (Throwable ignored) {}
        }
    }
}

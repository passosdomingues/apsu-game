package br.apsu.audio;

import br.apsu.model.environment.OceanDepthProfile;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Adaptador JavaFX: AudioClip para efeitos curtos e MediaPlayer para música em loop. */
final class JavaFxAudioOutput implements AudioOutput {
    private final boolean enabled;
    private final Map<OceanDepthProfile, Map<AudioCue, AudioClip>> effects = new EnumMap<>(OceanDepthProfile.class);
    private MediaPlayer currentMusic;

    private static final double MUSIC_VOLUME = 0.55;

    JavaFxAudioOutput(boolean enabled) {
        this.enabled = enabled;
        if (enabled) loadAssets();
    }

    private void loadAssets() {
        for (OceanDepthProfile profile : OceanDepthProfile.values()) {
            EnumMap<AudioCue, AudioClip> profileEffects = new EnumMap<>(AudioCue.class);
            Map<String, AudioClip> profileAssets = new HashMap<>();
            for (AudioCue cue : AudioCue.values()) {
                String asset = asset(cue);
                AudioClip clip = profileAssets.get(asset);
                if (!profileAssets.containsKey(asset)) {
                    clip = load("/audio/generated/" + key(profile) + "/" + asset + ".wav");
                    if (clip == null) clip = load("/audio/" + asset + ".wav");
                    if (clip != null) profileAssets.put(asset, clip);
                }
                if (clip != null) profileEffects.put(cue, clip);
            }
            effects.put(profile, profileEffects);
        }
    }

    private AudioClip load(String path) {
        try {
            URL resource = getClass().getResource(path);
            return resource == null ? null : new AudioClip(resource.toExternalForm());
        } catch (Exception error) {
            System.err.println("[AUDIO] Não foi possível carregar efeito " + path + ": " + error.getMessage());
            return null;
        }
    }

    @Override public void playEffect(AudioCue cue, OceanDepthProfile depth) {
        if (!enabled || cue == null || depth == null) return;
        AudioClip clip = effects.getOrDefault(depth, Map.of()).get(cue);
        if (clip != null) try { clip.play(gain(cue) * depthGain(depth)); }
        catch (RuntimeException ignored) { }
    }

    @Override public void playMusic(OceanDepthProfile depth, boolean bossMode) {
        if (!enabled) return;
        startMusic(depth, bossMode, true, false);
    }

    @Override public void changeMusic(OceanDepthProfile depth, boolean bossMode, boolean resume) {
        if (!enabled) return;
        startMusic(depth, bossMode, resume, false);
    }

    @Override public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.dispose();
            currentMusic = null;
        }
    }

    private void startMusic(OceanDepthProfile depth, boolean bossMode, boolean resume, boolean useOriginalFallback) {
        if (depth == null) return;
        stopMusic();

        String generatedPath = bossMode
            ? "/audio/generated/boss/" + key(depth) + ".wav"
            : "/audio/generated/" + key(depth) + "/apsu-theme.wav";
        String selectedPath = useOriginalFallback ? "/audio/apsu-theme.wav" : generatedPath;
        URL resource = getClass().getResource(selectedPath);
        if (resource == null && !useOriginalFallback) {
            System.err.println("[AUDIO] Faixa ausente: " + selectedPath + "; tentando a faixa original.");
            startMusic(depth, bossMode, resume, true);
            return;
        }
        if (resource == null) {
            System.err.println("[AUDIO] Nenhuma faixa de música disponível para " + depth + ".");
            return;
        }

        try {
            MediaPlayer player = new MediaPlayer(new Media(resource.toExternalForm()));
            currentMusic = player;
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(MUSIC_VOLUME * depthGain(depth));
            player.setOnReady(() -> {
                if (currentMusic == player && resume) {
                    if (player.getStatus() == MediaPlayer.Status.READY) player.play();
                    System.out.println("[AUDIO] Trilha ativa: " + selectedPath
                        + " (volume " + Math.round(MUSIC_VOLUME * depthGain(depth) * 100) + "%).");
                }
            });
            player.setOnError(() -> {
                if (currentMusic != player) return;
                String message = player.getError() == null ? "erro de reprodução" : player.getError().getMessage();
                System.err.println("[AUDIO] Falha na faixa " + selectedPath + ": " + message);
                player.dispose();
                currentMusic = null;
                if (!useOriginalFallback) startMusic(depth, bossMode, resume, true);
            });
        } catch (RuntimeException error) {
            System.err.println("[AUDIO] Não foi possível iniciar " + selectedPath + ": " + error.getMessage());
            currentMusic = null;
            if (!useOriginalFallback) startMusic(depth, bossMode, resume, true);
        }
    }

    private static String key(OceanDepthProfile profile) {
        return profile.name().toLowerCase().replace('_', '-');
    }

    /** Asset names belong to this output adapter; gameplay emits only AudioCue values. */
    static String asset(AudioCue cue) {
        return switch (cue) {
            case MENU_NAVIGATE -> "menu-navigate";
            case MENU_CONFIRM -> "menu-confirm";
            case DIALOGUE_ADVANCE, PICKUP -> "collect";
            case ATTACK -> "shoot";
            case ENEMY_DAMAGED -> "enemy-damaged";
            case ENEMY_DEFEATED -> "collect";
            case BOSS_DAMAGED, BOSS_PHASE -> "boss-hit";
            case HERO_DAMAGED -> "hurt";
            case VICTORY -> "victory";
        };
    }

    static double gain(AudioCue cue) {
        return switch (cue) {
            case MENU_NAVIGATE -> 0.12;
            case MENU_CONFIRM -> 0.20;
            case DIALOGUE_ADVANCE -> 0.16;
            case PICKUP -> 0.28;
            case ATTACK -> 0.22;
            case ENEMY_DAMAGED -> 0.24;
            case ENEMY_DEFEATED -> 0.16;
            case BOSS_DAMAGED -> 0.30;
            case BOSS_PHASE -> 0.24;
            case HERO_DAMAGED -> 0.38;
            case VICTORY -> 0.30;
        };
    }

    private static double depthGain(OceanDepthProfile profile) {
        return switch (profile) {
            case COASTAL -> 1.00;
            case DEEP_REEF -> 0.94;
            case ABYSSAL_PLAIN -> 0.88;
            case HYDROTHERMAL_VENT -> 0.82;
            case HADAL_TRENCH -> 0.76;
        };
    }
}

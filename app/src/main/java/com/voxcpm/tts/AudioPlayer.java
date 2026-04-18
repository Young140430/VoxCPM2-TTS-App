package com.voxcpm.tts;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Audio player for WAV/MP3/OGG files using AudioTrack.
 * For WAV: direct PCM playback.
 * For other formats: uses MediaPlayer approach via temp file.
 */
public class AudioPlayer {
    private static final String TAG = "AudioPlayer";

    private AudioTrack pcmTrack;
    private volatile boolean isPlaying = false;
    private volatile boolean destroyed = false;
    private Thread playThread;
    private android.media.MediaPlayer mediaPlayer;

    /**
     * Play WAV audio data (raw PCM or WAV file bytes).
     */
    public void playWav(byte[] wavData) {
        stop();
        destroyed = false;

        // Parse WAV header to get format info
        int sampleRate = 24000;
        int channels = 1;
        int bitsPerSample = 16;

        if (wavData.length > 44) {
            // Read sample rate from WAV header (offset 24, 4 bytes little-endian)
            sampleRate = readLittleEndianInt(wavData, 24);
            // Read channels (offset 22, 2 bytes)
            channels = readLittleEndianShort(wavData, 22);
            // Read bits per sample (offset 34, 2 bytes)
            bitsPerSample = readLittleEndianShort(wavData, 34);
        }

        int channelConfig = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int encoding = bitsPerSample == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;

        int minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);

        pcmTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(encoding)
                        .build())
                .setBufferSizeInBytes(Math.max(minBuf, sampleRate * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        pcmTrack.play();
        isPlaying = true;

        // Data offset after WAV header
        int dataOffset = 44;
        if (wavData.length > 44) {
            // Find "data" chunk
            for (int i = 12; i < wavData.length - 4; i++) {
                if (wavData[i] == 'd' && wavData[i + 1] == 'a' &&
                        wavData[i + 2] == 't' && wavData[i + 3] == 'a') {
                    dataOffset = i + 8;
                    break;
                }
            }
        }

        final int offset = dataOffset;
        playThread = new Thread(() -> {
            try {
                int written = pcmTrack.write(wavData, offset, wavData.length - offset);
                Log.d(TAG, "WAV playback written: " + written + " bytes");
            } catch (Exception e) {
                Log.e(TAG, "WAV playback error", e);
            } finally {
                isPlaying = false;
            }
        }, "WavPlayThread");
        playThread.start();
    }

    /**
     * Play audio file using MediaPlayer (for MP3, FLAC, OGG formats).
     */
    public void playFile(String filePath) {
        stop();
        destroyed = false;

        try {
            mediaPlayer = new android.media.MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            mediaPlayer.setOnPreparedListener(mp -> {
                isPlaying = true;
                mp.start();
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                Log.e(TAG, "MediaPlayer error: what=" + what + " extra=" + extra);
                return false;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer setup failed", e);
        }
    }

    public void stop() {
        isPlaying = false;

        if (pcmTrack != null) {
            try {
                pcmTrack.pause();
                pcmTrack.flush();
                pcmTrack.stop();
                pcmTrack.release();
            } catch (Exception ignored) {}
            pcmTrack = null;
        }

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        if (playThread != null) {
            try {
                playThread.join(500);
            } catch (InterruptedException ignored) {}
            playThread = null;
        }
    }

    public void destroy() {
        destroyed = true;
        stop();
    }

    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return isPlaying;
    }

    private int readLittleEndianInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    private short readLittleEndianShort(byte[] data, int offset) {
        return (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
    }
}

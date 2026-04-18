package com.voxcpm.tts;

import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenAI-compatible /v1/audio/speech TTS API client.
 * Calls the VoxCPM2 TTS endpoint exposed by vllm_omni.
 */
public class TTSApi {
    private static final String TAG = "TTSApi";
    private static final MediaType JSON_MT = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private Call currentCall;

    public interface CallbackListener {
        void onSuccess(byte[] audioData, String format);
        void onFailure(String error);
    }

    public TTSApi() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Generate speech from text using the OpenAI-compatible /v1/audio/speech endpoint.
     *
     * @param text       Text to synthesize
     * @param model      Model name (e.g., "voxcpm2")
     * @param apiBase    API server base URL (e.g., "http://192.168.1.100:8000")
     * @param apiKey     API key (e.g., "sk-empty")
     * @param format     Output audio format (wav/mp3/flac/ogg)
     * @param refAudio   Reference audio as base64 data URI, URL, or null for zero-shot
     * @param listener   Callback for success/failure
     */
    public void generate(String text, String model, String apiBase, String apiKey,
                         String format, String refAudio, CallbackListener listener) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("input", text);
            payload.put("voice", "default");
            payload.put("response_format", format);

            if (refAudio != null && !refAudio.isEmpty()) {
                payload.put("ref_audio", refAudio);
            }

            String url = apiBase.replaceAll("/+$", "") + "/v1/audio/speech";

            RequestBody body = RequestBody.create(payload.toString(), JSON_MT);
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);

            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            currentCall = client.newCall(requestBuilder.build());
            currentCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Request failed", e);
                    if (!call.isCanceled()) {
                        listener.onFailure(e.getMessage() != null ? e.getMessage() : "请求失败");
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String errBody = "";
                            if (response.body() != null) {
                                errBody = response.body().string();
                                errBody = errBody.length() > 300 ? errBody.substring(0, 300) : errBody;
                            }
                            Log.e(TAG, "Error " + response.code() + ": " + errBody);
                            listener.onFailure("HTTP " + response.code() + ": " + errBody);
                            return;
                        }

                        byte[] audioData = response.body() != null ? response.body().bytes() : new byte[0];
                        if (audioData.length == 0) {
                            listener.onFailure("返回的音频数据为空");
                            return;
                        }

                        Log.d(TAG, "Received " + audioData.length + " bytes, format=" + format);
                        listener.onSuccess(audioData, format);
                    } catch (Exception e) {
                        Log.e(TAG, "Response processing error", e);
                        listener.onFailure(e.getMessage() != null ? e.getMessage() : "响应处理失败");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Build request failed", e);
            listener.onFailure(e.getMessage() != null ? e.getMessage() : "构建请求失败");
        }
    }

    /**
     * Cancel the current request.
     */
    public void cancel() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
            currentCall = null;
        }
    }

    /**
     * Encode a local audio file to a base64 data URI.
     */
    public static String encodeAudioToBase64(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("音频文件不存在: " + filePath);
        }

        String ext = filePath.toLowerCase();
        String mime = "audio/wav";
        if (ext.endsWith(".mp3")) mime = "audio/mpeg";
        else if (ext.endsWith(".flac")) mime = "audio/flac";
        else if (ext.endsWith(".ogg")) mime = "audio/ogg";

        byte[] fileBytes = readFileBytes(file);
        String b64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP);
        return "data:" + mime + ";base64," + b64;
    }

    private static byte[] readFileBytes(File file) throws IOException {
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        try {
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            if (read != data.length) {
                // Try reading fully
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                java.io.FileInputStream fis2 = new java.io.FileInputStream(file);
                while ((n = fis2.read(buf)) != -1) baos.write(buf, 0, n);
                fis2.close();
                return baos.toByteArray();
            }
            return data;
        } finally {
            fis.close();
        }
    }

    /**
     * Save audio data to a file.
     */
    public static String saveAudioFile(byte[] data, String format, File outputDir) throws IOException {
        String filename = "tts_" + System.currentTimeMillis() + "." + format;
        File outFile = new File(outputDir, filename);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(data);
        }
        return outFile.getAbsolutePath();
    }
}

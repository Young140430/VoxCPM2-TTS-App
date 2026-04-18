package com.voxcpm.tts;

public class Constants {
    // Status
    public static final String STATUS_IDLE = "idle";
    public static final String STATUS_GENERATING = "generating";
    public static final String STATUS_PLAYING = "playing";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_SUCCESS = "success";

    // SharedPreferences keys
    public static final String PREF_API_BASE = "apiBase";
    public static final String PREF_API_KEY = "apiKey";
    public static final String PREF_MODEL = "model";
    public static final String PREF_FORMAT = "format";

    // Defaults
    public static final String DEFAULT_API_BASE = "http://localhost:8000";
    public static final String DEFAULT_API_KEY = "sk-empty";
    public static final String DEFAULT_MODEL = "voxcpm2";
    public static final String DEFAULT_FORMAT = "wav";

    // Supported formats
    public static final String[] FORMATS = {"wav", "mp3", "flac", "ogg"};

    // Permission request code
    public static final int REQUEST_PERMISSIONS_CODE = 1001;
    public static final int REQUEST_AUDIO_FILE = 2001;
}

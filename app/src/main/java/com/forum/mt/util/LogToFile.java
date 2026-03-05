package com.forum.mt.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日志记录到文件
 */
public class LogToFile {
    private static final String TAG = "LogToFile";
    private static final String LOG_FILE_NAME = "app_log.txt";
    private static File logFile;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    
    public static void init(Context context) {
        logFile = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
        i(TAG, "Log file initialized: " + logFile.getAbsolutePath());
    }
    
    public static void d(String tag, String message) {
        Log.d(tag, message);
        writeToFile("D", tag, message);
    }
    
    public static void e(String tag, String message) {
        Log.e(tag, message);
        writeToFile("E", tag, message);
    }
    
    public static void e(String tag, String message, Throwable e) {
        Log.e(tag, message, e);
        writeToFile("E", tag, message + " - " + Log.getStackTraceString(e));
    }
    
    public static void i(String tag, String message) {
        Log.i(tag, message);
        writeToFile("I", tag, message);
    }
    
    private static void writeToFile(String level, String tag, String message) {
        if (logFile == null) return;
        
        try {
            FileWriter writer = new FileWriter(logFile, true);
            String time = dateFormat.format(new Date());
            writer.write(String.format("%s [%s] %s: %s\n", time, level, tag, message));
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log", e);
        }
    }
    
    public static String getLogFilePath() {
        return logFile != null ? logFile.getAbsolutePath() : "not initialized";
    }
    
    public static String readLogs() {
        if (logFile == null || !logFile.exists()) {
            return "No logs available";
        }
        
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(logFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            return "Error reading logs: " + e.getMessage();
        }
    }
    
    public static void clearLogs() {
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
    }
}
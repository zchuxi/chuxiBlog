package com.chuxi.common;

import java.util.regex.Pattern;

/** 输入内容净化工具：过滤危险 HTML 标签 */
public final class InputSanitizer {

    private static final Pattern SCRIPT_TAG = Pattern.compile("<\\s*/?\\s*script[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_BLOCK = Pattern.compile("<\\s*script[^>]*>.*?<\\s*/\\s*script\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IFRAME_TAG = Pattern.compile("<\\s*/?\\s*iframe[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ON_EVENT_ATTR = Pattern.compile("\\bon\\w+\\s*=\\s*[\"'][^\"']*[\"']", Pattern.CASE_INSENSITIVE);

    private InputSanitizer() {}

    /** 去除 script / iframe / on* 事件属性等危险片段 */
    public static String sanitize(String input) {
        if (input == null) return null;
        String cleaned = SCRIPT_BLOCK.matcher(input).replaceAll("");
        cleaned = SCRIPT_TAG.matcher(cleaned).replaceAll("");
        cleaned = IFRAME_TAG.matcher(cleaned).replaceAll("");
        cleaned = ON_EVENT_ATTR.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }
}

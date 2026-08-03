package com.chuxi.common;

import java.util.regex.Pattern;

/** 校验前端生成的匿名访客标识，避免把任意长字符串写入索引字段。 */
public final class VisitorIds {
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{16,64}");

    private VisitorIds() {}

    public static boolean isValid(String visitorId) {
        return visitorId != null && SAFE_ID.matcher(visitorId).matches();
    }
}

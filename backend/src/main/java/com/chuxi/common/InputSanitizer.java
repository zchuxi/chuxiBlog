package com.chuxi.common;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/** 输入内容净化工具：基于 OWASP 白名单策略过滤 HTML */
public final class InputSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "b", "em", "i", "u",
                           "a", "img", "ul", "ol", "li", "h1", "h2",
                           "h3", "h4", "h5", "h6", "blockquote", "code",
                           "pre", "hr", "table", "thead", "tbody", "tr",
                           "th", "td", "span", "div", "figure", "figcaption")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src", "alt", "title").onElements("img")
            .allowAttributes("class").globally()
            .allowUrlProtocols("http", "https")
            .toFactory();

    private InputSanitizer() {}

    /** 使用白名单策略净化 HTML 输入，仅保留安全标签和属性 */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) return "";
        return POLICY.sanitize(input);
    }

    /** 截断字符串到指定最大长度 */
    public static String truncate(String input, int maxLength) {
        if (input == null) return "";
        return input.length() <= maxLength ? input : input.substring(0, maxLength);
    }
}

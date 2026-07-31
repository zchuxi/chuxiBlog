package com.chuxi.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {

    @Test
    void sanitize_null_returnsEmpty() {
        assertThat(InputSanitizer.sanitize(null)).isEmpty();
    }

    @Test
    void sanitize_blank_returnsEmpty() {
        assertThat(InputSanitizer.sanitize("   ")).isEmpty();
    }

    @Test
    void sanitize_plainText_preserved() {
        assertThat(InputSanitizer.sanitize("Hello World")).isEqualTo("Hello World");
    }

    @Test
    void sanitize_allowedTags_preserved() {
        String input = "<p>Hello <strong>World</strong></p>";
        assertThat(InputSanitizer.sanitize(input)).contains("<p>").contains("<strong>");
    }

    @Test
    void sanitize_scriptTag_stripped() {
        String input = "<script>alert('xss')</script>Hello";
        String result = InputSanitizer.sanitize(input);
        assertThat(result).doesNotContain("<script>").doesNotContain("alert");
        assertThat(result).contains("Hello");
    }

    @Test
    void sanitize_svgOnload_stripped() {
        String input = "<svg onload=alert(1)>Hello</svg>";
        String result = InputSanitizer.sanitize(input);
        assertThat(result).doesNotContain("onload").doesNotContain("alert");
    }

    @Test
    void sanitize_imgOnerror_stripped() {
        String input = "<img src=x onerror=alert(1)>";
        String result = InputSanitizer.sanitize(input);
        assertThat(result).doesNotContain("onerror");
    }

    @Test
    void sanitize_javascriptUri_stripped() {
        String input = "<a href=\"javascript:alert(1)\">click</a>";
        String result = InputSanitizer.sanitize(input);
        assertThat(result).doesNotContain("javascript:");
    }

    @Test
    void sanitize_iframe_stripped() {
        String input = "<iframe src=\"https://evil.com\"></iframe>Hello";
        String result = InputSanitizer.sanitize(input);
        assertThat(result).doesNotContain("<iframe>");
        assertThat(result).contains("Hello");
    }

    @Test
    void sanitize_imgWithValidSrc_preserved() {
        String input = "<img src=\"https://example.com/photo.jpg\" alt=\"photo\">";
        String result = InputSanitizer.sanitize(input);
        assertThat(result).contains("src=").contains("alt=");
    }

    @Test
    void truncate_null_returnsEmpty() {
        assertThat(InputSanitizer.truncate(null, 20)).isEmpty();
    }

    @Test
    void truncate_shortString_unchanged() {
        assertThat(InputSanitizer.truncate("hello", 20)).isEqualTo("hello");
    }

    @Test
    void truncate_longString_truncated() {
        assertThat(InputSanitizer.truncate("1234567890123456789012345", 20)).hasSize(20);
        assertThat(InputSanitizer.truncate("1234567890123456789012345", 20)).isEqualTo("12345678901234567890");
    }

    @Test
    void truncate_exactLength_unchanged() {
        assertThat(InputSanitizer.truncate("12345678901234567890", 20)).isEqualTo("12345678901234567890");
    }
}

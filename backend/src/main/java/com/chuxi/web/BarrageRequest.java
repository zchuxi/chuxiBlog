package com.chuxi.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BarrageRequest {
    @Size(max = 20, message = "昵称长度1-20字符")
    private String nickname;

    @NotBlank(message = "内容不能为空")
    @Size(max = 500, message = "内容最多500字符")
    private String content;

    @Size(max = 10, message = "表情最多10字符")
    private String mood;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
}

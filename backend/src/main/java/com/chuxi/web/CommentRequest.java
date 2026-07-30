package com.chuxi.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequest {
    @Size(max = 20, message = "昵称长度最多20字符")
    private String nickname;

    @NotBlank(message = "内容不能为空")
    @Size(max = 500, message = "内容最多500字符")
    private String content;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

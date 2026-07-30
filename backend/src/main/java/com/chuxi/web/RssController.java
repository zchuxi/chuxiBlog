package com.chuxi.web;

import com.chuxi.entity.Article;
import com.chuxi.repo.ArticleRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RssController {

    private final ArticleRepo articleRepo;

    public RssController(ArticleRepo articleRepo) {
        this.articleRepo = articleRepo;
    }

    @GetMapping(value = "/rss", produces = "application/atom+xml;charset=UTF-8")
    public String atom() {
        List<Article> articles = articleRepo.findAllPublished();
        List<Article> latest = articles.stream().limit(20).toList();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<feed xmlns=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <title>初曦の巢</title>\n");
        xml.append("  <subtitle>初曦の巢 - 个人博客</subtitle>\n");
        xml.append("  <link href=\"https://chuxi.online\" rel=\"alternate\"/>\n");
        xml.append("  <link href=\"/api/rss\" rel=\"self\"/>\n");
        xml.append("  <id>https://chuxi.online</id>\n");
        xml.append("  <updated>").append(formatAtomDate(
                latest.isEmpty() ? java.time.LocalDateTime.now() : latest.get(0).getUpdatedAt()
        )).append("</updated>\n");

        for (Article a : latest) {
            xml.append("  <entry>\n");
            xml.append("    <title>").append(escapeXml(a.getTitle())).append("</title>\n");
            xml.append("    <link href=\"https://chuxi.online/article/").append(a.getId()).append("\"/>\n");
            xml.append("    <id>https://chuxi.online/article/").append(a.getId()).append("</id>\n");
            if (a.getUpdatedAt() != null) {
                xml.append("    <updated>").append(formatAtomDate(a.getUpdatedAt())).append("</updated>\n");
            }
            if (a.getPublishedAt() != null) {
                xml.append("    <published>").append(formatAtomDate(a.getPublishedAt())).append("</published>\n");
            }
            if (a.getSummary() != null) {
                xml.append("    <summary type=\"text\">").append(escapeXml(a.getSummary())).append("</summary>\n");
            }
            xml.append("  </entry>\n");
        }

        xml.append("</feed>\n");
        return xml.toString();
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String formatAtomDate(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        return dt.atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}

package com.chuxi.init;

import com.chuxi.common.VisitorIds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chuxi.entity.*;
import com.chuxi.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/** 幂等种子数据：对应表为空时才导入 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ObjectMapper mapper;
    private final ArticleRepo articleRepo;
    private final HomeCarouselRepo homeCarouselRepo;
    private final CollapseCardRepo collapseCardRepo;
    private final TeamMemberRepo teamMemberRepo;
    private final ArchiveCategoryRepo archiveCategoryRepo;
    private final TimelineCarouselRepo timelineCarouselRepo;
    private final TimelineEventRepo timelineEventRepo;
    private final ParallaxStoryRepo parallaxStoryRepo;
    private final ToolSiteRepo toolSiteRepo;
    private final BarrageRepo barrageRepo;
    private final CalledTextRepo calledTextRepo;
    private final MusicRepo musicRepo;
    private final BangumiRecordRepo bangumiRecordRepo;
    private final SiteContentRepo siteContentRepo;

    public DataInitializer(ObjectMapper mapper, ArticleRepo articleRepo,
                           HomeCarouselRepo homeCarouselRepo, CollapseCardRepo collapseCardRepo,
                           TeamMemberRepo teamMemberRepo, ArchiveCategoryRepo archiveCategoryRepo,
                           TimelineCarouselRepo timelineCarouselRepo, TimelineEventRepo timelineEventRepo,
                           ParallaxStoryRepo parallaxStoryRepo, ToolSiteRepo toolSiteRepo,
                           BarrageRepo barrageRepo, CalledTextRepo calledTextRepo,
                           MusicRepo musicRepo, BangumiRecordRepo bangumiRecordRepo,
                           SiteContentRepo siteContentRepo) {
        this.mapper = mapper;
        this.articleRepo = articleRepo;
        this.homeCarouselRepo = homeCarouselRepo;
        this.collapseCardRepo = collapseCardRepo;
        this.teamMemberRepo = teamMemberRepo;
        this.archiveCategoryRepo = archiveCategoryRepo;
        this.timelineCarouselRepo = timelineCarouselRepo;
        this.timelineEventRepo = timelineEventRepo;
        this.parallaxStoryRepo = parallaxStoryRepo;
        this.toolSiteRepo = toolSiteRepo;
        this.barrageRepo = barrageRepo;
        this.calledTextRepo = calledTextRepo;
        this.musicRepo = musicRepo;
        this.bangumiRecordRepo = bangumiRecordRepo;
        this.siteContentRepo = siteContentRepo;
    }

    @Override
    public void run(String... args) {
        ensureVisitorSecret();
        seed("articles.json", articleRepo, new TypeReference<List<Article>>() {});
        seed("home-carousels.json", homeCarouselRepo, new TypeReference<List<HomeCarousel>>() {});
        seed("collapse-cards.json", collapseCardRepo, new TypeReference<List<CollapseCard>>() {});
        seed("team-members.json", teamMemberRepo, new TypeReference<List<TeamMember>>() {});
        seed("archive-categories.json", archiveCategoryRepo, new TypeReference<List<ArchiveCategory>>() {});
        seed("timeline-carousels.json", timelineCarouselRepo, new TypeReference<List<TimelineCarousel>>() {});
        seed("timeline-events.json", timelineEventRepo, new TypeReference<List<TimelineEvent>>() {});
        seed("parallax-stories.json", parallaxStoryRepo, new TypeReference<List<ParallaxStory>>() {});
        seed("tool-sites.json", toolSiteRepo, new TypeReference<List<ToolSite>>() {});
        seed("barrages.json", barrageRepo, new TypeReference<List<Barrage>>() {});
        seed("called-texts.json", calledTextRepo, new TypeReference<List<CalledText>>() {});
        seed("musics.json", musicRepo, new TypeReference<List<Music>>() {});
        seed("bangumi-records.json", bangumiRecordRepo, new TypeReference<List<BangumiRecord>>() {});
        seed("site-contents.json", siteContentRepo, new TypeReference<List<SiteContent>>() {});
    }

    /**
     * visitor 签名密钥：持久化到 site_content（key=visitor-secret），重启不失效。
     * 客户端无法自行构造合法访客标识，只能通过签发接口获取。
     */
    private void ensureVisitorSecret() {
        try {
            final String KEY = "visitor-secret";
            String hex = siteContentRepo.findByContentKey(KEY)
                    .map(SiteContent::getContentJson)
                    .orElse(null);
            if (hex == null || hex.length() < 32) {
                byte[] buf = new byte[32];
                new SecureRandom().nextBytes(buf);
                hex = HexFormat.of().formatHex(buf);
                SiteContent sc = new SiteContent();
                sc.setContentKey(KEY);
                sc.setContentJson(hex);
                sc.setUpdatedAt(LocalDateTime.now());
                siteContentRepo.save(sc);
                log.info("已生成并持久化 visitor 签名密钥");
            }
            VisitorIds.init(HexFormat.of().parseHex(hex));
        } catch (Exception e) {
            log.error("visitor 签名密钥初始化失败：{}", e.getMessage());
        }
    }

    private <T> void seed(String file, JpaRepository<T, Long> repo, TypeReference<List<T>> type) {
        try {
            if (repo.count() > 0) return;
            try (InputStream in = new ClassPathResource("seed/" + file).getInputStream()) {
                List<T> list = mapper.readValue(in, type);
                repo.saveAll(list);
                log.info("seeded {} rows from {}", list.size(), file);
            }
        } catch (Exception e) {
            log.error("seed {} failed: {}", file, e.getMessage());
        }
    }
}

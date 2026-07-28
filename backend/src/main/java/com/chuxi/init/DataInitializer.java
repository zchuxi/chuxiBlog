package com.chuxi.init;

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

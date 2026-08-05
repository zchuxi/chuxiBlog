package com.chuxi.web;

import com.chuxi.common.PageData;
import com.chuxi.common.R;
import com.chuxi.repo.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/front/home")
@Transactional(readOnly = true)
public class HomeController {

    private final HomeCarouselRepo carouselRepo;
    private final CollapseCardRepo collapseCardRepo;
    private final ArticleRepo articleRepo;
    private final TeamMemberRepo teamMemberRepo;

    public HomeController(HomeCarouselRepo carouselRepo, CollapseCardRepo collapseCardRepo,
                          ArticleRepo articleRepo, TeamMemberRepo teamMemberRepo) {
        this.carouselRepo = carouselRepo;
        this.collapseCardRepo = collapseCardRepo;
        this.articleRepo = articleRepo;
        this.teamMemberRepo = teamMemberRepo;
    }

    @GetMapping("/landing")
    public R<Map<String, Object>> landing() {
        var carousels = carouselRepo.findVisibleOrderBySortIndexDesc();
        var cards = collapseCardRepo.findAllByOrderBySortIndexDesc();
        // 列表投影：不加载 LONGTEXT 正文，避免每次 landing 全量载入
        var published = articleRepo.findPublishedLite();
        var articles = published.stream().limit(6).map(Dtos.ArticleItem::fromLite).toList();
        long categoryCount = published.stream()
                .flatMap(a -> java.util.stream.Stream.of(a.getCategoryName(), a.getArchiveCategory()))
                .filter(c -> c != null && !c.isBlank())
                .distinct().count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("carousels", carousels);
        data.put("collapseCards", cards);
        data.put("articles", articles);
        data.put("stats", Map.of(
                "articleCount", published.size(),
                "categoryCount", categoryCount,
                "carouselCount", carousels.size(),
                "collapseCardCount", cards.size()
        ));
        return R.ok(data);
    }

    @GetMapping("/articles")
    public R<PageData<Dtos.ArticleItem>> articles(@RequestParam(defaultValue = "1") int pageNo,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 50) return R.fail("分页参数无效");
        var pageable = PageRequest.of(pageNo - 1, pageSize);
        var result = articleRepo.findPublishedPage(pageable);
        var items = result.getContent().stream().map(Dtos.ArticleItem::of).toList();
        return R.ok(new PageData<>(items, result.getTotalElements(), pageNo, pageSize));
    }

    @GetMapping("/team-members")
    public R<List<?>> teamMembers() {
        return R.ok(teamMemberRepo.findAll());
    }
}

package com.lxblog.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxblog.common.R;
import com.lxblog.entity.*;
import com.lxblog.repo.*;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 后台管理统一 CRUD：/api/admin/{res}，按路径段分发到各资源处理器 */
@RestController
@RequestMapping("/api/admin")
public class AdminContentController {

    private final ObjectMapper mapper;
    private final CommentRepo commentRepo;
    private final Map<String, ResourceHandler<?>> handlers = new LinkedHashMap<>();

    public AdminContentController(ObjectMapper springMapper,
                                  ArticleRepo articleRepo,
                                  HomeCarouselRepo homeCarouselRepo,
                                  CollapseCardRepo collapseCardRepo,
                                  TeamMemberRepo teamMemberRepo,
                                  ArchiveCategoryRepo archiveCategoryRepo,
                                  TimelineCarouselRepo timelineCarouselRepo,
                                  TimelineEventRepo timelineEventRepo,
                                  ParallaxStoryRepo parallaxStoryRepo,
                                  ToolSiteRepo toolSiteRepo,
                                  BarrageRepo barrageRepo,
                                  CalledTextRepo calledTextRepo,
                                  MusicRepo musicRepo,
                                  CommentRepo commentRepo,
                                  BangumiRecordRepo bangumiRecordRepo) {
        // 复制一份 Spring 的 ObjectMapper，容忍未知字段
        this.mapper = springMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.commentRepo = commentRepo;
        handlers.put("articles", new ResourceHandler<>(articleRepo, Article.class, false, true, false));
        handlers.put("home-carousels", new ResourceHandler<>(homeCarouselRepo, HomeCarousel.class, false, false, false));
        handlers.put("collapse-cards", new ResourceHandler<>(collapseCardRepo, CollapseCard.class, false, false, false));
        handlers.put("team-members", new ResourceHandler<>(teamMemberRepo, TeamMember.class, false, false, false));
        handlers.put("archive-categories", new ResourceHandler<>(archiveCategoryRepo, ArchiveCategory.class, true, true, false));
        handlers.put("timeline-carousels", new ResourceHandler<>(timelineCarouselRepo, TimelineCarousel.class, false, false, false));
        handlers.put("timeline-events", new ResourceHandler<>(timelineEventRepo, TimelineEvent.class, false, false, false));
        handlers.put("parallax-stories", new ResourceHandler<>(parallaxStoryRepo, ParallaxStory.class, false, false, false));
        handlers.put("tool-sites", new ResourceHandler<>(toolSiteRepo, ToolSite.class, false, true, false));
        handlers.put("barrages", new ResourceHandler<>(barrageRepo, Barrage.class, true, false, true));
        handlers.put("called-texts", new ResourceHandler<>(calledTextRepo, CalledText.class, false, false, false));
        handlers.put("musics", new ResourceHandler<>(musicRepo, Music.class, false, false, false));
        handlers.put("comments", new ResourceHandler<>(commentRepo, Comment.class, true, false, true));
        handlers.put("bangumi-records", new ResourceHandler<>(bangumiRecordRepo, BangumiRecord.class, true, true, true));
    }

    @GetMapping("/{res}")
    @Transactional(readOnly = true)
    public R<?> list(@PathVariable String res) {
        ResourceHandler<?> h = handlers.get(res);
        if (h == null) return R.fail("未知资源: " + res);
        return R.ok(h.list());
    }

    @PostMapping("/{res}")
    @Transactional
    public R<?> create(@PathVariable String res, @RequestBody Map<String, Object> body) {
        ResourceHandler<?> h = handlers.get(res);
        if (h == null) return R.fail("未知资源: " + res);
        return R.ok(h.create(body));
    }

    @PutMapping("/{res}/{id}")
    @Transactional
    public R<?> update(@PathVariable String res, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        ResourceHandler<?> h = handlers.get(res);
        if (h == null) return R.fail("未知资源: " + res);
        Object saved = h.update(id, body);
        return saved == null ? R.fail("记录不存在") : R.ok(saved);
    }

    @DeleteMapping("/{res}/{id}")
    @Transactional
    public R<Object> delete(@PathVariable String res, @PathVariable Long id) {
        ResourceHandler<?> h = handlers.get(res);
        if (h == null) return R.fail("未知资源: " + res);
        // 删文章时顺带删除其全部评论
        if ("articles".equals(res)) commentRepo.deleteByArticleId(id);
        h.delete(id);
        return R.ok(null);
    }

    /** 单个资源的 CRUD 封装 */
    private class ResourceHandler<T> {
        private final JpaRepository<T, Long> repo;
        private final Class<T> type;
        private final boolean autoId;  // 数据库自增，新建时无需 max(id)+1 兜底
        private final boolean hasTags; // tags 在 API 层为数组、DB 为 CSV
        private final boolean desc;    // 列表按 id 降序

        ResourceHandler(JpaRepository<T, Long> repo, Class<T> type, boolean autoId, boolean hasTags, boolean desc) {
            this.repo = repo;
            this.type = type;
            this.autoId = autoId;
            this.hasTags = hasTags;
            this.desc = desc;
        }

        List<Object> list() {
            Sort sort = Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, "id");
            return repo.findAll(sort).stream().map(this::toOut).toList();
        }

        Object create(Map<String, Object> body) {
            T entity = toEntity(body);
            BeanWrapper bw = new BeanWrapperImpl(entity);
            if (!autoId && bw.getPropertyValue("id") == null) {
                bw.setPropertyValue("id", nextId());
            }
            fillTimes(bw, null);
            return toOut(repo.save(entity));
        }

        Object update(Long id, Map<String, Object> body) {
            T old = repo.findById(id).orElse(null);
            if (old == null) return null;
            T entity = toEntity(body);
            BeanWrapper bw = new BeanWrapperImpl(entity);
            bw.setPropertyValue("id", id);
            fillTimes(bw, new BeanWrapperImpl(old));
            return toOut(repo.save(entity));
        }

        void delete(Long id) {
            if (repo.existsById(id)) repo.deleteById(id);
        }

        /** 无自增实体的兜底 id：max(id)+1 */
        private long nextId() {
            return repo.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().findFirst()
                    .map(e -> (Long) new BeanWrapperImpl(e).getPropertyValue("id"))
                    .orElse(0L) + 1;
        }

        /** createdAt 入参为空保留原值（新建取当前时间）；updatedAt 入参为空取当前时间 */
        private void fillTimes(BeanWrapper bw, BeanWrapper old) {
            if (bw.isWritableProperty("createdAt") && bw.getPropertyValue("createdAt") == null) {
                Object kept = old != null ? old.getPropertyValue("createdAt") : LocalDateTime.now();
                bw.setPropertyValue("createdAt", kept);
            }
            if (bw.isWritableProperty("updatedAt") && bw.getPropertyValue("updatedAt") == null) {
                bw.setPropertyValue("updatedAt", LocalDateTime.now());
            }
        }

        /** 入参 Map -> 实体：tags 数组先转 CSV，实体字段是 String 无法直接反序列化 */
        private T toEntity(Map<String, Object> body) {
            Map<String, Object> copy = new LinkedHashMap<>(body);
            if (hasTags && copy.get("tags") instanceof List<?> arr) {
                copy.put("tags", arr.stream().map(String::valueOf).collect(Collectors.joining(",")));
            }
            return mapper.convertValue(copy, type);
        }

        /** 出参：tags CSV -> 数组，其余字段原样返回 */
        private Object toOut(T entity) {
            if (!hasTags) return entity;
            Map<String, Object> m = mapper.convertValue(entity, new TypeReference<LinkedHashMap<String, Object>>() {});
            m.put("tags", Dtos.splitTags((String) m.get("tags")));
            return m;
        }
    }
}

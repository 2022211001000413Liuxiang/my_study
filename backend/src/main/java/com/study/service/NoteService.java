package com.study.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.config.StudyProperties;
import com.study.model.CategoryDto;
import com.study.model.GraphDto;
import com.study.model.GraphEdgeDto;
import com.study.model.GraphNodeDto;
import com.study.model.HeadingDto;
import com.study.model.NoteDto;
import com.study.model.NoteSummary;
import com.study.model.NotesResponse;
import com.study.model.ReviewDayDto;
import com.study.model.ReviewLevelStatsDto;
import com.study.model.ReviewOverviewDto;
import com.study.model.ReviewQueueDto;
import com.study.model.StatsDto;
import com.study.model.TagDto;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class NoteService {
  private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.+)$");
  private static final Pattern INVALID_WINDOWS_NAME = Pattern.compile("[<>:\"\\\\|?*\\x00-\\x1F]");
  private static final Pattern MARKDOWN_LINK = Pattern.compile("(?<!!)\\[[^]]+?]\\(([^)#]+)(?:#[^)]+)?\\)");
  private static final Pattern OBSIDIAN_LINK = Pattern.compile("(?<!!?)\\[\\[([^]#|]+)(?:#[^]|]+)?(?:\\|[^]]+)?]]");
  private static final Pattern KEYWORD = Pattern.compile("[\\p{IsHan}]{2,}|[a-zA-Z][a-zA-Z0-9_]{2,}");

  private final StudyProperties properties;
  private final ObjectMapper objectMapper;
  private Path workspaceRoot;
  private Path mdRoot;
  private Path metaPath;
  private List<NoteSummary> indexCache = List.of();
  private MetaStore metaCache = new MetaStore();

  public NoteService(StudyProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public synchronized void init() throws IOException {
    workspaceRoot = discoverWorkspaceRoot();
    mdRoot = resolveConfiguredPath(properties.getMdRoot(), "md");
    metaPath = resolveConfiguredPath(properties.getMetaPath(), ".study-meta.json");
    Files.createDirectories(mdRoot);
    loadMeta();
    rebuildIndex();
  }

  public synchronized NotesResponse list(String category, String query, String review) throws IOException {
    String normalizedCategory = category == null ? "" : category.trim();
    String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    String reviewFilter = normalizeReviewFilter(review);
    LocalDate today = LocalDate.now();
    List<NoteSummary> notes = indexCache;

    if (!normalizedCategory.isBlank() && !"all".equals(normalizedCategory)) {
      notes = notes.stream()
          .filter(note -> note.category().equals(normalizedCategory)
              || note.path().startsWith(normalizedCategory + "/"))
          .toList();
    }

    if (!"all".equals(reviewFilter)) {
      notes = notes.stream()
          .filter(note -> matchesReviewFilter(note, reviewFilter, today))
          .toList();
    }

    if (!q.isBlank()) {
      List<NoteSummary> matched = new ArrayList<>();
      for (NoteSummary note : notes) {
        String markdown = Files.readString(note.absolutePath(), StandardCharsets.UTF_8);
        String haystack = (note.title() + " " + note.path() + " " + String.join(" ", note.tags()) + " " + markdown)
            .toLowerCase(Locale.ROOT);
        if (haystack.contains(q)) {
          matched.add(note);
        }
      }
      notes = matched;
    }

    return new NotesResponse(
        notes.stream().map(NoteSummary::toDto).toList(),
        buildCategories(indexCache),
        buildStats(indexCache)
    );
  }

  public synchronized NoteDetail read(String id) throws IOException {
    NoteSummary note = findNote(id).orElseThrow(() -> new NotFoundException("笔记不存在。"));
    String raw = Files.readString(note.absolutePath(), StandardCharsets.UTF_8);
    return new NoteDetail(note.toDto(), rewriteMarkdownAssets(raw, note.absolutePath()), raw);
  }

  public synchronized ReviewQueueDto reviewsToday() {
    return buildReviewQueue(LocalDate.now());
  }

  public synchronized ReviewOverviewDto reviewOverview() {
    return buildReviewOverview(LocalDate.now());
  }

  public synchronized GraphDto graph() throws IOException {
    return buildGraph(LocalDate.now());
  }

  public synchronized ReviewQueueDto submitReview(String id, String level) throws IOException {
    NoteSummary note = findNote(id).orElseThrow(() -> new NotFoundException("笔记不存在。"));
    String normalizedLevel = normalizeReviewLevel(level);
    LocalDate today = LocalDate.now();
    NoteMeta meta = metaCache.notes.getOrDefault(note.id(), new NoteMeta());
    int nextCount = Math.max(0, meta.reviewCount) + 1;
    meta.reviewCount = nextCount;
    meta.lastReviewedAt = Instant.now().toString();
    meta.nextReviewAt = today.plusDays(reviewIntervalDays(normalizedLevel, nextCount)).toString();
    meta.reviewLevel = normalizedLevel;
    metaCache.notes.put(note.id(), meta);
    saveMeta();
    rebuildIndex();
    return buildReviewQueue(today);
  }

  public synchronized NoteDto create(String title, String category, String markdown) throws IOException {
    String cleanTitle = sanitizeTitle(title == null || title.isBlank() ? "新的学习记录" : title);
    String cleanCategory = sanitizeCategory(category == null || category.isBlank() ? "inbox" : category);
    String body = markdown == null || markdown.isBlank() ? "# " + cleanTitle + "\n\n" : markdown;
    Path filePath = uniqueNotePath(cleanCategory, cleanTitle + ".md", null);
    Files.createDirectories(filePath.getParent());
    Files.writeString(filePath, body, StandardCharsets.UTF_8);
    rebuildIndex();
    return indexCache.stream()
        .filter(note -> note.absolutePath().equals(filePath))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("笔记创建失败。"))
        .toDto();
  }

  public synchronized NoteDto update(String id, String markdown, MetaPatch patch) throws IOException {
    NoteSummary note = findNote(id).orElseThrow(() -> new NotFoundException("笔记不存在。"));
    Files.writeString(note.absolutePath(), markdown == null ? "" : markdown, StandardCharsets.UTF_8);
    NoteMeta meta = metaCache.notes.getOrDefault(note.id(), new NoteMeta());
    meta.favorite = patch != null && patch.favorite;
    meta.status = normalizeStatus(patch == null ? null : patch.status);
    meta.tags = patch == null || patch.tags == null ? List.of() : unique(patch.tags);
    metaCache.notes.put(note.id(), meta);
    saveMeta();
    rebuildIndex();
    return findNote(id).orElseThrow(() -> new NotFoundException("笔记不存在。")).toDto();
  }

  public synchronized NoteDto move(String id, String title, String category) throws IOException {
    NoteSummary note = findNote(id).orElseThrow(() -> new NotFoundException("笔记不存在。"));
    String nextTitle = sanitizeTitle(title == null || title.isBlank() ? note.title() : title);
    String nextCategory = sanitizeCategory(category == null || category.isBlank() ? note.category() : category);
    Path target = uniqueNotePath(nextCategory, nextTitle + ".md", note.absolutePath());
    assertInsideMd(target);
    Files.createDirectories(target.getParent());
    Files.move(note.absolutePath(), target, StandardCopyOption.REPLACE_EXISTING);

    NoteMeta oldMeta = metaCache.notes.remove(note.id());
    NoteMeta nextMeta = oldMeta == null ? new NoteMeta() : oldMeta;
    nextMeta.title = nextTitle;
    metaCache.notes.put(idFromAbsolutePath(target), nextMeta);
    saveMeta();
    rebuildIndex();
    return indexCache.stream()
        .filter(item -> item.absolutePath().equals(target))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("笔记移动失败。"))
        .toDto();
  }

  public synchronized void delete(String id) throws IOException {
    NoteSummary note = findNote(id).orElseThrow(() -> new NotFoundException("笔记不存在。"));
    assertInsideMd(note.absolutePath());
    Files.deleteIfExists(note.absolutePath());
    metaCache.notes.remove(note.id());
    saveMeta();
    rebuildIndex();
  }

  public synchronized Optional<Path> resolveAssetPath(String rawPath) {
    String relative = decode(rawPath == null ? "" : rawPath).replace('\\', '/');
    Path assetPath = mdRoot.resolve(relative).normalize();
    if (!isInside(mdRoot, assetPath) || !Files.isRegularFile(assetPath)) {
      return Optional.empty();
    }
    return Optional.of(assetPath);
  }

  private void loadMeta() {
    if (!Files.isRegularFile(metaPath)) {
      metaCache = new MetaStore();
      return;
    }
    try {
      metaCache = objectMapper.readValue(metaPath.toFile(), MetaStore.class);
      if (metaCache.notes == null) {
        metaCache.notes = new LinkedHashMap<>();
      }
    } catch (IOException ignored) {
      metaCache = new MetaStore();
    }
  }

  private void saveMeta() throws IOException {
    if (metaPath.getParent() != null) {
      Files.createDirectories(metaPath.getParent());
    }
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(metaPath.toFile(), metaCache);
  }

  private void rebuildIndex() throws IOException {
    List<Path> files = listMarkdownFiles();
    List<NoteSummary> summaries = new ArrayList<>();
    for (Path file : files) {
      summaries.add(readNoteSummary(file));
    }
    summaries.sort(Comparator.comparing(NoteSummary::updatedAt).reversed());
    indexCache = List.copyOf(summaries);
  }

  private List<Path> listMarkdownFiles() throws IOException {
    List<Path> files = new ArrayList<>();
    Files.walkFileTree(mdRoot, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile() && file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md")) {
          files.add(file.toAbsolutePath().normalize());
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return files;
  }

  private NoteSummary readNoteSummary(Path absolutePath) throws IOException {
    assertInsideMd(absolutePath);
    String markdown = Files.readString(absolutePath, StandardCharsets.UTF_8);
    BasicFileAttributes attrs = Files.readAttributes(absolutePath, BasicFileAttributes.class);
    String id = idFromAbsolutePath(absolutePath);
    String relativePath = toPosix(mdRoot.relativize(absolutePath));
    String dirname = toPosix(mdRoot.relativize(absolutePath).getParent());
    String category = dirname.isBlank() ? "inbox" : dirname;
    List<HeadingDto> headings = extractHeadings(markdown);
    Map<String, Object> frontmatter = parseFrontmatter(markdown);
    NoteMeta meta = metaCache.notes.getOrDefault(id, new NoteMeta());
    String title = firstNonBlank(meta.title, asString(frontmatter.get("title")),
        headings.isEmpty() ? null : headings.get(0).text(),
        stripMdExtension(absolutePath.getFileName().toString()));
    List<String> tags = unique(joinLists(asStringList(frontmatter.get("tags")), meta.tags, List.of(category.split("/"))));
    return new NoteSummary(
        id,
        absolutePath,
        relativePath,
        title,
        category,
        tags,
        headings,
        Instant.ofEpochMilli(attrs.lastModifiedTime().toMillis()).toString(),
        countWords(markdown),
        Boolean.TRUE.equals(meta.favorite),
        normalizeStatus(meta.status),
        Math.max(0, meta.reviewCount),
        meta.lastReviewedAt,
        meta.nextReviewAt,
        normalizeReviewLevel(meta.reviewLevel)
    );
  }

  private List<HeadingDto> extractHeadings(String markdown) {
    List<HeadingDto> headings = new ArrayList<>();
    for (String line : markdown.split("\\R")) {
      Matcher matcher = HEADING.matcher(line);
      if (matcher.matches()) {
        String text = matcher.group(2).replaceAll("[#*`]", "").trim();
        headings.add(new HeadingDto(matcher.group(1).length(), text, slugify(text)));
      }
      if (headings.size() >= 32) {
        break;
      }
    }
    return headings;
  }

  private Map<String, Object> parseFrontmatter(String markdown) {
    if (!markdown.startsWith("---")) {
      return Map.of();
    }
    int end = markdown.indexOf("\n---", 3);
    if (end < 0) {
      return Map.of();
    }
    Map<String, Object> data = new LinkedHashMap<>();
    String block = markdown.substring(3, end).trim();
    for (String line : block.split("\\R")) {
      int splitAt = line.indexOf(':');
      if (splitAt < 0) {
        continue;
      }
      String key = line.substring(0, splitAt).trim();
      String value = line.substring(splitAt + 1).trim();
      if ("tags".equals(key)) {
        data.put(key, List.of(value.replaceAll("^\\[|]$", "").split("[,\\s]+")));
      } else {
        data.put(key, value);
      }
    }
    return data;
  }

  private String rewriteMarkdownAssets(String markdown, Path absolutePath) {
    Path noteDir = absolutePath.getParent();
    String obsidian = replaceObsidianImages(markdown, noteDir);
    Pattern markdownImage = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");
    Matcher matcher = markdownImage.matcher(obsidian);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String alt = matcher.group(1);
      String src = matcher.group(2).trim();
      if (src.matches("^[a-zA-Z]+://.*") || src.startsWith("/api/assets/")) {
        matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
        continue;
      }
      Optional<Path> asset = resolveAsset(src, noteDir);
      if (asset.isEmpty()) {
        matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
      } else {
        String next = "![" + alt + "](/api/assets/" + encodePath(toPosix(mdRoot.relativize(asset.get()))) + ")";
        matcher.appendReplacement(buffer, Matcher.quoteReplacement(next));
      }
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private String replaceObsidianImages(String markdown, Path noteDir) {
    Pattern obsidianImage = Pattern.compile("!\\[\\[([^]]+)]]");
    Matcher matcher = obsidianImage.matcher(markdown);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String raw = matcher.group(1);
      Optional<Path> asset = resolveAsset(raw, noteDir);
      String replacement = asset
          .map(path -> "![" + Path.of(raw.split("\\|")[0]).getFileName() + "](/api/assets/"
              + encodePath(toPosix(mdRoot.relativize(path))) + ")")
          .orElse("> 图片未找到或位于 md 目录外：" + raw);
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private Optional<Path> resolveAsset(String raw, Path noteDir) {
    String cleaned = decode(raw.split("\\|")[0].trim());
    List<Path> candidates = List.of(
        noteDir.resolve(cleaned).normalize(),
        noteDir.resolve("attachments").resolve(cleaned).normalize(),
        mdRoot.resolve(cleaned).normalize()
    );
    return candidates.stream().filter(path -> isInside(mdRoot, path) && Files.isRegularFile(path)).findFirst();
  }

  private Optional<NoteSummary> findNote(String id) {
    return indexCache.stream().filter(note -> note.id().equals(id)).findFirst();
  }

  private List<CategoryDto> buildCategories(List<NoteSummary> notes) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (NoteSummary note : notes) {
      counts.put(note.category(), counts.getOrDefault(note.category(), 0) + 1);
    }
    return counts.entrySet().stream()
        .map(entry -> new CategoryDto(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(CategoryDto::name, CollatorHolder.ZH_CN))
        .toList();
  }

  private StatsDto buildStats(List<NoteSummary> notes) {
    Map<String, Integer> tags = new LinkedHashMap<>();
    int words = 0;
    int favorites = 0;
    int dueReviews = 0;
    LocalDate today = LocalDate.now();
    for (NoteSummary note : notes) {
      words += note.wordCount();
      if (note.favorite()) {
        favorites += 1;
      }
      if (isDue(note.nextReviewAt(), today)) {
        dueReviews += 1;
      }
      for (String tag : note.tags()) {
        tags.put(tag, tags.getOrDefault(tag, 0) + 1);
      }
    }
    List<TagDto> tagDtos = tags.entrySet().stream()
        .map(entry -> new TagDto(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparingInt(TagDto::count).reversed())
        .limit(18)
        .toList();
    return new StatsDto(notes.size(), words, favorites, dueReviews, tagDtos);
  }

  private ReviewQueueDto buildReviewQueue(LocalDate today) {
    List<NoteSummary> due = new ArrayList<>();
    int reviewedToday = 0;
    LocalDate nextReviewAt = null;

    for (NoteSummary note : indexCache) {
      if (isReviewedOn(note.lastReviewedAt(), today)) {
        reviewedToday += 1;
      }
      if (isDue(note.nextReviewAt(), today)) {
        due.add(note);
        continue;
      }
      LocalDate nextDate = parseDate(note.nextReviewAt()).orElse(null);
      if (nextDate != null && (nextReviewAt == null || nextDate.isBefore(nextReviewAt))) {
        nextReviewAt = nextDate;
      }
    }

    due.sort(Comparator
        .comparing((NoteSummary note) -> parseDate(note.nextReviewAt()).orElse(LocalDate.MIN))
        .thenComparing(NoteSummary::updatedAt, Comparator.reverseOrder()));
    return new ReviewQueueDto(
        due.stream().map(NoteSummary::toDto).toList(),
        due.size(),
        reviewedToday,
        nextReviewAt == null ? null : nextReviewAt.toString()
    );
  }

  private ReviewOverviewDto buildReviewOverview(LocalDate today) {
    Map<LocalDate, Integer> completed = new LinkedHashMap<>();
    Map<LocalDate, Integer> upcoming = new LinkedHashMap<>();
    for (int i = 13; i >= 0; i -= 1) {
      completed.put(today.minusDays(i), 0);
    }
    for (int i = 0; i < 30; i += 1) {
      upcoming.put(today.plusDays(i), 0);
    }

    int dueToday = 0;
    int reviewedToday = 0;
    int unreviewed = 0;
    int again = 0;
    int normal = 0;
    int easy = 0;
    int scheduled = 0;
    List<NoteSummary> recentReviewed = new ArrayList<>();

    for (NoteSummary note : indexCache) {
      if (isDue(note.nextReviewAt(), today)) {
        dueToday += 1;
      }
      if (note.lastReviewedAt() == null || note.lastReviewedAt().isBlank()) {
        unreviewed += 1;
      }
      if ("again".equals(note.reviewLevel())) {
        again += 1;
      } else if ("normal".equals(note.reviewLevel())) {
        normal += 1;
      } else if ("easy".equals(note.reviewLevel())) {
        easy += 1;
      }
      if (parseDate(note.nextReviewAt()).map(date -> date.isAfter(today)).orElse(false)) {
        scheduled += 1;
      }

      Optional<LocalDate> reviewedDate = reviewedDate(note.lastReviewedAt());
      if (reviewedDate.isPresent()) {
        LocalDate date = reviewedDate.get();
        if (completed.containsKey(date)) {
          completed.put(date, completed.get(date) + 1);
        }
        if (date.equals(today)) {
          reviewedToday += 1;
        }
        recentReviewed.add(note);
      }

      Optional<LocalDate> nextDate = parseDate(note.nextReviewAt());
      if (nextDate.isPresent() && upcoming.containsKey(nextDate.get())) {
        LocalDate date = nextDate.get();
        upcoming.put(date, upcoming.get(date) + 1);
      }
    }

    recentReviewed.sort(Comparator
        .comparing(NoteSummary::lastReviewedAt, Comparator.nullsLast(Comparator.naturalOrder()))
        .reversed());

    return new ReviewOverviewDto(
        dueToday,
        reviewedToday,
        new ReviewLevelStatsDto(unreviewed, again, normal, easy, scheduled),
        toReviewDays(completed),
        toReviewDays(upcoming),
        recentReviewed.stream().limit(6).map(NoteSummary::toDto).toList()
    );
  }

  private GraphDto buildGraph(LocalDate today) throws IOException {
    List<GraphNodeDto> nodes = indexCache.stream()
        .map(note -> new GraphNodeDto(
            note.id(),
            note.path(),
            note.title(),
            note.category(),
            note.tags(),
            note.wordCount(),
            note.status(),
            note.reviewLevel(),
            isDue(note.nextReviewAt(), today),
            note.updatedAt()
        ))
        .toList();

    Map<String, NoteGraphData> graphData = new LinkedHashMap<>();
    Map<String, String> lookup = new LinkedHashMap<>();
    for (NoteSummary note : indexCache) {
      String markdown = Files.readString(note.absolutePath(), StandardCharsets.UTF_8);
      NoteGraphData data = new NoteGraphData(
          note,
          extractLinkedTargets(markdown),
          extractKeywords(note, markdown)
      );
      graphData.put(note.id(), data);
      lookup.put(normalizeLookup(note.title()), note.id());
      lookup.put(normalizeLookup(stripMdExtension(note.path())), note.id());
      lookup.put(normalizeLookup(stripMdExtension(Path.of(note.path()).getFileName().toString())), note.id());
    }

    Map<String, EdgeAccumulator> edges = new LinkedHashMap<>();
    for (int i = 0; i < indexCache.size(); i += 1) {
      NoteSummary source = indexCache.get(i);
      NoteGraphData sourceData = graphData.get(source.id());
      for (int j = i + 1; j < indexCache.size(); j += 1) {
        NoteSummary target = indexCache.get(j);
        NoteGraphData targetData = graphData.get(target.id());
        List<String> reasons = new ArrayList<>();
        int weight = 0;

        List<String> sharedTags = intersection(source.tags(), target.tags());
        if (!sharedTags.isEmpty()) {
          weight += Math.min(5, sharedTags.size() * 2);
          reasons.add("共同标签：" + String.join("、", sharedTags.stream().limit(4).toList()));
        }

        if (source.category().equals(target.category())) {
          weight += 2;
          reasons.add("同分类：" + source.category());
        }

        boolean linked = linksTo(sourceData.links(), target, lookup) || linksTo(targetData.links(), source, lookup);
        if (linked) {
          weight += 5;
          reasons.add("正文链接互相关联");
        }

        List<String> sharedKeywords = intersection(sourceData.keywords(), targetData.keywords()).stream()
            .limit(5)
            .toList();
        if (!sharedKeywords.isEmpty()) {
          weight += Math.min(4, sharedKeywords.size());
          reasons.add("关键词重合：" + String.join("、", sharedKeywords));
        }

        if (weight > 0) {
          addEdge(edges, source.id(), target.id(), weight, reasons);
        }
      }
    }

    Map<String, Integer> keptPerNode = new LinkedHashMap<>();
    List<GraphEdgeDto> edgeDtos = edges.values().stream()
        .sorted(Comparator
            .comparingInt(EdgeAccumulator::weight).reversed()
            .thenComparing(edge -> edge.source + edge.target))
        .filter(edge -> {
          int sourceCount = keptPerNode.getOrDefault(edge.source, 0);
          int targetCount = keptPerNode.getOrDefault(edge.target, 0);
          if (sourceCount >= 8 || targetCount >= 8) {
            return false;
          }
          keptPerNode.put(edge.source, sourceCount + 1);
          keptPerNode.put(edge.target, targetCount + 1);
          return true;
        })
        .map(edge -> new GraphEdgeDto(edge.source, edge.target, edge.weight, List.copyOf(edge.reasons)))
        .toList();

    return new GraphDto(nodes, edgeDtos);
  }

  private List<ReviewDayDto> toReviewDays(Map<LocalDate, Integer> days) {
    return days.entrySet().stream()
        .map(entry -> new ReviewDayDto(entry.getKey().toString(), entry.getValue()))
        .toList();
  }

  private List<String> extractLinkedTargets(String markdown) {
    LinkedHashSet<String> links = new LinkedHashSet<>();
    Matcher markdownMatcher = MARKDOWN_LINK.matcher(markdown);
    while (markdownMatcher.find()) {
      String raw = decode(markdownMatcher.group(1).trim()).replace('\\', '/');
      if (!raw.matches("^[a-zA-Z]+://.*") && !raw.startsWith("/api/assets/")) {
        links.add(normalizeLookup(stripMdExtension(raw)));
        links.add(normalizeLookup(stripMdExtension(Path.of(raw).getFileName().toString())));
      }
    }
    Matcher obsidianMatcher = OBSIDIAN_LINK.matcher(markdown);
    while (obsidianMatcher.find()) {
      String raw = obsidianMatcher.group(1).trim();
      links.add(normalizeLookup(stripMdExtension(raw)));
      links.add(normalizeLookup(stripMdExtension(Path.of(raw).getFileName().toString())));
    }
    return links.stream().filter(value -> !value.isBlank()).toList();
  }

  private List<String> extractKeywords(NoteSummary note, String markdown) {
    String text = (note.title() + " " + note.category() + " " + String.join(" ", note.tags()) + " "
        + markdown.substring(0, Math.min(markdown.length(), 2000))).toLowerCase(Locale.ROOT);
    LinkedHashSet<String> keywords = new LinkedHashSet<>();
    Matcher matcher = KEYWORD.matcher(text);
    while (matcher.find() && keywords.size() < 80) {
      String keyword = matcher.group().trim();
      if (isUsefulKeyword(keyword)) {
        keywords.add(keyword);
      }
    }
    return List.copyOf(keywords);
  }

  private boolean isUsefulKeyword(String keyword) {
    return !List.of(
        "the", "and", "for", "with", "this", "that", "from", "class", "public", "private", "return",
        "一个", "这个", "可以", "进行", "使用", "如果", "就是", "因为", "所以", "以及", "时候", "需要", "不会", "没有"
    ).contains(keyword);
  }

  private boolean linksTo(List<String> links, NoteSummary target, Map<String, String> lookup) {
    if (links.isEmpty()) {
      return false;
    }
    for (String link : links) {
      if (target.id().equals(lookup.get(link))) {
        return true;
      }
    }
    return false;
  }

  private List<String> intersection(List<String> source, List<String> target) {
    LinkedHashSet<String> targetSet = new LinkedHashSet<>(target);
    List<String> shared = new ArrayList<>();
    for (String item : source) {
      if (targetSet.contains(item)) {
        shared.add(item);
      }
    }
    return shared;
  }

  private void addEdge(Map<String, EdgeAccumulator> edges, String source, String target, int weight, List<String> reasons) {
    String key = source.compareTo(target) <= 0 ? source + "::" + target : target + "::" + source;
    EdgeAccumulator edge = edges.get(key);
    if (edge == null) {
      edges.put(key, new EdgeAccumulator(source, target, weight, new ArrayList<>(reasons)));
      return;
    }
    edge.weight += weight;
    for (String reason : reasons) {
      if (!edge.reasons.contains(reason)) {
        edge.reasons.add(reason);
      }
    }
  }

  private String normalizeLookup(String value) {
    return (value == null ? "" : value)
        .replace('\\', '/')
        .replaceFirst("(?i)\\.md$", "")
        .trim()
        .toLowerCase(Locale.ROOT);
  }

  private boolean isDue(String nextReviewAt, LocalDate today) {
    return parseDate(nextReviewAt).map(date -> !date.isAfter(today)).orElse(true);
  }

  private boolean matchesReviewFilter(NoteSummary note, String reviewFilter, LocalDate today) {
    return switch (reviewFilter) {
      case "due" -> isDue(note.nextReviewAt(), today);
      case "unreviewed" -> note.lastReviewedAt() == null || note.lastReviewedAt().isBlank();
      case "again", "normal", "easy" -> reviewFilter.equals(note.reviewLevel());
      case "scheduled" -> parseDate(note.nextReviewAt()).map(date -> date.isAfter(today)).orElse(false);
      default -> true;
    };
  }

  private boolean isReviewedOn(String lastReviewedAt, LocalDate date) {
    if (lastReviewedAt == null || lastReviewedAt.isBlank()) {
      return false;
    }
    try {
      return Instant.parse(lastReviewedAt).atZone(ZoneId.systemDefault()).toLocalDate().equals(date);
    } catch (RuntimeException ignored) {
      return parseDate(lastReviewedAt).map(date::equals).orElse(false);
    }
  }

  private Optional<LocalDate> reviewedDate(String lastReviewedAt) {
    if (lastReviewedAt == null || lastReviewedAt.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Instant.parse(lastReviewedAt).atZone(ZoneId.systemDefault()).toLocalDate());
    } catch (RuntimeException ignored) {
      return parseDate(lastReviewedAt);
    }
  }

  private Optional<LocalDate> parseDate(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(value.substring(0, Math.min(10, value.length()))));
    } catch (RuntimeException ignored) {
      return Optional.empty();
    }
  }

  private Path uniqueNotePath(String category, String filename, Path currentPath) throws IOException {
    Path baseDir = mdRoot.resolve(category).normalize();
    assertInsideMd(baseDir);
    String cleanFilename = sanitizeTitle(stripMdExtension(filename));
    Path candidate = baseDir.resolve(cleanFilename + ".md").normalize();
    int index = 2;
    while (Files.exists(candidate) && (currentPath == null || !candidate.equals(currentPath.normalize()))) {
      candidate = baseDir.resolve(cleanFilename + "-" + index + ".md").normalize();
      index += 1;
    }
    assertInsideMd(candidate);
    return candidate;
  }

  private Path discoverWorkspaceRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    if (Files.isDirectory(cwd.resolve("md"))) {
      return cwd;
    }
    if (Files.isDirectory(cwd.resolve("..").resolve("md"))) {
      return cwd.resolve("..").normalize();
    }
    return cwd;
  }

  private Path resolveConfiguredPath(String value, String fallback) {
    String configured = value == null || value.isBlank() ? fallback : value;
    Path path = Path.of(configured);
    return path.isAbsolute() ? path.normalize() : workspaceRoot.resolve(path).normalize();
  }

  private void assertInsideMd(Path target) {
    if (!isInside(mdRoot, target.toAbsolutePath().normalize())) {
      throw new IllegalArgumentException("路径超出 md 目录。");
    }
  }

  private boolean isInside(Path parent, Path target) {
    Path normalizedParent = parent.toAbsolutePath().normalize();
    Path normalizedTarget = target.toAbsolutePath().normalize();
    Path parentRoot = normalizedParent.getRoot();
    Path targetRoot = normalizedTarget.getRoot();
    if (parentRoot != null && targetRoot != null && !parentRoot.equals(targetRoot)) {
      return false;
    }
    return normalizedTarget.startsWith(normalizedParent);
  }

  private String sanitizeTitle(String value) {
    String clean = INVALID_WINDOWS_NAME.matcher(value == null ? "" : value).replaceAll("").trim();
    if (clean.length() > 80) {
      clean = clean.substring(0, 80).trim();
    }
    return clean.isBlank() ? "新的学习记录" : clean;
  }

  private String sanitizeCategory(String value) {
    String normalized = (value == null ? "" : value).replace('\\', '/');
    List<String> parts = new ArrayList<>();
    for (String rawPart : normalized.split("/")) {
      String part = INVALID_WINDOWS_NAME.matcher(rawPart).replaceAll("").trim();
      if (!part.isBlank()) {
        parts.add(part);
      }
    }
    return parts.isEmpty() ? "inbox" : String.join("/", parts);
  }

  private String idFromAbsolutePath(Path absolutePath) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(toPosix(mdRoot.relativize(absolutePath)).getBytes(StandardCharsets.UTF_8));
  }

  private String toPosix(Path value) {
    return value == null ? "" : value.toString().replace('\\', '/');
  }

  private String slugify(String value) {
    return value.trim().toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{N}]+", "-")
        .replaceAll("^-|-$", "");
  }

  private String encodePath(String value) {
    String[] parts = value.split("/");
    List<String> encoded = new ArrayList<>();
    for (String part : parts) {
      encoded.add(java.net.URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
    }
    return String.join("/", encoded);
  }

  private String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private int countWords(String markdown) {
    int chinese = 0;
    int latin = 0;
    for (int i = 0; i < markdown.length(); i++) {
      char c = markdown.charAt(i);
      if (c >= '\u4e00' && c <= '\u9fff') {
        chinese += 1;
      }
    }
    Matcher matcher = Pattern.compile("[a-zA-Z0-9_]+").matcher(markdown.replaceAll("[\\u4e00-\\u9fff]", " "));
    while (matcher.find()) {
      latin += 1;
    }
    return chinese + latin;
  }

  private String normalizeStatus(String status) {
    return List.of("learning", "reviewing", "done").contains(status) ? status : "learning";
  }

  private String normalizeReviewLevel(String level) {
    if (level == null) {
      return null;
    }
    return List.of("again", "normal", "easy").contains(level) ? level : null;
  }

  private String normalizeReviewFilter(String review) {
    if (review == null || review.isBlank()) {
      return "all";
    }
    String normalized = review.trim().toLowerCase(Locale.ROOT);
    return List.of("all", "due", "unreviewed", "again", "normal", "easy", "scheduled").contains(normalized)
        ? normalized
        : "all";
  }

  private int reviewIntervalDays(String level, int reviewCount) {
    if ("again".equals(level)) {
      return 1;
    }
    if ("normal".equals(level)) {
      return 3;
    }
    if (reviewCount <= 1) {
      return 7;
    }
    return reviewCount == 2 ? 14 : 30;
  }

  private String stripMdExtension(String filename) {
    return filename == null ? "" : filename.replaceFirst("(?i)\\.md$", "");
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "未命名笔记";
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  @SuppressWarnings("unchecked")
  private List<String> asStringList(Object value) {
    if (value instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    if (value instanceof String string) {
      return List.of(string);
    }
    return List.of();
  }

  private List<String> joinLists(List<String>... lists) {
    List<String> joined = new ArrayList<>();
    for (List<String> list : lists) {
      if (list != null) {
        joined.addAll(list);
      }
    }
    return joined;
  }

  private List<String> unique(List<String> values) {
    LinkedHashSet<String> set = new LinkedHashSet<>();
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        set.add(value.trim());
      }
    }
    return List.copyOf(set);
  }

  public record NoteDetail(NoteDto note, String markdown, String rawMarkdown) {}

  public static class MetaPatch {
    public boolean favorite;
    public String status;
    public List<String> tags = List.of();
  }

  public static class MetaStore {
    public Map<String, NoteMeta> notes = new LinkedHashMap<>();
  }

  public static class NoteMeta {
    public String title;
    public List<String> tags = List.of();
    public Boolean favorite = false;
    public String status = "learning";
    public int reviewCount = 0;
    public String lastReviewedAt;
    public String nextReviewAt;
    public String reviewLevel;
  }

  public static class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
      super(message);
    }
  }

  private record NoteGraphData(NoteSummary note, List<String> links, List<String> keywords) {}

  private static final class EdgeAccumulator {
    private final String source;
    private final String target;
    private int weight;
    private final List<String> reasons;

    private EdgeAccumulator(String source, String target, int weight, List<String> reasons) {
      this.source = source;
      this.target = target;
      this.weight = weight;
      this.reasons = reasons;
    }

    private int weight() {
      return weight;
    }
  }

  private static final class CollatorHolder {
    private static final Comparator<String> ZH_CN = java.text.Collator.getInstance(Locale.CHINA)::compare;
  }
}

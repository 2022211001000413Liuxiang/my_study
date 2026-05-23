package com.study.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.config.StudyProperties;
import com.study.model.CategoryDto;
import com.study.model.HeadingDto;
import com.study.model.NoteDto;
import com.study.model.NoteSummary;
import com.study.model.NotesResponse;
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

  public synchronized NotesResponse list(String category, String query) throws IOException {
    String normalizedCategory = category == null ? "" : category.trim();
    String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<NoteSummary> notes = indexCache;

    if (!normalizedCategory.isBlank() && !"all".equals(normalizedCategory)) {
      notes = notes.stream()
          .filter(note -> note.category().equals(normalizedCategory)
              || note.path().startsWith(normalizedCategory + "/"))
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

  private boolean isDue(String nextReviewAt, LocalDate today) {
    return parseDate(nextReviewAt).map(date -> !date.isAfter(today)).orElse(true);
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

  private static final class CollatorHolder {
    private static final Comparator<String> ZH_CN = java.text.Collator.getInstance(Locale.CHINA)::compare;
  }
}

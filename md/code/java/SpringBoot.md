# SpringBoot 常用知识

## 1. 项目基础

### 1.1 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/example/demo/
│   │       ├── DemoApplication.java      # 启动类
│   │       ├── controller/               # 控制器层
│   │       ├── service/                  # 业务逻辑层
│   │       │   └── impl/                 # 接口实现
│   │       ├── mapper/                   # 数据访问层（MyBatis）
│   │       ├── repository/               # 数据访问层（JPA）
│   │       ├── entity/                   # 实体类
│   │       ├── dto/                      # 数据传输对象
│   │       ├── vo/                       # 视图对象
│   │       ├── config/                   # 配置类
│   │       ├── common/                   # 公共类（工具、常量等）
│   │       ├── exception/                # 自定义异常
│   │       └── interceptor/              # 拦截器
│   └── resources/
│       ├── application.yml               # 主配置文件
│       ├── application-dev.yml           # 开发环境配置
│       ├── application-prod.yml          # 生产环境配置
│       ├── mapper/                       # MyBatis XML 映射文件
│       ├── static/                       # 静态资源
│       └── templates/                    # 模板文件
└── test/                                 # 测试代码
```

### 1.2 启动类

```java
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

`@SpringBootApplication` 是组合注解，包含：
- `@SpringBootConfiguration` — 标记为配置类
- `@EnableAutoConfiguration` — 开启自动配置
- `@ComponentScan` — 组件扫描

### 1.3 配置文件 application.yml

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: demo-app
  profiles:
    active: dev    # 激活 dev 环境配置
  datasource:
    url: jdbc:mysql://localhost:3306/dbname?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.demo.entity
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.example.demo: debug
```

---

## 2. 控制层 Controller

### 2.1 基本注解

| 注解                         | 说明                                        |
| -------------------------- | ----------------------------------------- |
| `@RestController`          | = `@Controller` + `@ResponseBody`，返回 JSON |
| `@RequestMapping("/path")` | 映射请求路径，可指定 method                         |
| `@GetMapping`              | GET 请求                                    |
| `@PostMapping`             | POST 请求                                   |
| `@PutMapping`              | PUT 请求                                    |
| `@DeleteMapping`           | DELETE 请求                                 |
| `@PathVariable`            | 获取路径参数 `/user/{id}`                       |
| `@RequestParam`            | 获取查询参数 `/user?name=xx`                    |
| `@RequestBody`             | 获取请求体 JSON                                |
| `@RequestHeader`           | 获取请求头                                     |

### 2.2 统一响应封装

```java
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
```

### 2.3 Controller 示例

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<User>> list(@RequestParam(required = false) String name) {
        return Result.success(userService.list(name));
    }

    @PostMapping
    public Result<Void> save(@RequestBody @Valid UserDTO userDTO) {
        userService.save(userDTO);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserDTO userDTO) {
        userService.update(id, userDTO);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }
}
```

---

## 3. 参数校验

使用 `spring-boot-starter-validation` 依赖。

### 3.1 常用注解

| 注解                 | 说明                |
| ------------------ | ----------------- |
| `@NotNull`         | 不能为 null          |
| `@NotEmpty`        | 不能为 null 或空字符串/集合 |
| `@NotBlank`        | 不能为 null、空串或纯空格   |
| `@Size(min, max)`  | 字符串长度 / 集合大小      |
| `@Min / @Max`      | 数值范围              |
| `@Email`           | 邮箱格式              |
| `@Pattern(regexp)` | 正则校验              |
| `@Positive`        | 正数                |

### 3.2 DTO 示例

```java
@Data
public class UserDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度 2-20")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄最小 1")
    @Max(value = 150, message = "年龄最大 150")
    private Integer age;
}
```

### 3.3 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)//取每个字段的错误信息
                .collect(Collectors.joining(", "));//用逗号拼接
        return Result.error(400, msg);
    }
```
当 DTO 上有 `@NotBlank`、`@Size` 等校验注解，且校验失败时，Spring 会抛出 `MethodArgumentNotValidException`。这里捕获它，提取所有字段的错误消息拼成一句话返回。
**触发场景**：用户提交了空用户名、邮箱格式错误等。
```
    // 自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
```

```
    // 兜底异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(500, "服务器内部错误");
    }
}
```
兜底异常用于捕获所有未被上面处理的异常

自定义业务异常：

```java
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

---

## 4. 业务层 Service

### 4.1 接口 + 实现模式

```java
// 接口
public interface UserService {
    User getById(Long id);
    List<User> list(String name);
    void save(UserDTO userDTO);
    void update(Long id, UserDTO userDTO);
    void delete(Long id);
}
```

```java
// 实现
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(UserDTO userDTO) {
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        userMapper.insert(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserDTO userDTO) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        BeanUtils.copyProperties(userDTO, user);
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userMapper.deleteById(id);
    }
}
```

> **注意**：`@Transactional` 必须指定 `rollbackFor = Exception.class`（所有异常都回滚），否则默认只对 RuntimeException 回滚。

---

## 5. 数据访问层

### 5.1 MyBatis-Plus（推荐）
详细见[[MyBatis Plus]]
#### 依赖

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>
```

#### Mapper

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // BaseMapper 已提供 CRUD 方法
    // 复杂查询可在 XML 中自定义
}
```

#### 实体类

```java
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String email;
    private Integer age;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;  // 逻辑删除
}
```

#### 常用 CRUD

```java
// 查询
userMapper.selectById(1L);
userMapper.selectBatchIds(List.of(1L, 2L, 3L));
userMapper.selectOne(new LambdaQueryWrapper<User>()
        .eq(User::getUsername, "admin"));

// 条件查询
List<User> users = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .like(StringUtils.isNotBlank(name), User::getUsername, name)
        .ge(User::getAge, 18)
        .orderByDesc(User::getCreateTime)
        .last("LIMIT 10")
);

// 分页查询
// 先配置分页插件
Page<User> page = userMapper.selectPage(
    new Page<>(1, 10),  // 第 1 页，每页 10 条
    new LambdaQueryWrapper<User>()
        .like(StringUtils.isNotBlank(name), User::getUsername, name)
);
List<User> records = page.getRecords();
long total = page.getTotal();

// 新增
userMapper.insert(user);

// 修改
userMapper.updateById(user);

// 删除
userMapper.deleteById(1L);
```

#### 分页插件配置

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

#### 自动填充

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
```

### 5.2 MyBatis XML 方式

```java
@Mapper
public interface UserMapper {
    User selectById(Long id);
    List<User> selectByName(@Param("name") String name);
    int insert(User user);
}
```

```xml
<!-- resources/mapper/UserMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.mapper.UserMapper">

    <select id="selectById" resultType="User">
        SELECT * FROM sys_user WHERE id = #{id}
    </select>

    <select id="selectByName" resultType="User">
        SELECT * FROM sys_user
        <where>
            <if test="name != null and name != ''">
                AND username LIKE CONCAT('%', #{name}, '%')
            </if>
        </where>
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO sys_user (username, password, email, age)
        VALUES (#{username}, #{password}, #{email}, #{age})
    </insert>
</mapper>
```

---

## 6. 常用注解速查

| 注解                         | 位置         | 说明          |
| -------------------------- | ---------- | ----------- |
| `@SpringBootApplication`   | 启动类        | 启动入口        |
| `@RestController`          | Controller | RESTful 控制器 |
| `@Service`                 | Service 实现 | 标记业务层       |
| `@Mapper`                  | Mapper 接口  | MyBatis 映射  |
| `@Component`               | 通用组件       | 泛化组件        |
| `@Configuration`           | 配置类        | 配置类         |
| `@Bean`                    | 配置类方法      | 注册 Bean     |
| `@Autowired`               | 字段/构造器     | 自动注入        |
| `@Value("${key}")`         | 字段         | 读取配置        |
| `@ConfigurationProperties` | 类          | 绑定配置前缀      |
| `@Transactional`           | 方法/类       | 事务管理        |
| `@Aspect`                  | 切面类        | AOP 切面      |
| `@Scheduled`               | 方法         | 定时任务        |
| `@Async`                   | 方法         | 异步执行        |
| `@Cacheable`               | 方法         | 缓存          |
| `@Profile`                 | 类/方法       | 环境限定        |

---

## 7. 依赖注入方式

### 7.1 构造器注入（推荐）

```java
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    // Spring 4.3+ 单构造器可省略 @Autowired
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}
```

### 7.2 字段注入

```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
}
```

### 7.3 Lombok 简化构造器注入

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
}
```

---

## 8. AOP 面向切面编程

### 8.1 切面示例（日志记录）

```java
@Aspect
@Component
@Slf4j
public class LogAspect {

    @Pointcut("execution(* com.example.demo.controller..*.*(..))")
    public void controllerPointcut() {}

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        log.info("开始执行: {}", methodName);

        Object result = joinPoint.proceed();

        long cost = System.currentTimeMillis() - start;
        log.info("执行完成: {}，耗时: {}ms", methodName, cost);
        return result;
    }
}
```

### 8.2 常用通知类型

| 注解 | 说明 |
|------|------|
| `@Before` | 前置通知 |
| `@After` | 后置通知（finally） |
| `@AfterReturning` | 返回后通知 |
| `@AfterThrowing` | 异常通知 |
| `@Around` | 环绕通知（最强大） |

### 8.3 常用切入点表达式

```java
// Controller 层所有方法
@Pointcut("execution(* com.example.demo.controller..*.*(..))")

// Service 层所有方法
@Pointcut("execution(* com.example.demo.service..*.*(..))")

// 特定注解标记的方法
@Pointcut("@annotation(com.example.demo.common.Log)")

// 组合
@Pointcut("controllerPointcut() && @annotation(log)")
```

---

## 9. 拦截器

### 9.1 定义拦截器

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || !TokenUtils.verify(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {}

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {}
}
```

### 9.2 注册拦截器

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/static/**");
    }
}
```

---

## 10. 跨域配置

### 10.1 全局配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

### 10.2 注解方式

```java
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
public class UserController { }
```

---

## 11. 定时任务

### 11.1 开启定时任务

```java
@SpringBootApplication
@EnableScheduling
public class DemoApplication { }
```

### 11.2 定时任务示例

```java
@Component
public class ScheduledTask {

    // 每天凌晨 2 点执行
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyTask() { }

    // 固定间隔 5 秒（上次执行结束后 5 秒）
    @Scheduled(fixedDelay = 5000)
    public void fixedDelayTask() { }

    // 固定频率 5 秒（每 5 秒执行一次）
    @Scheduled(fixedRate = 5000)
    public void fixedRateTask() { }
}
```

### 11.3 Cron 表达式

```
格式: 秒 分 时 日 月 周 [年]

常用示例:
0 0/30 * * * ?      每 30 分钟
0 0 8 * * ?         每天 8:00
0 0 8 * * 1-5       工作日 8:00
0 0 0 1 * ?         每月 1 号 0:00
0 0 12 ? * WED      每周三 12:00
```

---

## 12. 异步任务

### 12.1 开启异步

```java
@SpringBootApplication
@EnableAsync
public class DemoApplication { }
```

### 12.2 使用异步

```java
@Service
public class AsyncService {

    @Async("taskExecutor")
    public void sendEmail(String to) {
        // 耗时的邮件发送操作
    }
}
```

### 12.3 线程池配置

```java
@Configuration
public class AsyncConfig {
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

## 13. 文件上传

```java
@RestController
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + ext;

        try {
            File dest = new File(uploadDir + fileName);
            dest.getParentFile().mkdirs();
            file.transferTo(dest);
            return Result.success("/files/" + fileName);
        } catch (IOException e) {
            return Result.error(500, "上传失败");
        }
    }
}
```

配置：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB

file:
  upload-dir: /data/uploads/
```

---

## 14. Redis 缓存

### 14.1 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 14.2 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 123456
    database: 0
```

### 14.3 RedisTemplate 使用

```java
@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
```

### 14.4 Redis 序列化配置

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}
```

### 14.5 注解缓存

```java
@SpringBootApplication
@EnableCaching
public class DemoApplication { }

@Service
public class UserServiceImpl {

    @Cacheable(value = "user", key = "#id")
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @CacheEvict(value = "user", key = "#id")
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @CachePut(value = "user", key = "#user.id")
    public User update(User user) {
        userMapper.updateById(user);
        return user;
    }
}
```

---

## 15. Swagger / Knife4j 接口文档

### 15.1 依赖（Knife4j）

```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-spring-boot-starter</artifactId>
    <version>4.3.0</version>
</dependency>
```

### 15.2 配置

```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
  group-configs:
    - group: default
      paths-to-match: /**
      packages-to-scan: com.example.demo.controller

knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 15.3 使用注解

```java
@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
public class UserController {

    @Operation(summary = "根据 ID 查询用户")
    @GetMapping("/{id}")
    public Result<User> getById(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> save(@RequestBody @Valid UserDTO userDTO) {
        userService.save(userDTO);
        return Result.success(null);
    }
}
```

---

## 16. 单元测试

### 16.1 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 16.2 Service 测试

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testGetById() {
        User user = userService.getById(1L);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("admin", user.getUsername());
    }

    @Test
    void testSave() {
        UserDTO dto = new UserDTO();
        dto.setUsername("test");
        dto.setEmail("test@example.com");
        dto.setAge(20);
        Assertions.assertDoesNotThrow(() -> userService.save(dto));
    }
}
```

### 16.3 Controller 测试（MockMvc）

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/user/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200));
    }

    @Test
    void testSave() throws Exception {
        String json = "{\"username\":\"test\",\"email\":\"test@example.com\",\"age\":20}";
        mockMvc.perform(MockMvcRequestBuilders.post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
```

---

## 17. 常用 Starter 依赖

```xml
<!-- Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- 参数校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- MyBatis-Plus -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- AOP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Hutool 工具包 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>
```

---

## 18. 打包与部署

```bash
# 打包（跳过测试）
mvn clean package -DskipTests

# 运行
java -jar target/demo-0.0.1-SNAPSHOT.jar

# 指定环境
java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 指定端口
java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=9090
```

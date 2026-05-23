## 使用方法
1. 导入依赖
2. 在 mapper 层继承 BaseMapper，并添加泛型
3. 在启动类添加 @MapperScan 扫描 mapper 接口

---

## 常用注解

| 注解            | 作用                                      |
| ------------- | --------------------------------------- |
| `@TableName`  | 指定表名                                    |
| `@TableId`    | 指定主键，`type = IdType.ASSIGN_ID` 雪花算法自动生成 |
| `@TableField` | 指定字段名，`exist = false` 表示非数据库字段          |
| `@TableLogic` | 逻辑删除字段                                  |

---

## 数据库表名与类名映射

### 默认映射规则（驼峰转下划线）

MyBatis Plus 默认开启驼峰命名自动映射：
- 类名 `User` → 表名 `user`
- 类名 `SysUser` → 表名 `sys_user`
- 字段 `userName` → 列名 `user_name`

名为id的字段作为主键
### @TableName 指定表名

当类名与表名不一致时，使用 `@TableName` 指定：

```java
@TableName("t_user")  // 数据库表名是 t_user
public class User {
    @TableId
    private Long id;
    
    @TableField("user_name")  // 字段名不一致时指定
    private String name;
}
```

### @TableField 指定列名

1.当字段名与数据库列名不一致时：

```java
public class User {
    @TableId("uid")  // 主键列名是 uid
    private Long id;
    
    @TableField("user_name")  // 列名是 user_name
    private String userName;
    
    @TableField("create_time")  // 列名是 create_time
    private LocalDateTime createTime;
}

//特
```
2.如果类名是is开头，且是布尔类型，也需要加上tablefield注解
因为mabitsplus在映射时会将is字段去除

3.成员变量名与数据库关键字冲突，使用tablefield还需要加上转义字符->"`order`"

4.成员变量不是数据库字段->exist=false
### 全局配置

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true  # 开启驼峰转下划线（默认 true）
  global-config:
    db-config:
      table-prefix: t_  # 表名前缀，类 User → 表 t_user
      id-type: auto     # 主键策略
```

### 常见映射示例

| Java 类名 | Java 字段 | 数据库表 | 数据库列 |
|-----------|----------|---------|---------|
| User | userName | user | user_name |
| SysUser | createTime | sys_user | create_time |
| OrderInfo | orderStatus | order_info | order_status |

---

## CRUD 方法

### 插入
```java
// 插入单条
int rows = userMapper.insert(user);

// 批量插入
List<User> users = ...;
users.forEach(userMapper::insert);
```

### 删除
```java
// 根据 ID 删除
userMapper.deleteById(1L);

// 条件删除
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("status", 0);
userMapper.delete(wrapper);

// 批量删除
userMapper.deleteBatchIds(Arrays.asList(1L, 2L, 3L));
```

### 修改
```java
// 根据 ID 修改
userMapper.updateById(user);

// 条件修改
User user = new User();
user.setName("新名字");
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("id", 1);
userMapper.update(user, wrapper);
```

### 查询
```java
// 根据 ID 查询
User user = userMapper.selectById(1L);

// 批量查询
List<User> users = userMapper.selectBatchIds(Arrays.asList(1L, 2L));

// 条件查询
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("status", 1)
       .orderByDesc("create_time");
List<User> users = userMapper.selectList(wrapper);

// 查询单条
User user = userMapper.selectOne(wrapper);

// 查询总记录数
Integer count = userMapper.selectCount(wrapper);
```

---

## 条件构造器

### QueryWrapper
```java
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.eq("name", "张三")          // 等于
       .ne("status", 0)            // 不等于
       .like("name", "张")          // 模糊匹配
       .between("age", 18, 30)     // 区间
       .in("id", Arrays.asList(1,2,3))  // IN 查询
       .orderByDesc("create_time") // 排序
       .select("id", "name", "age"); // 指定查询字段
```

### LambdaQueryWrapper（推荐）
LambdaQueryWrapper是用于解决QueryWrapper的硬编码的问题
```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getStatus, 1)
       .like(User::getName, "张")
       .orderByDesc(User::getCreateTime);
List<User> users = userMapper.selectList(wrapper);
```

### UpdateWrapper
基于BaseMapper中的update方法更新时只能直接赋值，对于一些复杂的需求就难以实现。
updatemapper用于指定更新条件和更新字段：

```java
// 条件更新
UpdateWrapper<User> wrapper = new UpdateWrapper<>();
wrapper.eq("id", 1)
       .set("name", "新名字")
       .set("status", 1);
userMapper.update(null, wrapper);

// LambdaUpdateWrapper
LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
wrapper.eq(User::getId, 1)
       .set(User::getName, "新名字")
       .set(User::getStatus, 1);
userMapper.update(null, wrapper);
```

### setNull - 将字段置空
```java
UpdateWrapper<User> wrapper = new UpdateWrapper<>();
wrapper.eq("id", 1)
       .set("name", null);  // 将 name 更新为 NULL
```

---

## IService 与 BaseMapper 的区别

| 特性 | BaseMapper | IService |
|------|------------|----------|
| 层级 | Mapper 层（数据访问层） | Service 层（业务逻辑层） |
| 返回值 | `int`（影响行数）或实体对象 | `boolean`（成功/失败）或实体对象 |
| 批量操作 | 需循环调用或手写 SQL | 内置 `saveBatch`、`removeByIds`、`updateBatchById` |
| 链式查询 | 不支持 | 支持 `lambdaQuery()` 链式调用 |
| 链式更新 | 不支持 | 支持 `lambdaUpdate()` 链式调用 |
| 事务 | 需手动管理 | 可配合 `@Transactional` 在 Service 层管理 |
| 业务逻辑 | 不适合放业务逻辑 | 适合封装业务逻辑 |

### 使用建议

- **简单 CRUD**：直接用 BaseMapper 即可
- **需要批量操作**：用 IService 的 saveBatch、updateBatchById
- **复杂业务逻辑**：用 IService，便于事务管理和逻辑封装
- **多表联查**：BaseMapper 配合 XML 自定义 SQL

---

## Service 层

### IService 接口

继承 `IService<T>` 接口，可获得更丰富的 CRUD 方法：

```java
public interface UserService extends IService<User> {
    // 自定义业务方法
}
```

### ServiceImpl 实现类

```java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    // 自定义业务方法实现
}
```

### 常用方法

#### 插入
```java
// 插入单条
boolean save(User user);

// 批量插入
boolean saveBatch(List<User> users);

// 批量插入（指定每批大小）
boolean saveBatch(List<User> users, int batchSize);
```

#### 删除
```java
// 根据 ID 删除
boolean removeById(Long id);

// 条件删除
boolean remove(Wrapper<User> wrapper);

// 批量删除
boolean removeByIds(Collection<Long> ids);
```

#### 修改
```java
// 根据 ID 修改
boolean updateById(User entity);

// 条件修改
boolean update(User entity, Wrapper<User> wrapper);

// 批量修改
boolean updateBatchById(List<User> entities);
```

#### 查询
```java
// 根据 ID 查询
User getById(Long id);

// 查询所有
List<User> list();

// 条件查询
List<User> list(Wrapper<User> wrapper);

// 分页查询
Page<User> page(Page<User> page, Wrapper<User> wrapper);

// 查询单条
User getOne(Wrapper<User> wrapper);

// 查询总记录数
int count();
```

#### 链式查询（Lambda）
```java
// 链式查询
List<User> users = lambdaQuery()
    .eq(User::getStatus, 1)
    .like(User::getName, "张")
    .orderByDesc(User::getCreateTime)
    .list();

// 链式查询单条
User user = lambdaQuery()
    .eq(User::getId, 1)
    .one();
```

#### 链式更新（Lambda）
```java
// 链式更新
boolean success = lambdaUpdate()
    .eq(User::getId, 1)
    .set(User::getName, "新名字")
    .set(User::getStatus, 1)
    .update();
```

---

## 分页查询

### 配置
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

### 使用
```java
Page<User> page = new Page<>(1, 10); // 当前页，每页条数
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getStatus, 1);
Page<User> result = userMapper.selectPage(page, wrapper);

// 获取数据
List<User> records = result.getRecords();
long total = result.getTotal();
```

---

## 自动填充

### 定义处理器
```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 字段配置
```java
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updateTime;
```

---

## 逻辑删除

### 配置
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted  # 全局逻辑删除字段名
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 字段配置
```java
@TableLogic
private Integer deleted;
```

配置后，调用 `deleteById` 会自动转为 `UPDATE SET deleted=1`，查询自动加 `WHERE deleted=0`。

---

## 代码生成器

```java
public class CodeGenerator {
    public static void main(String[] args) {
        AutoGenerator generator = new AutoGenerator();
        
        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
        dsc.setUrl("jdbc:mysql://localhost:3306/db_name?useUnicode=true&characterEncoding=utf-8");
        dsc.setUsername("root");
        dsc.setPassword("root");
        generator.setDataSource(dsc);
        
        // 包配置
        PackageConfig pc = new PackageConfig();
        pc.setModuleName("demo");
        pc.setParent("com.example");
        generator.setPackageInfo(pc);
        
        // 策略配置
        StrategyConfig sc = new StrategyConfig();
        sc.setInclude("user", "order"); // 指定要生成的表
        sc.setEntityLombokModel(true);
        sc.setRestControllerStyle(true);
        generator.setStrategy(sc);
        
        generator.execute();
    }
}
```

---

## 常见问题

1. **字段名与数据库不一致**：使用 `@TableField(value = "db_column")` 映射
2. **排除非数据库字段**：使用 `@TableField(exist = false)`
3. **性能分析**：开发环境可启用 SQL 分析插件
4. **多表联查**：MP 不支持，需手写 SQL 或使用 MyBatis XML

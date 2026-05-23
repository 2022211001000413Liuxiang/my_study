# RabbitMQ 学习文档

## 一、RabbitMQ 简介

### 1.1 什么是 RabbitMQ
- 开源的消息代理和队列服务器
- 基于 AMQP（高级消息队列协议）实现
- 支持多种消息传递模式
- 由 Erlang 语言开发，天然支持高并发

### 1.2 核心概念
| 概念           | 说明                                         |
| ------------ | ------------------------------------------ |
| Producer     | 消息生产者，负责发送消息                               |
| Consumer     | 消息消费者，负责接收消息                               |
| Exchange     | 交换机，接收生产者消息并根据规则路由到队列                      |
| Queue        | 消息队列，存储消息等待消费者消费                           |
| Binding      | 绑定关系，连接 Exchange 和 Queue                   |
| Virtual Host | 虚拟主机，用于隔离不同的用户和权限                          |
| Channel      | 信道，TCP 连接上的虚拟连接                            |
| Connection   | 网络连接，Producer/Consumer 与 Broker 之间的 TCP 连接 |

### 1.3 架构模型
```
Producer → Exchange → Binding → Queue → Consumer
```

---

## 二、Exchange 类型

### 2.1 Direct Exchange（直连交换机）
- 消息路由到与 BindingKey 完全匹配的 Queue
- 精确匹配，性能最高
- 适用场景：点对点精确路由

### 2.2 Fanout Exchange（扇出交换机）
- 将消息广播到所有绑定的 Queue，忽略 RoutingKey
- 类似于发布/订阅模式
- 适用场景：广播通知、日志分发

### 2.3 Topic Exchange（主题交换机）
- 支持通配符匹配（`*` 匹配一个单词，`#` 匹配零个或多个单词）
- 灵活的路由规则
- 适用场景：按分类订阅消息

### 2.4 Headers Exchange（头部交换机）
- 基于消息头属性进行匹配
- 不依赖 RoutingKey
- 适用场景：复杂属性匹配

### 2.5 Default Exchange（默认交换机）
- 名称为空字符串的 Direct Exchange
- 所有队列名作为 RoutingKey
- 适用场景：简单队列绑定

---

## 三、消息模型

### 3.1 简单队列（Simple Queue）
- 一个生产者 → 一个队列 → 一个消费者
- 最简单的点对点模式

### 3.2 工作队列（Work Queue）
- 一个生产者 → 一个队列 → 多个消费者
- 消息默认轮询分发（Round-Robin）-->消息平分型
- 支持公平分发（Fair Dispatch）：设置 prefetch=1-->设置当处理完1个消息才能进行下个消息的处理（能者多劳型）

### 3.3 发布/订阅模式（Pub/Sub）
- 一个生产者 → Exchange → 多个队列 → 多个消费者
- 使用 Fanout Exchange 实现广播

### 3.4 路由模式（Routing）
- 一个生产者 → Direct Exchange → 多个队列（不同 RoutingKey）
- 消费者只接收匹配 RoutingKey 的消息

### 3.5 主题模式（Topic）
- 一个生产者 → Topic Exchange → 多个队列（通配符匹配）
- 支持模糊匹配路由

### 3.6 RPC 模式
- 请求/响应模式
- 使用回调队列（reply_to）和关联ID（correlation_id）
- 适用场景：远程过程调用

---

## 四、消息确认机制

### 4.1 生产者确认
- **Publisher Confirm**：生产者发送消息后等待 Broker 确认
- 异步确认 vs 同步确认
- 保证消息不丢失

### 4.2 消费者确认
- **Manual Acknowledge**：手动 ACK，消费者处理完成后发送确认
- **Auto Acknowledge**：自动 ACK，消息发送即认为成功
- **Reject/Nack**：拒绝消息，可选择是否重新入队

### 4.3 持久化
- 交换机持久化：`durable=true`
- 队列持久化：`durable=true`
- 消息持久化：`deliveryMode=2`
- 三者结合才能保证消息不丢失

---

## 五、高可用

### 5.1 镜像队列（Classic Mirror Queue）
- 主从架构，Master 节点写入，Mirror 节点同步
- 提供高可用和容错能力
- 已被 Quorum Queue 取代

### 5.2 Quorum Queue（仲裁队列）
- 基于 Raft 一致性算法
- 替代镜像队列的新方案
- 更好的性能和可靠性

### 5.3 Exchange 与 Queue 的高可用
- Classic Queue：单节点存储
- Quorum Queue：多节点复制
- Stream Queue：日志型队列，支持多消费者

---

## 六、死信队列（Dead Letter Exchange）

### 6.1 什么是死信队列
- 无法被消费的消息称为"死信"
- 死信队列专门处理这些消息

### 6.2 死信产生原因
1. 消息被拒绝（reject/nack）且不重新入队
2. 消息 TTL 过期
3. 队列达到最大长度（x-max-length）

### 6.3 配置方式
- 设置 `x-dead-letter-exchange` 参数
- 设置 `x-dead-letter-routing-key` 参数

### 6.4 应用场景
- 延迟队列实现
- 订单超时处理
- 错误消息收集与重试

---

## 七、延迟队列（TTL + DLX）

### 7.1 实现原理
- 消息设置 TTL
- 队列绑定死信交换机
- TTL 到期后消息自动路由到死信队列

### 7.2 实现方式
1. 单一 TTL 队列 + DLX
2. 多个 TTL 队列（不同过期时间）
3. RabbitMQ 延迟插件（rabbitmq_delayed_message_exchange）

### 7.3 应用场景
- 定时任务
- 订单超时取消
- 延迟通知

---

## 八、消息幂等性与顺序性

### 8.1 消息幂等性
- 消费者多次消费产生相同效果
- 实现方案：
  - 唯一消息ID + 去重表
  - 状态机控制
  - 业务逻辑去重

### 8.2 消息顺序性
- 同一队列内消息默认有序
- 分区队列（Partitioned Queue）不保证顺序
- 需要顺序保证时，使用单队列或指定分区

---

## 九、高级特性

### 9.1 优先级队列（Priority Queue）
- `x-max-priority` 参数设置优先级
- 优先级高的消息优先被消费

### 9.2 惰性队列（Lazy Queue）
- 消息直接存储到磁盘
- 减少内存占用
- 适用场景：大量消息堆积

### 9.3 Stream Queue
- 日志型队列，消息不可删除
- 支持多消费者独立消费
- 高吞吐量场景

### 9.4 消息 TTL
- 队列级别：`x-message-ttl`
- 消息级别：`expiration` 属性
- 两者取较小值

### 9.5 队列长度限制
- `x-max-length`：最大消息数
- `x-max-length-bytes`：最大字节数
- 超出时可选择丢弃或死信

---

## 十、管理与运维

### 10.1 管理界面（Management UI）
- 默认端口：15672
- 可视化监控、队列管理、策略配置

### 10.2 监控指标
- 消息发布速率 / 消费速率
- 队列长度
- 内存 / 磁盘使用
- 连接数 / 信道数

### 10.3 常用命令
```bash
# 启动 RabbitMQ
rabbitmq-server -detached

# 查看状态
rabbitmqctl status

# 查看队列
rabbitmqctl list_queues

# 添加用户
rabbitmqctl add_user <username> <password>

# 设置权限
rabbitmqctl set_permissions -p <vhost> <user> ".*" ".*" ".*"
```

### 10.4 集群部署
- 普通集群：元数据同步，队列数据单节点存储
- 镜像集群：队列数据多节点复制
- 联邦插件：跨数据中心同步

---

## 十一、与其他消息队列对比

| 特性 | RabbitMQ | Kafka | RocketMQ |
|------|----------|-------|----------|
| 开发语言 | Erlang | Java/Scala | Java |
| 消息模型 | AMQP | 发布/订阅 | 发布/订阅 |
| 吞吐量 | 万级 | 百万级 | 十万级 |
| 延迟 | 微秒级 | 毫秒级 | 毫秒级 |
| 顺序性 | 单队列保证 | 分区保证 | 队列保证 |
| 事务支持 | 支持 | 不支持 | 支持 |
| 延迟队列 | 原生支持 | 不支持 | 原生支持 |

---

## 十二、Spring Boot 集成

### 12.1 依赖配置
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 12.2 配置文件
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

### 12.3 核心注解
- `@RabbitListener`：监听队列消息
- `@RabbitHandler`：处理消息的方法
- `@Exchange`：声明交换机
- `@Queue`：声明队列
- `@QueueBinding`：绑定关系

### 12.4 消息确认配置
```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated  # 生产者确认
    publisher-returns: true             # 消息退回
    listener:
      simple:
        acknowledge-mode: manual        # 手动确认
        prefetch: 1                     # 公平分发
```

### 12.5 消息转换器（Message Converter）

#### 什么是消息转换器
- 负责消息体的序列化和反序列化
- 默认使用 JDK 序列化（`SimpleMessageConverter`）
- 生产环境推荐使用 JSON 格式

#### 默认转换器的问题
- JDK 序列化：体积大、性能差、跨语言支持差
- 可读性差，调试困难

#### 配置 JSON 消息转换器
```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

#### 自定义转换器配置
```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 设置 JSON 转换器
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // 消费者端也要设置转换器
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }
}
```

#### 发送消息示例
```java
@Service
@RequiredArgsConstructor
public class ProducerService {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(UserDTO user) {
        // 直接发送对象，Jackson2JsonMessageConverter 会自动序列化为 JSON
        rabbitTemplate.convertAndSend("exchange", "routing.key", user);
    }
}
```

#### 接收消息示例
```java
@Component
public class ConsumerService {

    @RabbitListener(queues = "queue")
    public void receiveMessage(UserDTO user) {
        // 自动反序列化为 UserDTO 对象
        System.out.println("收到消息: " + user.getName());
    }

    // 也可以接收原始 Message 对象
    @RabbitListener(queues = "queue")
    public void receiveRawMessage(Message message, Channel channel) {
        // 手动处理消息体
        byte[] body = message.getBody();
    }
}
```

#### 常用转换器类型
| 转换器                             | 说明                  | 适用场景      |
| ------------------------------- | ------------------- | --------- |
| SimpleMessageConverter          | 默认，JDK 序列化          | 不推荐生产使用   |
| Jackson2JsonMessageConverter    | JSON 序列化            | **推荐使用**  |
| MappingJackson2MessageConverter | JSON 序列化（Spring 封装） | Spring 项目 |
| MarshallingMessageConverter     | XML 序列化             | 需要 XML 格式 |
| ByteArrayMessageConverter       | 字节数组                | 原始字节数据    |
| StringMessageConverter          | 字符串                 | 纯文本消息     |

#### JSON 转换器配置选项
```java
@Bean
public MessageConverter jsonMessageConverter() {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
    // 设置日期格式
    ObjectMapper mapper = new ObjectMapper();
    mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    converter.setObjectMapper(mapper);
    return converter;
}
```

#### 消息头信息
Jackson2JsonMessageConverter 会自动添加以下消息头：
- `__TypeId__`：类型ID，用于反序列化
- `__ContentTypeId__`：内容类型ID
- `__KeyTypeId__`：键类型ID

---

## 十三、常见问题与解决方案

### 13.1 消息丢失
- 生产者确认 + 持久化 + 消费者手动 ACK

### 13.2 消息重复
- 消费者幂等性处理

### 13.3 消息堆积
- 增加消费者数量
- 使用惰性队列
- 扩容集群

### 13.4 消息顺序错乱
- 使用单队列保证顺序
- 业务层排序

### 13.5 RabbitMQ 内存溢出
- 设置内存高水位线
- 使用磁盘告警
- 增加节点内存

---

## 十四、最佳实践

1. **队列设计**：避免过多队列，合理规划 Exchange 和 RoutingKey
2. **消息设计**：消息体精简，避免携带过多数据
3. **确认机制**：生产者确认 + 消费者手动 ACK
4. **持久化**：关键消息设置持久化
5. **监控告警**：监控队列长度、消费延迟、连接数
6. **集群部署**：生产环境至少 3 节点
7. **死信队列**：为每个业务队列配置死信队列
8. **幂等消费**：所有消费者实现幂等性

---

## 十五、学习资源

- [RabbitMQ 官方文档](https://www.rabbitmq.com/docs)
- [RabbitMQ 中文社区](https://www.rabbitmq.com/)
- 《RabbitMQ 实战指南》
- Spring Boot AMQP 官方文档

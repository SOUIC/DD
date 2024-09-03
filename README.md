基于 SpringCloud+微信小程序实现的代驾平台
- 技术栈：Spring Boot, Spring Cloud Alibaba, MyBatis Plus, Redis, MongoDB, RabbitMQ, Seata, Docker, 微信小程序
- 集成了Spring Cloud微服务架构，实现了服务发现、负载均衡、API网关等功能。
- 开发了基于地理位置的服务（GEO）和规则引擎（Drools）的订单费用计算模块。
- 实现了基于Seata的分布式事务管理机制，确保跨服务的数据一致性。
- 构建了基于XXL-JOB的任务调度系统，用于自动处理订单超时等业务逻辑。
- 开发了基于Redis的分布式锁解决方案，确保司机抢单过程中的数据一致性。
- 实现了Redis缓存策略，减少了数据库访问频率，提高了系统响应速度。
- 构建了基于Redis的消息队列，优化了订单推送流程

# Task 2 Auth And Rbac Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 打通用户注册、登录、JWT 鉴权、网关 Header 透传和基础角色校验，形成后续活动后台接口可复用的认证权限闭环。  
**Architecture:** 用户服务负责用户持久化、密码加密、令牌签发和当前用户查询；`common-security` 下沉 JWT、用户上下文和角色校验能力；Gateway 通过全局过滤器校验 Bearer Token 并把用户信息透传到下游服务。默认用户通过 SQL 种子数据初始化，便于本地联调。  
**Tech Stack:** Spring Boot 3.2.4、Spring Cloud Gateway、MyBatis Plus、MySQL 8、BCrypt、JJWT、JUnit 5、Spring Boot Test。

---

### Task 1: 补齐 Task 2 文件边界与种子数据

**Files:**
- Create: `deploy/sql/02_seed_user.sql`
- Modify: `README.md`
- Modify: `flash-sale-user-service/pom.xml`
- Modify: `flash-sale-gateway/pom.xml`
- Modify: `flash-sale-common/common-security/pom.xml`

- [ ] Step 1: 增加默认管理员、发布方、普通用户种子数据脚本。
- [ ] Step 2: 为用户服务、网关、公共安全模块补齐测试和 JWT 所需依赖。
- [ ] Step 3: 在 README 中补充 Task 2 联调账号说明。

### Task 2: 先写公共安全能力测试

**Files:**
- Create: `flash-sale-common/common-security/src/test/java/com/flashsale/common/security/jwt/JwtTokenServiceTest.java`
- Create: `flash-sale-common/common-security/src/test/java/com/flashsale/common/security/web/RequireRoleAspectTest.java`
- Create: `flash-sale-common/common-security/src/test/java/com/flashsale/common/security/context/UserContextHolderTest.java`

- [ ] Step 1: 先写 JWT 生成、解析、失效校验测试。
- [ ] Step 2: 先写用户上下文持有器的设置、读取、清理测试。
- [ ] Step 3: 先写角色校验注解在未登录和角色不匹配时失败的测试。

### Task 3: 先写用户服务接口测试

**Files:**
- Create: `flash-sale-user-service/src/test/java/com/flashsale/user/interfaces/UserAuthControllerTest.java`
- Create: `flash-sale-user-service/src/test/resources/application-test.yml`

- [ ] Step 1: 先写注册接口测试，验证用户名唯一、密码加密和默认角色。
- [ ] Step 2: 先写登录接口测试，验证用户名密码正确时返回 JWT。
- [ ] Step 3: 先写当前用户查询接口测试，验证 Header 上下文可回显。
- [ ] Step 4: 先写按 ID 查询接口测试，验证后台角色校验生效。

### Task 4: 先写网关鉴权测试

**Files:**
- Create: `flash-sale-gateway/src/test/java/com/flashsale/gateway/security/JwtAuthenticationFilterTest.java`
- Create: `flash-sale-gateway/src/test/resources/application-test.yml`

- [ ] Step 1: 先写登录和注册接口放行测试。
- [ ] Step 2: 先写缺失 Token 返回 401 的测试。
- [ ] Step 3: 先写合法 Token 被解析并透传标准 Header 的测试。

### Task 5: 补最小实现并完成验证

**Files:**
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/context/UserContextHolder.java`
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/jwt/JwtProperties.java`
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/jwt/JwtTokenService.java`
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/auth/RequireRole.java`
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/auth/RequireRoleAspect.java`
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/web/UserContextInterceptor.java`
- Create: `flash-sale-common/common-security/src/main/java/com/flashsale/common/security/config/SecurityAutoConfiguration.java`
- Create: `flash-sale-common/common-security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/domain/UserEntity.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/domain/UserRole.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/domain/UserStatus.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/mapper/UserMapper.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/service/UserAuthService.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/service/UserQueryService.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/web/UserAuthController.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/web/dto/LoginRequest.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/web/dto/RegisterRequest.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/web/dto/LoginResponse.java`
- Create: `flash-sale-user-service/src/main/java/com/flashsale/user/web/dto/UserProfileResponse.java`
- Create: `flash-sale-gateway/src/main/java/com/flashsale/gateway/security/JwtAuthenticationFilter.java`
- Modify: `flash-sale-user-service/src/main/resources/application.yml`
- Modify: `flash-sale-gateway/src/main/resources/application.yml`
- Modify: `flash-sale-user-service/src/main/java/com/flashsale/user/FlashSaleUserApplication.java`
- Modify: `flash-sale-gateway/src/main/java/com/flashsale/gateway/FlashSaleGatewayApplication.java`

- [ ] Step 1: 让公共安全模块测试先通过。
- [ ] Step 2: 让用户服务接口测试通过。
- [ ] Step 3: 让网关鉴权测试通过。
- [ ] Step 4: 执行 `mvn -q -pl flash-sale-common/common-security,flash-sale-user-service,flash-sale-gateway test`。
- [ ] Step 5: 执行 `mvn -q -DskipTests package`。

### Task 6: 更新项目日志

**Files:**
- Modify: `docs/项目日志.md`

- [ ] Step 1: 记录 Task 2 的实现范围、测试命令和剩余风险。

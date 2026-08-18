# AI-MES 后端架构设计

> Spring Boot 3.3 · MyBatis-Plus · Sa-Token · 分层单体架构

## 1. 设计目标

| 目标 | 说明 |
|------|------|
| 职责清晰 | Controller 只做协议适配，Service 承载业务，Converter 做对象转换 |
| 类型安全 | 用 DTO 接收入参、VO 返回出参，减少 `Map<String, Object>` |
| 可演进 | 复杂聚合（工艺 BOM、排产上下文）可阶段性保留 `Map`，逐步 VO 化 |
| 与前端契约稳定 | JSON 字段名与历史 API 保持一致，重构不改变对外形态 |

## 2. 目录结构

```
backend/src/main/java/com/aimes/
├── AiMesApplication.java          # 启动入口
├── common/                        # 横切：统一响应、异常、操作审计
├── config/                        # Spring / Sa-Token / Redis / WebSocket 配置
├── security/                      # 验证码、登录保护、权限接口实现
├── controller/                    # REST 控制器（按业务域，薄层）
├── dto/
│   └── request/                   # 入参 DTO（按域分包）
│       ├── auth/
│       ├── plan/
│       ├── workorder/
│       ├── ...
├── vo/                            # 出参视图对象（按域分包）
│   ├── common/                    # PageResult、OptionVo 等通用结构
│   ├── auth/
│   ├── plan/
│   └── ...
├── converter/                     # Entity / 聚合 → VO 转换（@Component）
├── entity/                        # 数据库实体（与表 1:1）
├── mapper/                        # MyBatis-Plus Mapper
├── service/                       # 业务服务
│   └── coze/                      # Coze 智能体子域
├── util/                          # 无状态工具
└── websocket/                     # WebSocket 推送
```

**资源目录**

```
src/main/resources/
└── application.yml                # 主配置（环境变量注入）
```

**测试**

```
src/test/java/com/aimes/
└── service/                       # Service 单元测试
```

## 3. 分层与数据流

```
HTTP Request
    ↓
Controller          @Valid dto.request.*  校验入参
    ↓
Service             业务逻辑、事务、权限侧效
    ↓
Mapper / Entity     持久化
    ↓
Converter           Entity → vo.*
    ↓
Controller          Result.ok(vo)
    ↓
HTTP Response (JSON)
```

### 3.1 各层职责

| 层 | 职责 | 禁止 |
|----|------|------|
| `controller` | 路由、参数绑定、`@SaCheckPermission`、调用 Service、`Result` 包装 | 直接访问 Mapper、拼业务 Map |
| `dto.request` | 请求体字段、JSR-303 校验注解 | 包含业务逻辑 |
| `vo` | 响应字段、Jackson 序列化格式 | 校验注解、数据库操作 |
| `converter` | 实体→VO、列表组装、关联字段填充 | 事务、修改数据库 |
| `service` | 业务规则、事务边界、通知推送 | 返回裸 Entity 给 Controller |
| `entity` | 表字段映射 | 作为 API 入参/出参 |
| `mapper` | CRUD、条件查询 | 业务判断 |

### 3.2 DTO 与 VO 区分

| 类型 | 包路径 | 命名 | 用途 |
|------|--------|------|------|
| Request DTO | `dto.request.{domain}` | `XxxRequest` / `XxxSaveRequest` | HTTP 入参 |
| View Object | `vo.{domain}` | `XxxVo` / `XxxDetailVo` | HTTP 出参 |
| Entity | `entity` | 表名语义 | 仅持久化层 |

**不必强行引入 Response DTO**：本项目统一用 `vo` 作为出参，与多数 Spring 项目习惯一致。

### 3.3 分页与列表

统一使用 `vo.common.PageResult<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "records": [ ... ]
  }
}
```

带统计的列表（如物料）使用专用 VO，例如 `MaterialListVo { summary, records }`。

## 4. 域分包对照

| 业务域 | request 包 | vo 包 | Controller 前缀 |
|--------|------------|-------|-----------------|
| 认证 | `dto.request.auth` | `vo.auth` | `/api/auth` |
| 生产计划 | `dto.request.plan` | `vo.plan` | `/api/plans` |
| 工单 | `dto.request.workorder` | `vo.workorder` | `/api/work-orders` |
| 异常 | `dto.request.exception` | `vo.exception` | `/api/exceptions` |
| 物料 | `dto.request.material` | `vo.material` | `/api/materials` |
| 班组 | `dto.request.team` | `vo.team` | `/api/teams` |
| 产品/BOM | `dto.request.product` | `vo.product` | `/api/products` |
| 工艺路线 | `dto.request.process` | （阶段二 VO 化） | `/api/process-routes` |
| 设备 | `dto.request.device` | （阶段二 VO 化） | `/api/devices` |
| 设备运维 | `dto.request.deviceops` | （阶段二 VO 化） | `/api/device-*` |
| 质量 | `dto.request.quality` | （阶段二 VO 化） | `/api/quality` |
| Coze | `dto.request.coze` | （阶段二 VO 化） | `/api/coze` |
| 管理后台 | `dto.request.admin` | `vo.admin` | `/api/admin/*` |
| 驾驶舱 | — | `vo.dashboard` | `/api/dashboard` |

## 5. Converter 规范

- 类名：`{Domain}Converter`，包：`com.aimes.converter`
- 标注 `@Component`，通过构造器注入所需 Mapper
- 方法命名：`toVo`、`toDetailVo`、`toBriefVo`、`toSummaryVo`
- 静态标签方法（如 `planStatusLabel`）可放在 Converter 或 Service 工具方法中

示例：

```java
@Component
@RequiredArgsConstructor
public class PlanConverter {
    public PlanVo toVo(ProdPlan plan) { ... }
    public PageResult<PlanVo> toPage(Page<ProdPlan> page) { ... }
}
```

## 6. 统一响应与异常

- 成功：`Result.ok(data)` → `{ code: 200, message: "success", data }`
- 业务异常：`BusinessException` → `GlobalExceptionHandler` → `{ code: 400, message }`
- 未授权：Sa-Token / `BusinessException(401)` → `{ code: 401, message }`

## 7. 迁移阶段（Map → VO）

### 阶段一（已完成 / 进行中）

- [x] 拆分 `Requests.java` → `dto.request.*`
- [x] 建立 `vo.common`、`vo.auth`、`vo.plan`、`vo.workorder` 基础 VO
- [x] 建立核心 `converter` 包
- [x] 迁移：计划、认证、班组、物料、异常、驾驶舱统计、用户/角色管理

### 阶段二（待办）

- [ ] `WorkOrderService`、`ProductService` 全量 VO 化
- [ ] `DeviceService`、`ProcessRouteService` 大型聚合 VO
- [ ] `CozeService` 流式/SSE 响应结构文档化
- [ ] 库存流水 `TransactionVo` 统一物料/产品流水

### 阶段三（可选）

- [ ] 按域拆 `controller` 子包（`controller.admin`、`controller.device`）
- [ ] Service 接口 + Impl（仅在需要多实现或超大类时）
- [ ] MapStruct 替代手写 Converter（类数量 > 80 时考虑）

## 8. 复杂聚合的处理原则

工艺 BOM、工序执行上下文、Coze 排产结果等字段多、结构动态时：

1. **短期**：Service 返回 `Map<String, Object>`，在 ARCHITECTURE 域表中标注「阶段二」
2. **中期**：拆为嵌套 VO（如 `ProcessRouteDetailVo` + `OperationVo` + `ParameterVo`）
3. **禁止**：在 Controller 层拼 Map

`ProductDetailVo.bom` 等字段在过渡期可保留 `Map<String, Object>`，待 `ProcessRouteService` VO 化后替换。

## 9. 配置与安全

- 环境变量：根目录 `.env`，由 `DotEnvLoader` 在启动前加载
- 数据库：手动执行 `sql/init.sql`，`spring.sql.init.mode=never`
- 权限：`@SaCheckPermission` 与 `sys_role_permission` 中文权限项一致
- 上传：`aimes.upload-dir`，默认 `uploads/`（勿提交 Git）

## 10. 新增 API 检查清单

1. 入参：新建 `dto.request.{domain}.XxxRequest`，加校验注解
2. 出参：新建 `vo.{domain}.XxxVo`，在 Converter 中转换
3. Service 返回 VO，不返回 Map（除阶段二域）
4. Controller 返回 `Result<XxxVo>` 或 `Result<PageResult<XxxVo>>`
5. Knife4j：`@Tag`、`@Operation`（推荐）
6. 权限：`@SaCheckPermission`
7. 写操作：考虑 `@OperationLog` + `OperationLogRunner`

## 11. 构建与测试

```bash
cd backend
mvn compile          # 编译
mvn test             # 单元测试
mvn spring-boot:run  # 本地启动
```

接口文档：http://localhost:8080/doc.html

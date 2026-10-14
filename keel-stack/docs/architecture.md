# 架构与选型

## 一句话概括

三端围绕 **一个 HTTP 接口 + 一条实时通道 + 一份数据库** 组织：

- HTTP 负责「读写」——查数据、改数据
- Socket.IO 负责「通知」——数据变了，告诉该知道的人
- MySQL 是唯一事实来源；Redis 只做缓存和自增序号，**没有它也能跑**

这条边界是骨架最重要的一条设计线：**任何写操作先落库，成功之后再广播事件。**
前端不轮询、不猜状态，收到事件就重新拉一次自己关心的数据。

## 技术选型

| 层 | 选型 | 为什么 |
|---|---|---|
| 服务端运行时 | Node.js 18+（ESM） | 与前端同语言，工具链一致；ESM 让依赖关系显式 |
| Web 框架 | Koa + @koa/router | 中间件洋葱模型直观，`app.js` 里一眼能看出请求会经过哪些关卡 |
| 参数校验 | Zod | 校验规则既是运行时防线，也是接口文档；解析失败统一转 400 |
| 数据库 | MySQL 8 + mysql2 | `dateStrings` + `decimalNumbers` 让 DATETIME/DECIMAL 直接以字符串/数字返回，省掉前端类型转换 |
| 缓存 | Redis 7 + ioredis | 只做两件事：读缓存、自增序号；**所有调用都吞异常并降级**，不做主流程依赖 |
| 鉴权 | jsonwebtoken + bcryptjs | 控制台用 JWT（无状态），密码只存 bcrypt 哈希 |
| 实时 | Socket.IO | 自动重连、房间、降级到长轮询；比裸 WebSocket 省掉一堆边界处理 |
| 上传 | @koa/multer + koa-static | 落盘到 `uploads/`，元数据进 `media` 表；静态服务直接挂 `/uploads` |
| 后台 | Vue 3 + TypeScript + Vite | Vite 的 dev proxy 把 `/api`、`/uploads`、`/socket.io` 一次性打通，前端只写相对路径 |
| UI | Ant Design Vue 4 | 组件齐、Design Token 体系成熟，主题色改一处全局生效 |
| 状态 | Pinia | 组合式写法，跟 Vue 3 同构 |
| App | Kotlin + Jetpack Compose | 声明式 UI，改状态即改界面；配 Material3 自定义 token |
| App 网络 | Retrofit + Gson + OkHttp | 接口写成 interface，调用像本地方法；OkHttp 统一加 header、复用连接池 |
| App 图片 | Coil | 一行加载远程图，`LocalPhoto` 里统一处理相对路径补全 |
| App 实时 | socket.io-client | 与服务端同一套协议与房间语义 |

> 刻意**没有**引入 ORM、Repository 抽象层、GraphQL、微服务拆分。
> 这个规模的项目里，它们带来的抽象成本大于收益。见文末「有意为之的取舍」。

## 分层

### 服务端

```
src/
├── app.js / server.js   装配中间件、起 HTTP + Socket.IO、优雅退出
├── config/             唯一读 env 的地方，其余代码只 import config
├── db/                 连接池与查询辅助（query/queryOne/insert/execute/transaction）
│                       redis 封装（cacheGet/cacheSet/nextSequence，全部可降级）
│                       migrate / seed / reset 三个脚本
├── middlewares/        auth（JWT / device）、error（统一兜底）、upload、validate（Zod）
├── routes/             HTTP 路由，只做「取参数 → 调 service → 落响应 → 发事件」
├── services/           业务逻辑与 SQL，一个领域一个文件
├── socket/             房间定义与事件投递（emitXxx）
└── utils/              response（统一响应/异常）、jwt、status（状态机工厂）
```

依赖方向严格单向：`routes → services → db`。中间件横切在 routes 之前。

### 后台控制台

```
src/
├── api/client.ts       axios 实例 + 拦截器（注入 token、解包 {code,message,data}、401 跳登录）
├── api/index.ts        按领域分组的 API 函数（authApi / itemApi / ...）
├── stores/             Pinia：auth（登录态）、realtime（socket 连接与事件）
├── router/             路由表；子路由的 meta 同时是侧边栏菜单的数据源
├── layouts/            BasicLayout：侧边栏 + 顶栏 + 内容区
├── theme/              Design Token（颜色、圆角、组件级覆盖）
└── views/              页面，只负责「取数 + 渲染 + 交互」
```

### Android 客户端

```
app/src/main/java/com/keel/app/
├── MainActivity.kt     导航壳：角色切换、NavHost 路由表、底部导航、通知横幅、反馈 toast
├── AppViewModel.kt     状态中心：偏好、条目数据、实时事件、站内消息、版本更新
├── data/AppModels.kt   本地领域模型 + Role 枚举
├── data/remote/        ApiClient（Retrofit 实例与地址切换）、ApiService（接口定义）
│                       Dto（服务端结构）、Mappers（DTO → 领域模型）、RealtimeClient（socket）
└── ui/                 theme（配色与字体）、components（通用组件）、各业务包（页面）
```

## 关键约定

### 1. 统一响应

所有接口都返回 `{ code, message, data }`，`code = 0` 表示成功。
控制台的 axios 拦截器就地把 `data` 拆出来，所以调用方直接拿到业务对象；
App 端保留 `ApiResponse<T>` 包装，`resp.ok` 即 `code == 0`。

### 2. 错误只有两个出口

业务代码里抛 `BizError`（`badRequest` / `unauthorized` / `forbidden` / `notFound`），
参数校验抛 `ZodError`。两者最终都由 `middlewares/error.js` 转成统一响应，
**路由里不写 try/catch**。

### 3. 状态机集中定义

`utils/status.js` 的 `createStateMachine({ text, transitions })` 一次定义三件事：
英文枚举、中文文案、合法流转。接口同时下发 `status`、`statusText`、`allowNext`，
前端按钮直接由 `allowNext` 驱动，不需要照抄一份状态映射表（也就不存在两边不一致的问题）。

### 4. 命名：库里 snake_case，接口上 camelCase

`services` 里的 `toDTO()` 负责转换。前端只认 camelCase，永远不用关心数据库列名。

### 5. 实时事件命名 `域:动作`

`item:changed`、`item:status`、`notification:new`、`release:new`。
房间与消息投递目标的值域统一为 `ALL / CLIENT / ADMIN`。

### 6. 图片「先入库，再引用」

上传落盘 + 写 `media` 记录，业务表只存 URL。`media.ref_count` 记录被引用次数，
大于 0 时禁止删除。新增带图片的业务表时，把它加进 `services/media.js` 的引用统计即可。

## 有意为之的取舍

| 取舍 | 原因 |
|---|---|
| 不引入 ORM | 表结构简单、SQL 可控；排查问题时直接看到最终 SQL，比猜 ORM 生成什么更快 |
| services 直接调 db helper | 再加一层 Repository 只是把同样的 SQL 换个地方写，收益低 |
| 不做离线兜底 | 骨架不塞本地假数据——连不上就明确提示，避免"看起来能用其实数据是假的" |
| 不做角色/权限体系 | 骨架只保留一个 `admin`；需要 RBAC 时给 `admin_users` 加 `role`、在 `requireAdmin` 里判断 |
| 控制台不引入图表库 | 概览页只用 `a-statistic` 展示计数，避免为演示引入重量级依赖 |
| App 不做混淆 | 内网自用，`isMinifyEnabled = false` 少一类「R8 缺规则导致崩溃」的坑 |
| 一个端口托管全部 | HTTP、图片、WebSocket 同端口，内网部署不需要 reverse proxy 配置 |

## 已知边界

- 骨架按内网/开发环境设计，明文 HTTP、CORS 全开、默认口令都只适合本机或局域网。
- App 用的是「设备标识」而非账号体系，多设备共享数据但无法区分人。
- Socket.IO 未做鉴权：任何客户端都能订阅房间。内网够用，公网需自行加 token 校验。

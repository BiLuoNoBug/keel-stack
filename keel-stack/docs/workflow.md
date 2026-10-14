# 开发流程

## 1. 日常循环

三端各起各的，改哪端启哪端，互不阻塞：

| 端 | 命令 | 热更新 |
|---|---|---|
| 服务端 | `cd server && npm run dev` | `node --watch`，改完自动重启 |
| 控制台 | `cd admin && npm run dev` | Vite HMR，浏览器即时生效 |
| App | Android Studio 点 Run，或 `./gradlew :app:installDebug` | Live Edit / 重跑 |

依赖只启一次：`cd server && docker compose up -d`。

**联调顺序建议**：先 `/api/health` 确认服务端在 → 控制台能登录（JWT 通了）→ App 能拉到列表（网络层通了）→ 两边都显示「实时同步中 / 实时连接」（Socket.IO 通了）。这四步过完，链路就全通了。

---

## 2. 新增一个业务模块（三端同步）

骨架里 `item` 就是标准答案。假设你要加一个 `book`，**六处、按顺序做**：

### ① 建表（服务端）

新增 `server/sql/02-book.sql`，**不要改 `01-schema.sql`**（它已经跑过，改动不会重放）。

```sql
CREATE TABLE IF NOT EXISTS books (
  id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
  title      VARCHAR(64) NOT NULL,
  cover_url  VARCHAR(255) DEFAULT NULL,
  status     ENUM('DRAFT','ON_SHELF','OFF_SHELF') NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

然后 `npm run db:migrate`（或 `db:reset` 重建）。

### ② 服务层

新增 `server/src/services/book.js`，**照抄 `item.js` 的形状**：

```
toDTO(row)          —— snake_case → camelCase，同时补 statusText / allowNext
listBooks(...)      —— 条件拼接 + COUNT + LIMIT/OFFSET，返回 { list, total, page, pageSize }
getBook(id)         —— 查不到抛 notFound
createBook(input)
updateBook(id, input)
updateStatus(id, next)
removeBook(id)
```

### ③ 校验

`server/src/middlewares/validate.js` 加一段：

```js
export const bookInput = z.object({
  title: z.string().trim().min(1, '书名不能为空').max(64),
  coverUrl: z.string().trim().max(255).nullish(),
  status: z.enum(['DRAFT', 'ON_SHELF', 'OFF_SHELF']).default('DRAFT')
})
```

### ④ 路由 + 事件

- `server/src/routes/app.js`：客户端要用的接口（列表/详情/写操作），挂 `/api/app/books`
- `server/src/routes/admin.js`：控制台要用的接口，挂 `/api/admin/books`（这一段在 `router.use(requireAdmin)` 之后，自动带鉴权）
- **写操作记得广播**：新增 `socket/index.js` 里的 `emitBookChanged` / `emitBookStatus`，在路由里调

> 路由注册顺序有坑：更具体的路径要放在带参数的路由之前，比如 `/books/status/count` 必须写在 `/books/:id` 前面。

### ⑤ 控制台

四小步，都在 `admin/src/`：

```
types/index.ts      加 Book / BookPayload / BookStatus 类型
api/index.ts        加 bookApi = { list, create, update, updateStatus, remove }
views/BookListView.vue    照抄 ItemListView.vue（表格 + 抽屉表单 + 状态流转 + 删除）
router/index.ts     加一条子路由（meta 里写 title/sub/icon，侧边栏菜单会自动出现）
```

如果列表要响应实时事件：`watch(() => realtime.tick, ...)`，`stores/realtime.ts` 里加对应的
`socket.on('book:changed', ...)`。

### ⑥ App

`app/src/main/java/com/keel/app/`：

```
data/remote/Dto.kt       加 BookDto
data/remote/ApiService.kt 加 @GET/@POST/@PUT/... 方法
data/remote/Mappers.kt   加 BookDto.toModel()
data/AppModels.kt        加 Book 领域模型
ui/book/BookScreens.kt   列表 / 详情 / 编辑页（照抄 ui/items/ItemScreens.kt）
AppViewModel.kt          加数据状态与 load/create/update/delete 方法 + 处理 book:changed 事件
MainActivity.kt          路由表加几条 composable
```

### 替换清单速查

| 要改的地方 | item 里的样子 | 换成你自己的 |
|---|---|---|
| 建表 | `sql/01-schema.sql` 的 `items` | 新建 `sql/02-xxx.sql` |
| 服务 | `services/item.js` | `services/xxx.js` |
| 校验 | `validate.js` 的 `itemInput` | `xxxInput` |
| 客户端路由 | `routes/app.js` 的 `/items` | `/xxx` |
| 控制台路由 | `routes/admin.js` 的 `/items` | `/xxx` |
| 实时事件 | `socket/index.js` 的 `emitItemChanged` | `emitXxxChanged` |
| 媒体引用 | `services/media.js` 的 `refreshAllRefCounts` | 新表也存图就加进去 |
| 控制台类型/接口/页面/路由 | `types`、`api`、`ItemListView.vue`、`router` | `BookListView.vue` 等 |
| App DTO/接口/映射/模型/页面/路由 | `Dto`、`ApiService`、`Mappers`、`AppModels`、`ItemScreens`、`MainActivity` | 同名替换 |

> 一句话：**服务端定契约 → 控制台做管理面 → App 做使用面 → 中间用 Socket.IO 打通。**

---

## 3. 接口约定速查

| 约定 | 规则 |
|---|---|
| 前缀 | 客户端 `/api/app/**`；控制台 `/api/admin/**`（除 login 外都要 JWT） |
| 响应 | `{ code, message, data }`，`code = 0` 成功；失败 `code` 等于 HTTP 状态码 |
| 分页 | 入参 `page` / `pageSize`，返回 `{ list, total, page, pageSize }` |
| 写操作 | 返回被写入的实体（方便前端直接替换本地那条），不在响应里塞额外字段 |
| 参数校验 | 用 Zod schema + `validateBody(...)`，失败统一 400，路由里不 try/catch |
| 业务异常 | 抛 `badRequest/unauthorized/forbidden/notFound` |
| 枚举 | 库里存英文，接口额外下发中文 `xxxText`；合法流转下发 `allowNext` |

---

## 4. 实时事件约定

- 命名：`域:动作`（`item:changed`、`item:status`、`notification:new`、`release:new`）
- 订阅：连上后 `emit('subscribe', { role, deviceId })`；`role=admin` 进 `room:admin`，其余进 `room:client`
- 投递：服务端只暴露 `emitTo(room, event, payload)` 这类函数，业务里不要直接摸 `io`
- 前端：事件到达只改 store 状态并 `tick++`，页面 `watch(tick)` 重新拉数据 —— **不要在事件回调里直接改 UI 列表**，避免多端覆盖

---

## 5. 调试

| 症状 | 先查什么 |
|---|---|
| 服务端起不来 | `docker compose ps` 看 MySQL 是否健康；看启动日志里的 `[mysql] 连接失败` |
| 接口 500 | 服务端控制台会打印 `[error] METHOD PATH` 与堆栈 |
| 接口 400 | Zod 校验不通过，响应 message 里会带具体字段与原因 |
| 控制台登录后立刻被踢回登录页 | JWT 校验失败：确认 `.env` 的 `JWT_SECRET` 没变、token 没过期 |
| 控制台「实时同步」一直是断开 | Vite proxy 的 `/socket.io` 必须带 `ws: true`；服务端 socket 是否初始化 |
| App 拉不到数据 | 服务地址页看地址；真机不在同网段就 `adb reverse tcp:8080 tcp:8080` 后用 `127.0.0.1` |
| App 收到推送崩溃/不刷新 | socket 回调在子线程，必须切回主线程再写 Compose 状态（`RealtimeClient.post` 已处理） |
| 想推倒重来 | `cd server && npm run db:reset`（会删表重建 + 灌种子） |

---

## 6. 打包与交付

### 服务端

```bash
cd server
npm ci --omit=dev
NODE_ENV=production npm start
```

用 systemd / pm2 / Docker 都行；关键是保证 `.env` 里是生产值（`JWT_SECRET`、数据库口令、`PUBLIC_BASE_URL` 指向对外可达地址，否则 App 拼出来的图片地址会指向 127.0.0.1）。

### 控制台

```bash
cd admin
npm run build          # 产出 dist/，含 typescript 类型检查
```

把 `dist/` 交给任意静态服务器即可，但**必须把三个路径反代到服务端**，否则登录和实时都会挂：

```
/api      -> http://<server>:8080
/uploads  -> http://<server>:8080
/socket.io -> http://<server>:8080   （需要 WebSocket 升级）
```

若 API 不在同域，可在构建时用 `VITE_API_BASE` 指定前缀（见 `admin/.env.development`）。

### Android

1. 生成签名密钥（只做一次，**不要入库**）：

   ```bash
   keytool -genkeypair -v -keystore keystore/release.jks -alias app -keyalg RSA -keysize 2048 -validity 10000
   ```

2. 在仓库根写 `keystore.properties`（已被 `.gitignore` 忽略）：

   ```properties
   storeFile=keystore/release.jks
   storePassword=...
   keyAlias=app
   keyPassword=...
   ```

3. 打包：

   ```bash
   ./gradlew :app:assembleRelease -PapiHost=https://your-server.example.com
   ```

4. 把 APK 放到服务端能访问的地址，然后在**控制台「版本管理」**里新增一条版本记录（填 `versionCode`、`versionName`、`changelog`、APK 的 `downloadUrl`），点发布。

   App 启动时会请求 `/api/app/releases/latest`，并监听 `release:new`：一旦有新版本会提示，用户在「关于」页点一下即可应用内下载并拉起安装器。

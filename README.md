# Keel Stack · 三端工程骨架

一套可以直接开工的「Node 服务端 + Web 后台 + Android App」最小工程。

它不包含任何具体业务，只把**可复用的壳子**先搭好：统一响应、错误兜底、JWT 后台鉴权、
图片上传与媒体库、Socket.IO 实时通道、站内消息、App 远程版本升级、双角色导航、主题系统、
请求层与 Pinia / Retrofit 封装……然后留一个**与业务无关的示例模块 `item`（条目）**，
把三端串成一条完整链路——从数据库表一路到 App 页面，照着抄就能接自己的业务。

> 结论先说：**框架能力齐全，业务为零。** 拿来当新项目的起点，或者当一套「三端怎么配合」的参考实现。

```
keel-stack/
├── server/      Node.js 服务端（Koa + MySQL + Redis + Socket.IO）
├── admin/       后台控制台（Vue 3 + TypeScript + Ant Design Vue）
├── app/         Android 客户端（Kotlin + Jetpack Compose）
├── docs/        架构说明与开发流程
├── gradle/      Gradle Wrapper（gradlew / gradlew.bat）
└── README.md
```

## 快速开始

### 0. 准备

- Node.js 18+（开发时用的是 24）、Docker、JDK 17
- Android 端另需 Android SDK（API 34）与一台 API 24+ 的设备/模拟器

### 1. 起 MySQL 与 Redis

```bash
cd server
docker compose up -d          # MySQL 3307 / Redis 6380，刻意错开本机常见的 3306 / 6379
```

首次启动会自动执行 `server/sql/*.sql` 建表。

### 2. 起服务端

```bash
cd server
cp .env.example .env
npm install
npm run db:seed               # 写入管理员 + 示例条目 + 一条版本记录
npm start                     # http://127.0.0.1:8080
```

接口、图片、实时通道都在同一个端口：`/api/**`、`/uploads/**`、`/socket.io`。
先访问 `http://127.0.0.1:8080/api/health` 确认活着。

### 3. 起后台控制台

```bash
cd admin
npm install
npm run dev                   # http://127.0.0.1:5174
```

默认账号 `admin` / `admin123`（来自 `server/src/db/seed.js`），登录后可在「系统设置」改密码。

### 4. 跑 Android 客户端

用 Android Studio 打开本目录，Gradle JDK 选 **JDK 17**，运行 `app`。命令行构建：

```bash
./gradlew :app:assembleDebug -PapiHost=http://192.168.1.100:8080
```

APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。

服务端地址有两个来源，优先级从高到低：

1. App 内「我的 → 设置 → 服务地址」手动改（存本地，立刻生效）
2. 编译期默认值 `app/build.gradle.kts` 的 `apiHost`，可用 `-PapiHost=...` 覆盖

真机跑的时候如果手机和电脑不在同一网段（比如手机走流量），可以用 USB 反向映射绕过去：

```bash
adb reverse tcp:8080 tcp:8080     # 手机的 127.0.0.1:8080 → 电脑的 8080
```

然后把 App 的服务地址改成 `http://127.0.0.1:8080`。

## 架构

### 数据流

```
Android App ──HTTP──┐
                    ├──> Koa ──> MySQL（唯一事实来源）
后台控制台 ──HTTP──┘         └─> Redis（可降级：缓存 / 自增序号）
        ▲   ▲
        └───┴── Socket.IO 实时通道（新条目、状态流转、站内消息、新版本）
```

### 鉴权模型

| 端 | 方式 |
|---|---|
| 后台控制台 | `/api/admin/**`，除 `login` 外都要 Bearer JWT（`scope: admin`） |
| App | `/api/app/**`，不做登录，用 `x-device-id` 头区分设备（默认 `default`） |

App 端要接真正的账号体系时，只需替换 `server/src/middlewares/auth.js` 里的 `deviceId` 中间件，
其余路由与业务代码不用动。

### 实时事件

客户端连上后 `emit('subscribe', { role, deviceId })` 自报身份，服务端据此放进房间
（`admin` → `room:admin`，其余 → `room:client`）。

| 事件 | 触发时机 | 推给 |
|---|---|---|
| `item:changed` | 条目增删改、发布/下架 | 客户端、控制台 |
| `item:status` | 条目状态流转 | 客户端、控制台 |
| `notification:new` | 控制台/App 发了一条站内消息 | 按 `target`（ALL / CLIENT / ADMIN） |
| `release:new` | 控制台发布了新版本 | 客户端、控制台 |

### 数据表

框架自带：`admin_users`（后台账号）、`media`（图片媒体库）、`notifications`（站内消息）、
`app_releases`（App 版本）。
示例模块：`items`（条目）。

## 骨架自带的能力

**服务端**：集中配置与 `.env`、MySQL 连接池与查询辅助、可降级 Redis 缓存与自增序号、
统一响应 `{ code, message, data }`、业务异常与 Zod 校验的统一落地、JWT 鉴权、图片上传（含 mime 白名单）、
媒体库与引用计数、站内消息与推送、App 版本发布与更新检查、Socket.IO 房间与事件投递、
建表 / 灌种子 / 重置三个脚本。

**后台控制台**：Layout 骨架（可收起侧边栏 + 顶栏标题 + 实时连接指示灯 + 未读角标 + 用户下拉）、
路由即菜单（读 `meta` 自动生成）、登录守卫与回跳、axios 统一解包与 401 处理、
Pinia（auth / realtime）、Ant Design Vue 主题 token 与全局样式、图片选择器、
媒体库页、消息推送页、版本管理页、账号与系统设置页。

**Android 客户端**：Compose 主题系统（品牌色 + 深浅色 + 双角色配色）、
双角色导航壳（角色切换 / 两套底部导航 / 按角色订阅房间）、
顶部通知横幅（仿系统通知，带震动）、网络层（Retrofit + OkHttp、地址热切换、上传、带进度下载、相对 URL 补全）、
Socket.IO 客户端（回调切主线程）、16 个通用 Compose 组件、设置 / 服务地址 / 关于页（含检查更新与 App 内安装）。

## 示例模块 item

唯一带「业务味」的地方，用来演示一条链路怎么走通，**与任何真实业务无关**：

- `server/src/services/item.js`：表结构 → `toDTO` → 列表/详情/新增/更新/状态流转/删除/计数
- `server/src/routes/app.js`、`routes/admin.js`：`/api/app/items`、`/api/admin/items` 两套路由
- `server/sql/01-schema.sql`：`items` 表
- `admin/src/views/ItemListView.vue`：表格 + 抽屉表单 + 封面选图 + 状态流转 + 发布开关
- `app/.../ui/items/ItemScreens.kt`：列表（分页/筛选）+ 详情 + 编辑 + 状态流转 + 删除

状态流转（`TODO → DOING → DONE`）定义在 `server/src/utils/status.js` 的 `createStateMachine`，
接口下发 `statusText`（中文文案）和 `allowNext`（合法的下一步），前端不用自己维护状态映射表。

## 接下来读什么

- [`docs/architecture.md`](docs/architecture.md) —— 技术选型、分层、关键约定与设计取舍
- [`docs/workflow.md`](docs/workflow.md) —— 日常开发流程、**新增一个业务模块的三端同步步骤**、打包交付

## 部署提醒

骨架按**内网/开发环境**设计。上线前至少要：

- 换掉 `JWT_SECRET`、MySQL 密码，`admin_users` 别用默认口令
- 控制台不要直接暴露到公网；正经用就给 `/api/admin/**` 加上 HTTPS 与访问控制
- App 的 `usesCleartextTraffic="true"` 只适合内网明文 HTTP，公网请换 HTTPS 并去掉它

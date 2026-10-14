-- Keel 三端工程骨架 —— 数据库结构
--
-- 容器首次启动会自动执行本目录下的 .sql（挂载到 /docker-entrypoint-initdb.d）；
-- 也可以手动执行：npm run db:migrate
--
-- 这份表结构只包含「框架自带的能力」和「示例模块 item」。
-- 加入自己的业务表时，在本目录新增 02-xxx.sql，不要改这个文件。

SET NAMES utf8mb4;
SET time_zone = '+08:00';

-- ---------------------------------------------------------------- 后台管理员
CREATE TABLE IF NOT EXISTS admin_users (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  username      VARCHAR(32) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  nickname      VARCHAR(32) NOT NULL DEFAULT '',
  avatar_url    VARCHAR(255) DEFAULT NULL,
  last_login_at DATETIME DEFAULT NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员';

-- ---------------------------------------------------------------- 图片媒体库
CREATE TABLE IF NOT EXISTS media (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  filename      VARCHAR(255) NOT NULL,
  url           VARCHAR(255) NOT NULL,
  mime          VARCHAR(64)  NOT NULL DEFAULT '',
  size          INT UNSIGNED NOT NULL DEFAULT 0,
  ref_count     INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '被引用次数，>0 时不允许删除',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_url (url),
  KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片媒体库';

-- ---------------------------------------------------------------- 站内消息
CREATE TABLE IF NOT EXISTS notifications (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  title         VARCHAR(64)  NOT NULL,
  content       VARCHAR(255) NOT NULL,
  link          VARCHAR(64)  DEFAULT NULL COMMENT '客户端跳转标识，骨架里不绑定业务',
  level         ENUM('INFO','IMPORTANT') NOT NULL DEFAULT 'INFO',
  target        ENUM('ALL','CLIENT','ADMIN') NOT NULL DEFAULT 'ALL',
  pushed        TINYINT NOT NULL DEFAULT 0,
  pushed_at     DATETIME DEFAULT NULL,
  push_count    INT UNSIGNED NOT NULL DEFAULT 0,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_created (created_at),
  KEY idx_pushed  (pushed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内消息';

-- ---------------------------------------------------------------- App 版本
CREATE TABLE IF NOT EXISTS app_releases (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  version_code  INT UNSIGNED NOT NULL COMMENT '单调递增的整型版本号',
  version_name  VARCHAR(32)  NOT NULL DEFAULT '',
  changelog     TEXT         DEFAULT NULL,
  download_url  VARCHAR(255) NOT NULL DEFAULT '',
  force_update  TINYINT NOT NULL DEFAULT 0,
  published     TINYINT NOT NULL DEFAULT 1,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_version_code (version_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App 版本发布';

-- ---------------------------------------------------------------- 示例模块：item（条目）
-- 与具体业务无关的样板表：新增自己的业务表时照抄这一段的写法。
CREATE TABLE IF NOT EXISTS items (
  id            INT UNSIGNED NOT NULL AUTO_INCREMENT,
  title         VARCHAR(64)  NOT NULL,
  category      VARCHAR(32)  NOT NULL DEFAULT '默认分类',
  content       TEXT         DEFAULT NULL,
  cover_url     VARCHAR(255) DEFAULT NULL,
  status        ENUM('TODO','DOING','DONE') NOT NULL DEFAULT 'TODO',
  priority      ENUM('LOW','NORMAL','HIGH') NOT NULL DEFAULT 'NORMAL',
  published     TINYINT NOT NULL DEFAULT 1,
  sort_order    INT NOT NULL DEFAULT 0 COMMENT '排序，越小越前',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_category  (category),
  KEY idx_status    (status),
  KEY idx_sort      (sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='示例模块：条目';

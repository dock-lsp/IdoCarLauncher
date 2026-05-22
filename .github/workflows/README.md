# GitHub Actions 构建工作流

本项目使用 GitHub Actions 自动构建 Debug 和 Release 版本的 APK。

## 工作流说明

### build.yml

自动构建 APK 的主工作流，支持以下触发方式：

1. **自动触发**
   - `push` 到 `main` 或 `develop` 分支
   - 创建版本标签 `v*` (如 v1.0.0)
   - `pull_request` 到 `main` 分支

2. **手动触发** (workflow_dispatch)
   - 可选择构建类型：debug / release / both

## 构建输出

| 类型 | 输出文件名 | 签名状态 |
|------|-----------|---------|
| Debug | `IdoCarLauncher-v{run_number}-debug.apk` | 使用 Debug 密钥签名 |
| Release | `IdoCarLauncher-v{run_number}-release.apk` | 使用上传的密钥签名（如有） |

## 配置 Release 签名

要为 Release APK 配置正式签名，需要在 GitHub 仓库设置中添加以下 Secrets：

### 1. 生成签名密钥

```bash
keytool -genkey -v -keystore idocar.keystore -alias idocar -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Base64 编码密钥文件

```bash
base64 -i idocar.keystore -o keystore.base64
```

### 3. 在 GitHub 仓库设置 Secrets

进入仓库 Settings → Secrets and variables → Actions，添加以下 Secrets：

| Secret 名称 | 说明 |
|------------|------|
| `KEYSTORE_BASE64` | 密钥文件的 Base64 编码内容 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

### 配置步骤图示

```
GitHub 仓库 → Settings → Secrets and variables → Actions → New repository secret

┌─────────────────────────────────────────────────────────┐
│  Name: KEYSTORE_BASE64                                  │
│  Secret: (粘贴 keystore.base64 文件内容)                 │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Name: KEYSTORE_PASSWORD                                │
│  Secret: (你的密钥库密码)                                │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Name: KEY_ALIAS                                        │
│  Secret: (你的密钥别名，如 idocar)                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  Name: KEY_PASSWORD                                     │
│  Secret: (你的密钥密码)                                  │
└─────────────────────────────────────────────────────────┘
```

## 下载构建产物

### 方式一：从 Actions 页面下载
1. 进入仓库 Actions 页面
2. 选择最新的工作流运行
3. 在 Artifacts 部分下载 APK

### 方式二：从 Release 页面下载
创建版本标签后，APK 会自动上传到对应的 Release：
```bash
git tag -a v1.0.1 -m "Release v1.0.1"
git push origin v1.0.1
```

## 手动触发构建

1. 进入仓库 Actions 页面
2. 选择 "Build APK" 工作流
3. 点击 "Run workflow"
4. 选择构建类型：debug / release / both
5. 点击 "Run workflow"

## 构建状态

[![Build APK](https://github.com/dock-lsp/IdoCarLauncher/actions/workflows/build.yml/badge.svg)](https://github.com/dock-lsp/IdoCarLauncher/actions/workflows/build.yml)

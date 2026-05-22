# 贡献指南

感谢您对 IdoCarLauncher 项目的关注！我们欢迎并感谢所有形式的贡献。

## 如何贡献

### 报告问题

如果您发现了 Bug 或有功能建议，请通过 [GitHub Issues](https://github.com/yourusername/IdoCarLauncher/issues) 提交。

提交问题时，请包含以下信息：
- 问题的简要描述
- 复现步骤
- 期望行为和实际行为
- 设备信息（Android 版本、设备型号）
- 截图或日志（如有）

### 提交代码

1. **Fork 仓库**
   ```bash
   git clone https://github.com/yourusername/IdoCarLauncher.git
   cd IdoCarLauncher
   ```

2. **创建特性分支**
   ```bash
   git checkout -b feature/your-feature-name
   # 或
   git checkout -b fix/your-bug-fix
   ```

3. **提交更改**
   ```bash
   git add .
   git commit -m "feat: 添加新功能描述"
   ```

4. **推送到您的 Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **创建 Pull Request**
   - 在 GitHub 上创建 PR
   - 描述您的更改
   - 关联相关的 Issue（如有）

### 代码规范

#### Kotlin 代码风格
- 遵循 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用 4 空格缩进
- 最大行长度 120 字符
- 使用有意义的变量和函数名

#### 提交信息规范
使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型 (type)：**
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行的变动）
- `refactor`: 重构
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

**示例：**
```
feat(navigation): 添加实时路况显示

- 集成高德路况 API
- 在导航卡片显示拥堵信息
- 支持自动路线重新规划

Closes #123
```

#### 代码注释
- 使用 KDoc 格式为公共 API 添加文档
- 复杂逻辑添加行内注释
- 使用中文或英文注释，保持统一

```kotlin
/**
 * 启动导航到指定目的地
 *
 * @param destination 目的地名称或地址
 * @param route 导航路线信息
 * @throws NavigationException 当导航初始化失败时抛出
 */
fun startNavigation(destination: String, route: NavigationRoute) {
    // 实现代码
}
```

### 开发环境设置

#### 要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

#### 构建项目
```bash
# 调试构建
./gradlew assembleDebug

# 发布构建
./gradlew assembleRelease

# 运行测试
./gradlew test

# 代码检查
./gradlew lint
```

### 测试

#### 单元测试
- 为 ViewModel 编写单元测试
- 使用 MockK 进行模拟
- 测试覆盖率目标：70%+

#### UI 测试
- 关键用户流程需要 UI 测试
- 使用 Espresso 框架

### 文档

- 更新 README.md（如添加新功能）
- 更新 FEATURES.md（功能变更）
- 更新 CHANGELOG.md（版本更新）
- 代码中的公共 API 需要 KDoc 注释

### 插件开发

如果您想开发插件，请参考 [插件开发指南](EXTENSIONS.md#插件开发)。

## 行为准则

### 我们的承诺

为了营造一个开放和友好的环境，我们作为贡献者和维护者承诺：

- 尊重不同的观点和经验
- 接受建设性的批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

### 不可接受的行为

- 使用带有性暗示的语言或图像
- 挑衅、侮辱/贬损的评论，以及个人或政治攻击
- 公开或私下的骚扰
- 未经明确许可发布他人的私人信息
- 其他不道德或不专业的行为

## 联系方式

- GitHub Issues: [https://github.com/yourusername/IdoCarLauncher/issues](https://github.com/yourusername/IdoCarLauncher/issues)
- 邮件: developer@idocar.com

## 许可证

通过贡献代码，您同意您的贡献将在 [MIT 许可证](LICENSE) 下发布。

---

再次感谢您对 IdoCarLauncher 的贡献！

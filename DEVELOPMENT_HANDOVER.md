# NexusApp 开发总结交接文档

> 编写日期：2026-05-13
> 项目状态：HTML原型完成，Android Kotlin骨架已搭建，待最终移植整合

---

## 一、项目概述

**项目名称**：NexusApp
**包名**：com.nexus.app
**定位**：科幻风格移动端应用，深空Cyberpunk视觉体系
**目标设备**：6.71英寸手机（1080×2400, ~400dpi）
**技术栈**：
- Android：Kotlin + Material3 + SurfaceView自绘
- 构建：Gradle 8.5 + AGP 8.2.2 + JDK 17
- CI/CD：GitHub Actions（push到main自动构建Debug/Release APK）

---

## 二、完整用户流程

### 流程1：开屏动画（6.2秒）
```
深空星场 → 核心光球亮起 → 能量弧线螺旋汇聚 → 几何织网绘制 → 六边形Logo凝聚 → 文字逐字显现 → 进度条完成
```

**粒子系统明细**：
| 系统 | 数量 | 说明 |
|------|------|------|
| 3层视差星辰 | 400+150+50 | 幂次大小分布，独立闪烁，色温偏移，视差漂移 |
| 宇宙尘 | 250 | 噪声漂移+拖尾线段+引力向心收缩 |
| 能量球 | 160 | 螺旋涌入+摆动+双帧软粒子拖尾 |
| 能量丝带 | 10 | 渐变色多段曲线+末端发光 |
| 动态火花 | ≤100 | 速度衰减+拖尾线段 |

**后期特效**：核心多层光晕、双层冲击波、Anamorphic镜头光晕、Film Grain胶片颗粒、暗角

### 流程2：过渡到登录界面
- 开屏元素淡出，粒子减弱为背景态
- 登录卡片逐行glitchIn入场动画

### 流程3：登录界面
- **唯一登录方式**：管理员邮箱 + 密码
- **验证凭据**：邮箱=`admin`，密码=`1`
- **交互**：输入框发光边框聚焦，按钮点击后"验证中..."状态，错误时红色提示
- **视觉风格**：Cyberpunk主题，切角clip-path，JetBrains Mono/Orbitron字体，#00ff88电光绿主色

### 流程4：登录成功过渡
- 登录框scale(1.05)淡出
- Canvas层绘制3层绿色冲击波扩散（1.5秒）
- 主界面淡入

### 流程5：主界面
- 欢迎文字 + NEXUS大标题 + "控制台已就绪·系统正常"
- 2×2功能卡片网格：数据面板、任务中心、系统监控、配置管理
- 卡片逐个上滑入场，hover发光边框

---

## 三、文件结构

```
E:\NexusApp\
├── app\
│   ├── build.gradle.kts          # 应用模块Gradle配置
│   ├── proguard-rules.pro        # 混淆规则
│   └── src\main\
│       ├── AndroidManifest.xml   # 清单（Splash→Main）
│       ├── java\com\nexus\app\
│       │   └── ui\
│       │       ├── splash\
│       │       │   ├── CgSplashView.kt   # 核心自绘动画View（580行）
│       │       │   └── SplashActivity.kt # 开屏Activity
│       │       └── main\
│       │           └── MainActivity.kt   # 主界面Activity
│       └── res\
│           ├── layout\activity_main.xml
│           ├── values\{colors,strings,themes}.xml
│           ├── drawable\ic_launcher_*.xml
│           └── mipmap-*/ic_launcher.xml
├── build.gradle.kts              # 根Gradle配置
├── settings.gradle.kts           # 模块声明
├── gradle.properties             # Gradle属性
├── gradle\wrapper\               # Wrapper配置
├── .github\workflows\build.yml   # GitHub Actions CI
├── splash-prototype.html         # ★完整HTML原型（含全流程）
└── .gitignore
```

---

## 四、GitHub仓库

- **地址**：https://github.com/lian1234244/Nexus
- **CI**：push到main分支自动触发构建
- **构建产物**：Actions → Artifacts → `nexus-debug-apk` / `nexus-release-apk`
- **当前CI方案**：`gradle/actions/setup-gradle@v4` + `gradle-version: '8.5'`（绕过wrapper jar）

---

## 五、HTML原型 vs Android现状

| 模块 | HTML原型 | Android Kotlin | 状态 |
|------|----------|---------------|------|
| 开屏动画 | ✅ 完整 | ✅ 已移植（粒子优先版） | 需同步HTML最新版 |
| 登录界面 | ✅ 完整 | ❌ 未实现 | 需新建LoginActivity |
| 登录逻辑 | ✅ admin/1验证 | ❌ 未实现 | 需实现 |
| 登录过渡动画 | ✅ 冲击波扩散 | ❌ 未实现 | 需实现 |
| 主界面 | ✅ 卡片网格 | ✅ 占位 | 需替换为卡片布局 |
| 动态SVG Logo | ✅ 登录页Logo | ❌ 未实现 | 需用AnimatedVectorDrawable |

---

## 六、后续工作清单

### 优先级P0（必做）
1. **新建LoginActivity** - 参照HTML原型的Cyberpunk风格，实现邮箱+密码登录
2. **登录验证逻辑** - admin/1校验，错误提示
3. **主界面布局替换** - 用Material3卡片替换占位TextView
4. **同步CgSplashView** - 将HTML原型最新的粒子参数同步到Kotlin版

### 优先级P1（建议）
5. **登录过渡动画** - 冲击波扩散效果
6. **动态Logo** - 用AnimatedVectorDrawable实现旋转环+轨道粒子
7. **主题统一** - 确保所有页面使用统一的Cyberpunk色彩体系（#00ff88/#ff00ff/#00d4ff）
8. **字体** - 引入Orbitron/JetBrains Mono字体资源

### 优先级P2（扩展）
9. **功能卡片落地** - 数据面板、任务中心、系统监控、配置管理
10. **导航框架** - Navigation Component + 底部导航栏
11. **数据层** - Room数据库 + Repository模式
12. **网络层** - Retrofit + OkHttp

---

## 七、关键技术决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| 渲染方式 | SurfaceView自绘 | 60fps粒子系统，避免View层级开销 |
| 粒子渲染 | RadialGradient软粒子 | 比硬圆真实，边缘自然衰减 |
| 光效混合 | PorterDuff.Mode.ADD | 发光叠加物理正确 |
| CI构建 | gradle action直接调用 | 绕过wrapper jar缺失问题 |
| 动画编排 | 时间轴phase映射 | 多阶段重叠，电影叙事感 |
| 原型验证 | HTML Canvas先行 | 快速迭代，确认效果再移植 |

---

## 八、已知问题

1. **Gradle Wrapper Jar缺失** - CI用gradle action绕过，本地开发需执行`gradle wrapper`生成
2. **CgSplashView与HTML原型不同步** - Kotlin版是早期粒子优先版，未同步最新的登录/主界面流程
3. **内存优化待做** - 大量粒子在低端设备可能GC压力，需考虑对象池
4. **横屏适配未处理** - 当前仅竖屏优化

---

> 交接人：CodeArts AI Agent
> 接收人：后续开发团队
> 文档版本：v1.0

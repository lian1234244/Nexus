# NexusApp 开发总结交接文档

> 编写日期：2026-05-13
> 项目状态：Android Kotlin核心流程已完成（开屏→登录→主界面），CI构建待最终验证

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

**后期特效**：核心多层光晕、Film Grain胶片颗粒、暗角（已移除体积光线/镜头光晕，用户认为不逼真）

### 流程2：过渡到登录界面
- 开屏元素淡出，粒子减弱为背景态
- 登录卡片逐行glitchIn入场动画

### 流程3：登录界面
- **唯一登录方式**：管理员邮箱 + 密码
- **验证凭据**：邮箱=`admin`，密码=`1`
- **交互**：输入框发光边框聚焦，按钮点击后"验证中..."状态，错误时红色提示+抖动
- **动态Logo**：AnimatedVectorDrawable旋转双环+六边形+核心脉冲
- **视觉风格**：Cyberpunk主题，切角clip-path，#00ff88电光绿主色

### 流程4：登录成功过渡
- 登录框scale(1.05)淡出动画
- 自动跳转到主界面

### 流程5：主界面
- 欢迎文字 + NEXUS大标题 + "控制台已就绪·系统正常"
- 2×2功能卡片网格：数据面板、任务中心、系统监控、配置管理
- 卡片逐个上滑入场动画

---

## 三、文件结构

```
E:\NexusApp\
├── app\
│   ├── build.gradle.kts          # 应用模块Gradle配置
│   ├── proguard-rules.pro        # 混淆规则
│   └── src\main\
│       ├── AndroidManifest.xml   # 清单（Splash→Login→Main）
│       ├── java\com\nexus\app\
│       │   └── ui\
│       │       ├── splash\
│       │       │   ├── CgSplashView.kt   # 核心自绘动画View
│       │       │   └── SplashActivity.kt # 开屏Activity
│       │       ├── login\
│       │       │   └── LoginActivity.kt  # 登录Activity（admin/1验证）
│       │       └── main\
│       │           └── MainActivity.kt   # 主界面Activity（卡片网格）
│       └── res\
│           ├── layout\
│           │   ├── activity_login.xml    # 登录布局
│           │   └── activity_main.xml     # 主界面卡片布局
│           ├── anim\
│           │   ├── avd_rotate_cw.xml     # AVD顺时针旋转
│           │   ├── avd_rotate_ccw.xml    # AVD逆时针旋转
│           │   ├── avd_rotate_ccw_slow.xml
│           │   ├── avd_rotate_cw_slow.xml
│           │   ├── avd_pulse.xml         # AVD缩放脉冲
│           │   └── avd_pulse_y.xml       # AVD Y轴缩放
│           ├── drawable\
│           │   ├── ic_nexus_logo.xml             # 静态Logo（旧版）
│           │   ├── ic_nexus_logo_static.xml      # AVD底图
│           │   ├── ic_nexus_logo_animated.xml    # 动态Logo（AVD）
│           │   └── ic_launcher_*.xml             # 启动器图标
│           ├── values\{colors,strings,themes}.xml
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
| 开屏动画 | ✅ 完整 | ✅ 已移植 | ✅ 已同步中文文字、移除光线 |
| 登录界面 | ✅ 完整 | ✅ 已实现 | ✅ Cyberpunk风格+动态Logo |
| 登录逻辑 | ✅ admin/1验证 | ✅ 已实现 | ✅ 含抖动错误反馈 |
| 登录过渡动画 | ✅ 冲击波扩散 | ✅ scale淡出 | 简化版（无Canvas冲击波） |
| 主界面 | ✅ 卡片网格 | ✅ 卡片网格 | ✅ 含入场动画 |
| 动态SVG Logo | ✅ 旋转环 | ✅ AVD实现 | ✅ 旋转+脉冲 |

---

## 六、后续工作清单

### 优先级P1（建议）
1. **Canvas冲击波过渡** - 登录成功后在Canvas层绘制3层绿色冲击波扩散（当前仅scale淡出）
2. **主题统一** - 确保所有页面使用统一的Cyberpunk色彩体系（#00ff88/#ff00ff/#00d4ff）
3. **字体** - 引入Orbitron/JetBrains Mono字体资源

### 优先级P2（扩展）
4. **功能卡片落地** - 数据面板、任务中心、系统监控、配置管理
5. **导航框架** - Navigation Component + 底部导航栏
6. **数据层** - Room数据库 + Repository模式
7. **网络层** - Retrofit + OkHttp

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
| 体积光线 | 移除drawFlare | 用户认为不逼真，偏好粒子为主的效果 |

---

## 八、踩坑注意事项（⚠️必读）

> 以下是开发过程中实际踩过的坑，务必避免重复犯错。

### 1. ⚠️⚠️⚠️ `RadialGradient` API 34 构造函数导致低版本手机闪退（最致命！）
- **错误**：`compileSdk=34` 但 `minSdk=24`，直接使用 `RadialGradient(x, y, r, longArrayOf(...), floatArrayOf(...), tileMode)` — 这个构造函数是 **API 34 新增**，在 API 33 及以下设备上运行时直接 `NoSuchMethodError` 闪退
- **后果**：APP 一启动（开屏动画 `CgSplashView.drawBg` 就调用 RadialGradient）立即崩溃，任何手机只要不是 Android 14+ 都 100% 闪退
- **正确**：必须做 API 级别判断：
  ```kotlin
  private fun radialGrad(cx: Float, cy: Float, r: Float, colors: IntArray, stops: FloatArray, tile: Shader.TileMode): RadialGradient {
      return if (Build.VERSION.SDK_INT >= 34) {
          RadialGradient(cx, cy, r, colors.map { it.toLong() }.toLongArray(), stops, tile)
      } else {
          @Suppress("DEPRECATION")
          RadialGradient(cx, cy, r, colors, stops, tile)
      }
  }
  ```
- **注意**：所有 `RadialGradient`/`LinearGradient`/`SweepGradient` 的颜色参数在 API 34+ 从 `Int`/`IntArray` 变为 `Long`/`LongArray`。**任何直接使用新签名的地方都必须加 API 守卫，否则低版本闪退！**

### 2. PowerShell 不支持 `&&` 语句分隔符
- **错误**：`cd E:/NexusApp && git status` → 解析错误
- **正确**：PowerShell中使用 `;` 连接命令：`cd E:/NexusApp; git status`
- **注意**：Git Bash环境可用 `&&`，PowerShell不可

### 2. PowerShell 中 `git commit -m` 含中文/空格会解析失败
- **错误**：`git commit -m "feat: 添加登录界面"` → 中文字符被PowerShell误解析
- **正确**：使用简短英文commit message：`git commit -m "feat-add-login-ui"`
- **替代**：Git Bash中HEREDOC语法 `$(cat <<'EOF'...EOF)` 在PowerShell也不可用

### 3. SurfaceView 中 `density` 属性不存在
- **错误**：`context.density` 编译失败
- **正确**：`context.resources.displayMetrics.density`

### 4. Kotlin 中 `sin(Long * Float)` 类型陷阱
- **错误**：`sin(time * 0.003)` 其中 `time` 是 `Long`，`0.003` 是 `Double`，结果为 `Double`，但 `sin()` 在不同上下文返回不同类型
- **正确**：显式 `.toFloat()`：`sin(time * 0.003f).toFloat()`

### 5. Gradle Wrapper Jar 缺失导致 CI 失败
- **错误**：直接运行 `./gradlew` 报 `ClassNotFoundException: GradleWrapperMain`
- **正确**：CI中使用 `gradle/actions/setup-gradle@v4` + `gradle-version: '8.5'` 直接调用 gradle，不依赖 wrapper jar
- **注意**：本地开发需先执行 `gradle wrapper` 生成 jar

### 6. AndroidManifest 引用的 `mipmap/ic_launcher` 必须提供资源文件
- **错误**：清单引用了 `@mipmap/ic_launcher` 但无对应资源 → `processDebugResources` 失败
- **正确**：在 `res/mipmap-*/` 下提供 `ic_launcher.xml`（adaptive icon）

### 7. VectorDrawable 不支持 `android:strokeDashArray` 属性
- **错误**：在 `<path>` 上使用 `android:strokeDashArray="8 12"` → AAPT报错 `attribute android:strokeDashArray not found`
- **正确**：VectorDrawable 的 `<path>` **不支持** `strokeDashArray`，只能用实线。虚线效果需用Canvas代码绘制或拆分为多段短path
- **注意**：SVG/HTML的 `stroke-dasharray` 在Android VectorDrawable中没有等价属性

### 8. VectorDrawable 的 `<path>` 必须有 `android:fillColor`
- **错误**：仅描边的path省略 `fillColor` → AAPT报错
- **正确**：所有 `<path>` 必须显式声明 `android:fillColor="#00000000"`（透明填充）表示不填充

### 9. `<group>` 标签不能设置 `strokeColor`/`strokeWidth` 等path属性
- **错误**：`<group android:strokeColor="#00FF88">` → AAPT报错
- **正确**：`<group>` 只支持 `name`、`translateX/Y`、`scaleX/Y`、`rotation`、`pivotX/Y`。描边/填充属性必须设在 `<path>` 上

### 10. AnimatedVectorDrawable 的 `<target>` 不支持 `<set>` 动画集
- **错误**：`<set>` 内含多个 `<objectAnimator>` 用于同一 target → 运行时崩溃或无效
- **正确**：每个 `<target>` 只能引用一个 `<objectAnimator>`。如需同时动画scaleX和scaleY，需拆分为两个 `<target>` 指向不同动画文件

### 12. API 34+ 的 `RadialGradient` 颜色参数类型变更（编译时类型不匹配）
- **错误**：`RadialGradient(x, y, r, intColor1, intColor2, tileMode)` → 编译时类型不匹配，API 34+ 期望 `Long`/`LongArray`
- **正确**：不要直接用新签名（会导致低版本闪退，见第1条），统一用兼容包装方法 `radialGrad()`/`radialGrad2()`，内部根据 `Build.VERSION.SDK_INT` 分发
- **关键**：不要用两色构造函数传 `.toLong()`，Kotlin会将其匹配到多色构造函数（签名歧义），导致"Long但期望LongArray"编译错误

### 13. Kotlin `surfaceChanged` 参数名不能与父方法重复
- **错误**：`override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, h: Int)` → 参数 `h` 冲突
- **正确**：`override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int)`

### 14. Kotlin 链式 `withEndAction` 的花括号缩进可能导致解析错误
- **错误**：
  ```kotlin
  view.animate()
      .translationX(10f).setDuration(50).withEndAction {
      // Kotlin可能把后续代码解析为链式调用而非lambda内容
      rootView.animate()...
      }
  ```
- **正确**：将 `withEndAction {` 和 lambda 内容写在同一行或确保花括号紧随方法：
  ```kotlin
  rootView.animate().translationX(10f).setDuration(50).withEndAction {
      rootView.animate().translationX(-10f).setDuration(50).withEndAction {
          // ...
      }
  }
  ```

### 15. `onBackPressed()` 空实现不能用 `{}` 且不能递归调用自身
- **错误**：`override fun onBackPressed() { onBackPressed() }` → 无限递归StackOverflow
- **正确**：`override fun onBackPressed() { /* no-op */ }`

---

## 九、已知问题

1. **Gradle Wrapper Jar缺失** - CI用gradle action绕过，本地开发需执行`gradle wrapper`生成
2. **内存优化待做** - 大量粒子在低端设备可能GC压力，需考虑对象池
3. **横屏适配未处理** - 当前仅竖屏优化
4. **CI构建待最终验证** - 最新修复已推送，等待CI确认通过

---

> 交接人：CodeArts AI Agent
> 接收人：后续开发团队
> 文档版本：v2.0

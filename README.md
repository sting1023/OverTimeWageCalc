# OverTimeWageCalc 加班工资计算器

安卓 app:计算本月加班工资总额,支持时薪/日薪两种模式。

## 功能

- 📅 **日历视图**:可视化当月,左右切换月份
- 💰 **两种模式**:
  - 时薪模式:时薪 × 工作小时数
  - 日薪模式:固定日薪(适合周末/假日)
- ➕ **额外加班**:任意模式下都可以加额外加班金额 + 备注
- 📊 **本月明细**:每天的工资构成 + 加班费 + 总额
- ⚙️ **设置默认**:左上角设置按钮,设默认时薪和默认日薪

## 编译

GitHub Actions 自动编译 debug APK,作为 artifact 上传。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- minSdk 24 / targetSdk 34

## 项目位置

源码:NAS `/AI交流文件夹/app工程/源文件/OverTimeWageCalc/`
APK:NAS `/AI交流文件夹/app工程/apk/`

## 包信息

- 包名:com.sting.overtimewagecalc
- versionCode:1
- versionName:1.0
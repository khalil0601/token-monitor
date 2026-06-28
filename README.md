# ⚡ Token Monitor — 傻瓜式部署教程

> 一个手机桌面小组件，实时显示你的 API Token 还剩多少。

---

## 📱 最终效果

部署完成后，你手机上会有一个这样的仪表盘：

```
┌──────────────────────────────┐
│  ⚡ Token Monitor      🔄   │
│  🟢 上次更新: 17:21:30       │
│                              │
│  ┌────────────────────────┐  │
│  │ 🟠 Anthropic      🟢 ● │  │
│  │                        │  │
│  │   ╭──────╮   已使用    │  │
│  │   │ 78%  │   125K      │  │
│  │   │ 剩余 │   总额度    │  │
│  │   ╰──────╯   500K      │  │
│  │             预估费用   │  │
│  │             $0.38      │  │
│  │  ████████░░░░░░░░░░░   │  │
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │ 🟢 OpenAI         🟢 ● │  │
│  │   （同上布局）          │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

---

## 🚀 第一步：部署到云端（5 分钟）

> 不需要懂代码，跟着点按钮就行。

### 方案 A：Railway 部署（推荐，最简单）

**1. 注册 Railway 账号**
- 打开 https://railway.app
- 点右上角 **Login**
- 用 **GitHub 账号** 登录（没有的话先去 github.com 注册一个）

**2. 上传代码到 GitHub**

- 打开 https://github.com/new
- Repository name 填 `token-monitor`
- 选 **Private**（私密仓库）
- 点 **Create repository**
- 然后你会看到一个上传页面，把 `token-monitor` 文件夹里的所有文件拖进去
- 点 **Commit changes**

**3. 在 Railway 部署**

- 回到 https://railway.app
- 点 **New Project** → **Deploy from GitHub**
- 授权 Railway 访问你的 GitHub
- 选择刚才创建的 `token-monitor` 仓库
- Railway 会自动检测并部署！
- 等待 2-3 分钟，看到 ✅ Success 就完成了

**4. 设置 API Key（最关键的一步！）**

- 在 Railway 项目页面，点你的服务卡片
- 点 **Variables** 标签
- 添加以下变量：

| 变量名 | 值（你的真实 API Key） |
|--------|----------------------|
| `ANTHROPIC_API_KEY` | `sk-ant-你的真实key` |
| `OPENAI_API_KEY` | `sk-你的真实key` |

- 点 **Add** 保存
- 服务会自动重启，等 10 秒就好

**5. 获取你的网址**

- 在 Railway 项目页面 → **Settings** → 找到 **Public URL**
- 类似 `https://token-monitor-xxxx.up.railway.app`
- 复制这个网址，**这就是你的 Token Monitor！**

---

### 方案 B：在你自己电脑上跑（不关机才能用）

**1. 安装 Python**

- 打开 https://www.python.org/downloads/
- 下载最新版 Python，安装时 **勾选 "Add Python to PATH"**

**2. 配置 API Key**

- 进入 `token-monitor` 文件夹
- 找到 `.env.example` 文件，复制一份改名为 `.env`
- 用记事本打开 `.env`，把 `sk-ant-xxxxxxxxxxxx` 替换成你的真实 API Key

**3. 双击 `start.bat`**（等一下会生成这个文件）
- 一个黑窗口会弹出来，显示服务器正在运行
- **不要关掉这个窗口！** 关了服务就停了

**4. 浏览器打开 http://localhost:8000**

---

## 🔑 第二步：获取 API Key

### Anthropic API Key

1. 打开 https://console.anthropic.com
2. 登录你的账号
3. 左侧菜单 → **API Keys**
4. 点 **Create Key** → 复制保存好
5. 格式类似：`sk-ant-api03-xxxxxxxxxxxx`

### OpenAI API Key

1. 打开 https://platform.openai.com/api-keys
2. 登录你的账号
3. 点 **Create new secret key**
4. 复制保存好
5. 格式类似：`sk-proj-xxxxxxxxxxxx`

> ⚠️ **重要**：这些 Key 相当于你的密码，不要发给任何人！

---

## 📲 第三步：添加到手机桌面

### Android 手机

1. 用 **Chrome 浏览器** 打开你的 Railway 网址
2. 地址栏右边会出现一个 **「⋮」菜单按钮**，点它
3. 选择 **「添加到主屏幕」**
4. 名字随便填（比如"Token"），点 **添加**
5. 桌面上就会出现 ⚡ **Token Monitor** 图标！
6. 以后就像打开普通 App 一样点它

### iPhone 手机

1. 用 **Safari 浏览器** 打开你的 Railway 网址
2. 底部中间点 **分享按钮**（方框带箭头）
3. 往下滑，选择 **「添加到主屏幕」**
4. 名字填 "Token"，点 **添加**
5. 桌面出现图标，点击即用

---

## 🎯 使用说明

| 操作 | 怎么弄 |
|------|--------|
| **查看余额** | 打开桌面图标，两个卡片的圆环显示剩余百分比 |
| **手动刷新** | 点右上角 🔄 按钮 |
| **自动刷新** | 每 60 秒自动更新，不用管 |
| **绿色=充足** | 剩余 > 25%，放心用 |
| **黄色=警告** | 剩余 10-25%，注意控制 |
| **红色=告急** | 剩余 < 10%，该充值了！ |

---

## ❓ 常见问题

**Q: 卡片显示「API key not configured」怎么办？**
> Railway 的 Variables 没设置对，回去检查 API Key 是否正确粘贴了。

**Q: 数据不准怎么办？**
> 刷新有延迟（API 提供商那边不是实时的），一般几分钟内同步。

**Q: 换个手机还能用吗？**
> 能！用浏览器打开同一个网址就行，也可以再次添加到桌面。

**Q: 能只监控一个平台吗？**
> 可以。只填一个 API Key，另一个卡片会显示「请配置 API Key」。

**Q: Railway 要钱吗？**
> 免费额度每月 $5，这个小应用基本不花钱。如果担心，可以设个用量上限。

**Q: 安全吗？**
> API Key 存在 Railway 的环境变量里，不经过第三方。而且你的 GitHub 仓库是私密的。

---

## 📋 检查清单

- [ ] GitHub 仓库已创建并上传代码
- [ ] Railway 部署成功，拿到 Public URL
- [ ] 在 Railway Variables 中填入了 `ANTHROPIC_API_KEY` 和 `OPENAI_API_KEY`
- [ ] 浏览器打开 URL 能看到两个卡片
- [ ] 手机成功添加到桌面
- [ ] 以后每天看一眼，心里有数 ✨

---

有任何问题直接把错误截图发给我。

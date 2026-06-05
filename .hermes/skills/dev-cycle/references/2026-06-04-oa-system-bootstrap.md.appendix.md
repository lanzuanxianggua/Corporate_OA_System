# 99f517b 重启后的真实状态 (2026-06-04 后续)

第 11 节表格（"4 个模块 closed、17/17 测试"）描述的是 **2824bd8 commit 阶段**的状态。
99f517b commit "删除 v1 业务代码" 把那些文件全部 `git rm` 掉了。本节描述
**99f517b 之后的真实世界**——sibling subagent 重写后**未 commit** 的工作。

## 1. reflog 真相（必读）

```
99f517b HEAD@{0}: commit: chore: 删除 v1 业务代码, 项目从 v2 设计文档重新开始
2824bd8 HEAD@{1}: checkout: moving from master to 2824bd8
ba581eb HEAD@{2}: reset: moving to ba581eb29b309d7ca8902b364bf25d252c5cb575
2824bd8 HEAD@{3}: commit: chore: 保存当前所有修改（含新模块和未跟踪文件）
ba581eb HEAD@{4}: commit: fix: oa_loan表添加repaid_amount列修复API测试
13b3f30 HEAD@{5}: merge main: Fast-forward
729e6bb HEAD@{6}: checkout: moving from main to master
13b3f30 HEAD@{7}: rebase (abort): returning to refs/heads/main
...
```

读法：
- **HEAD 当前在 99f517b**（v2 重启点）
- 99f517b 的 commit message 是"删除 v1 业务代码"，**实际把 2824bd8 commit
  里的所有 v1+v2 文件都 `git rm` 了**（包括 oa-platform-* 模块的 v2 草稿）
- **当前 `code/` 目录下的 81 个 java 文件都是 sibling subagent 在
  99f517b 之后重写的，从来没 commit 过**（`git ls-files code/` 返回空）
- **第 11 节表格的"17/17 测试"指的是 2824bd8 阶段**——那些 .class 和
  target/jacoco.exec 是前次编译产物，**新写的源码可能和它们对不上**

## 2. 当前真实模块分布（不是 §11 表格）

| 模块 | 实际 java 数 | 状态 | 备注 |
|------|------------|------|------|
| oa-platform-common | 24 | untracked, 有 target/ | §11 表格写 22 个，**实际 24** |
| oa-platform-security | 8 + 1 测试 | untracked, 有 target/ | 模块骨架已完整，**不是从零写** |
| oa-platform-web | 4 + 2 测试 | untracked | 启动模块 + PingController + CorsConfig + ApplicationSmokeTest + PingControllerIT |
| oa-system | 2 | untracked | 只有 OaSystemModuleConfig + AuthController |
| oa-workflow | 0 | 空目录 | **sibling 写 WfDefinitionService 写到一半崩了**（patch partial-view 警告的受害者） |
| 其他 13 个业务模块 | 0 | 空目录 | oa-hr-leave / oa-finance / oa-document / ... |
| **合计** | **81 java** | **全部 untracked** | |

注意 **oa-workflow 是 0 个 java 文件**——之前 session_search summary
提的"在 oa-workflow/WfDefinitionService 写崩"= **该文件根本不存在**，
sibling 写完被 reset 删了。这是判断"上轮实际进度"的关键。

## 3. dev-cycle 的"4 个模块 closed 17/17"是错的

第 11 节描述的是 **2824bd8 阶段**的快照。99f517b 之后 sibling 重新写
的那批文件**质量未审阅**——可能：
- 复制了 2824bd8 阶段的代码 + 改了一两行（**这是最可能的，因为 17/17
  测试的 target/ 还存在**）
- 或者完全重写（概率小，因为时间不够）

**审计准则**：下次 session 第一次接活时，**先 `diff` 一下 oa-platform-*
当前的 untracked 文件 vs 2824bd8 commit 里的同路径文件**，确认
sibling 是不是只复制 + 改了一两行。

```bash
cd E:/JavaProject/Corporate_OA_System
# 2824bd8 commit 里的同路径文件
git show 2824bd8:code/backend/oa-platform-security/src/main/java/cn/oa/platform/security/jwt/JwtUtil.java > /tmp/jwt-2824bd8.java
# 当前 untracked 的同路径
diff -u /tmp/jwt-2824bd8.java code/backend/oa-platform-security/src/main/java/cn/oa/platform/security/jwt/JwtUtil.java
```

## 4. 20+ 孤儿 worktree 是 sibling 痕迹

`.claude/worktrees/wf_*` 下 20+ 个 worktree：
- 大部分是 orphan（sibling session 关闭但 worktree 没 prune）
- 少数 `locked` 的（pid 4976 等）**先 tasklist 确认进程在不在**：
  ```bash
  tasklist | grep -E "java|python" | head -10
  ```
  pid 在 = 真实活跃，不清；pid 不在 = 假 locked，可强清：
  ```bash
  git worktree remove --force .claude/worktrees/<name>
  git worktree prune
  ```

**清理前先确认每个 worktree 的 HEAD**（`git worktree list --porcelain`），
避免误删了还在做实际工作的 session。

## 5. 实战恢复顺序（本会话已验证）

1. `git reflog` 解出 reflog 真相（HEAD 实际轨迹）
2. `git status --short` 看 untracked 范围
3. `git ls-files code/` 验证是否真 untracked
4. `find code/backend -name "*.java" | wc -l` 数实际 java 文件
5. `git worktree list --porcelain` 列孤儿 + 区分 locked/active/orphan
6. **汇报给用户**：模块骨架已存在 / 不存在 / 状态如何，让用户重选下一步
7. **不直接动代码**——audit 阶段还在等用户拍板

## 6. 教训：session_search 的 summary 是 stale 的典型模式

- summary 提的"2824bd8 commit 阶段"实际是 1 天前的快照
- summary 提的"HR 试点 T1-T3 完成"实际是 2824bd8 阶段的，99f517b 之后
  sibling 可能压根没做 HR 模块（oa-hr-leave 目录是空的）
- summary 提的"oa-platform-security 待写"实际是 sibling 已经写完了 8 个文件
  + 1 个测试 + jar 都打了

**结论**：**session_search 只用于回忆对话内容，不用于判断项目状态**。
项目状态唯一信源是 `git reflog + git status + git ls-files + find`。

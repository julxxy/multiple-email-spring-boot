#!/usr/bin/env bash
# 超级安全版本：只清理提交记录，绝不删除分支和代码

set -e

echo "=== 安全清理所有分支的提交记录 ==="
echo "✅ 保留所有分支"
echo "✅ 保留所有代码"
echo "✅ 只清理提交历史"
echo ""

# 1. 正确获取分支列表（避免把文件当分支）
echo "正在获取分支列表..."
branches=($(git branch --format='%(refname:short)' 2>/dev/null))

if [ ${#branches[@]} -eq 0 ]; then
  echo "❌ 未发现任何分支"
  exit 1
fi

echo "发现的本地分支："
for i in "${!branches[@]}"; do
  commits=$(git rev-list --count "${branches[i]}" 2>/dev/null || echo "0")
  files=$(git ls-tree -r --name-only "${branches[i]}" 2>/dev/null | wc -l || echo "0")
  echo "  $((i+1)). ${branches[i]} (${commits}个提交, ${files}个文件)"
done

echo ""
read -p "确认以上分支列表正确，开始清理历史？(y/N): " confirm
if [[ $confirm != "y" && $confirm != "Y" ]]; then
  echo "操作已取消"
  exit 0
fi

# 2. 记录当前分支
current_branch=$(git branch --show-current 2>/dev/null || echo "")
echo "当前分支: ${current_branch:-"未知"}"

# 3. 为每个分支清理历史
echo ""
echo "开始处理各分支..."

for branch in "${branches[@]}"; do
  echo ""
  echo ">>> 正在处理分支: $branch"

  # 验证分支存在
  if ! git show-ref --verify --quiet "refs/heads/$branch"; then
    echo "⚠️  分支 $branch 不存在，跳过"
    continue
  fi

  # 切换到目标分支
  echo "  切换到分支: $branch"
  git checkout "$branch" >/dev/null 2>&1

  # 显示当前状态
  commit_count=$(git rev-list --count HEAD 2>/dev/null)
  file_count=$(git ls-files | wc -l 2>/dev/null)
  echo "  当前状态: $commit_count个提交, $file_count个文件"

  # 创建临时的孤立分支（唯一命名避免冲突）
  temp_branch="temp-clean-$branch-$(date +%s)-$$"
  echo "  创建临时分支: $temp_branch"
  git checkout --orphan "$temp_branch" >/dev/null 2>&1

  # 确保所有文件都被添加（保留代码）
  echo "  保留所有代码文件..."
  git add -A

  # 检查暂存区状态
  staged_files=$(git diff --cached --name-only | wc -l)
  echo "  暂存文件数: $staged_files"

  # 创建新的初始提交
  commit_msg="Reset history for branch '$branch' - $(date '+%Y-%m-%d %H:%M:%S')"
  git commit -m "$commit_msg" >/dev/null 2>&1

  # 删除原分支（只删除分支引用，不删除代码）
  git branch -D "$branch" >/dev/null 2>&1

  # 将临时分支重命名为原分支名
  git branch -m "$branch" >/dev/null 2>&1

  # 验证结果
  new_commit_count=$(git rev-list --count HEAD 2>/dev/null)
  new_file_count=$(git ls-files | wc -l 2>/dev/null)
  echo "  ✅ $branch 完成: $new_commit_count个提交, $new_file_count个文件"
done

# 4. 恢复到原分支
if [[ -n "$current_branch" ]]; then
  echo ""
  echo "恢复到原分支: $current_branch"
  git checkout "$current_branch" >/dev/null 2>&1
fi

# 5. 显示最终结果
echo ""
echo "🎉 清理完成！最终状态："
echo ""
printf "%-20s %-10s %-10s\n" "分支名" "提交数" "文件数"
echo "----------------------------------------"
for branch in "${branches[@]}"; do
  commits=$(git rev-list --count "$branch" 2>/dev/null || echo "0")
  files=$(git ls-tree -r --name-only "$branch" 2>/dev/null | wc -l || echo "0")
  printf "%-20s %-10s %-10s\n" "$branch" "$commits" "$files"
done

echo ""
echo "✅ 所有分支已保留"
echo "✅ 所有代码已保留"
echo "✅ 提交历史已清理"
echo ""
echo "如需推送到远程，使用:"
echo "git push --force --all origin"

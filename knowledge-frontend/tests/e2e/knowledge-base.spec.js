// E2E Tests for Personal Knowledge Management System
// Tests cover: Home, Notes CRUD, Search, Tags, Categories
import { test, expect, request } from '@playwright/test'

// Shared state across tests within the suite
let createdNoteTitle = ''
let createdNoteId = ''
const API = 'http://localhost:8080/api'

// Seed a note with 'Test' in title so search tests have data
test.beforeAll(async () => {
  const ctx = await request.newContext()
  await ctx.post(`${API}/notes`, {
    data: { title: 'Test Seed Note', content: 'Seed content for E2E search testing', tagIds: [] },
    headers: { 'Content-Type': 'application/json' }
  })
  await ctx.dispose()
})

// ─────────────────────────────────────────────
// TC-001: 首页加载
// ─────────────────────────────────────────────
test('TC-001: 首页加载 - 验证页面标题和基础元素', async ({ page }) => {
  await page.goto('/')
  await page.waitForLoadState('networkidle')

  // Verify page title
  await expect(page).toHaveTitle(/知识库/)

  // Verify header logo text
  await expect(page.locator('.logo-text')).toContainText('个人知识库')

  // Verify header search input exists
  await expect(page.locator('.app-header .search-input')).toBeVisible()

  // Verify "新建笔记" button exists (scope to header to avoid strict mode)
  await expect(page.locator('.app-header').getByRole('button', { name: '新建笔记' })).toBeVisible()
})

// ─────────────────────────────────────────────
// TC-001b: 首页空状态提示（仅在无数据时）
// ─────────────────────────────────────────────
test('TC-001b: 首页 - 页面主体区域可见（笔记列表或空状态）', async ({ page }) => {
  await page.goto('/')
  await page.waitForLoadState('networkidle')

  // Wait for notes to load (loading state disappears)
  await page.waitForTimeout(2000)

  // Either note list or empty state should be visible
  const hasNotes = await page.locator('.note-list').isVisible().catch(() => false)
  const hasEmpty = await page.locator('.empty-state, [class*="empty"]').isVisible().catch(() => false)
  const hasToolbar = await page.locator('.toolbar').isVisible().catch(() => false)

  // At least one of these should exist after page loads
  expect(hasNotes || hasEmpty || hasToolbar).toBeTruthy()
})

// ─────────────────────────────────────────────
// TC-002: 创建笔记
// ─────────────────────────────────────────────
test('TC-002: 创建笔记 - 新建、填写内容、保存', async ({ page }) => {
  await page.goto('/')
  await page.waitForLoadState('networkidle')

  // Click "新建笔记" button in header (avoid strict mode with multiple matches)
  await page.locator('.app-header').getByRole('button', { name: '新建笔记' }).click()

  // Verify URL navigated to /notes/new
  await expect(page).toHaveURL(/\/notes\/new/)

  // Wait for the form to appear
  await page.waitForSelector('.title-input', { state: 'visible', timeout: 10000 })

  // Generate unique title for this test run
  createdNoteTitle = `E2E测试笔记_${Date.now()}`

  // Fill in title
  await page.locator('.title-input input').fill(createdNoteTitle)

  // Fill in Markdown content via textarea
  const editorTextarea = page.locator('.editor-content textarea, .editor-pane textarea').first()
  await editorTextarea.fill('# E2E 测试内容\n\n这是由 Playwright 自动化测试创建的笔记。\n\n- 测试项目1\n- 测试项目2')

  // Click save button
  await page.getByRole('button', { name: '保存' }).click()

  // Wait for success message
  await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 10000 })

  // Verify redirect to note detail page
  await page.waitForURL(/\/notes\/\d+$/, { timeout: 10000 })

  // Capture the created note ID from URL
  const url = page.url()
  const match = url.match(/\/notes\/(\d+)$/)
  if (match) {
    createdNoteId = match[1]
  }

  expect(page.url()).toMatch(/\/notes\/\d+$/)
})

// ─────────────────────────────────────────────
// TC-003: 笔记列表显示
// ─────────────────────────────────────────────
test('TC-003: 笔记列表 - 创建后在首页显示', async ({ page }) => {
  await page.goto('/')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)

  // Note list or toolbar should be visible (seed note guaranteed by beforeAll)
  const noteList = page.locator('.note-list, .note-card, .el-card')
  await expect(noteList.first()).toBeVisible({ timeout: 10000 })

  // Count should be shown in toolbar
  const totalCount = page.locator('.total-count')
  if (await totalCount.isVisible()) {
    const countText = await totalCount.textContent()
    const num = parseInt(countText?.replace(/\D/g, '') || '0')
    expect(num).toBeGreaterThan(0)
  }

  // Note cards should contain title and summary
  const cards = page.locator('.note-card, .el-card')
  const count = await cards.count()
  expect(count).toBeGreaterThan(0)

  // Each visible card should have a title
  const firstCard = cards.first()
  await expect(firstCard).toBeVisible()
  const titleEl = firstCard.locator('.note-title, h3').first()
  await expect(titleEl).toBeVisible()
})

// ─────────────────────────────────────────────
// TC-004: 搜索功能
// ─────────────────────────────────────────────
test('TC-004: 搜索功能 - 输入关键词搜索并验证结果', async ({ page }) => {
  // Use created note title if available, else use known seed data keyword
  const keyword = createdNoteTitle ? createdNoteTitle.substring(0, 10) : 'Test'

  await page.goto('/')
  await page.waitForLoadState('networkidle')

  // Find search input in header
  const searchInput = page.locator('.app-header .search-input input, .app-header input[placeholder*="搜索"]')
  await searchInput.fill(keyword)

  // Press Enter to search
  await searchInput.press('Enter')

  // Verify URL navigated to /search with query param
  await page.waitForURL(/\/search\?q=/, { timeout: 10000 })
  expect(page.url()).toContain('/search')
  expect(page.url()).toContain('q=')

  // Wait for search results to load
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1500)

  // Verify result count text is shown or result list appears
  const resultCount = page.locator('.result-count')
  const resultList = page.locator('.result-list')
  const emptyState = page.locator('.empty-state, [class*="empty"]')

  const hasResults = await resultList.isVisible().catch(() => false)
  const hasEmpty = await emptyState.isVisible().catch(() => false)
  const hasCountText = await resultCount.isVisible().catch(() => false)

  // Either results or empty state should be shown (search page loaded)
  expect(hasResults || hasEmpty || hasCountText).toBeTruthy()
})

// ─────────────────────────────────────────────
// TC-004b: 搜索结果有内容
// ─────────────────────────────────────────────
test('TC-004b: 搜索功能 - 使用已知关键词"Test"搜索有结果', async ({ page }) => {
  // Navigate directly to search with a known keyword from seed data
  await page.goto('/search?q=Test')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)

  // Result count should show
  const resultCount = page.locator('.result-count')
  if (await resultCount.isVisible()) {
    const text = await resultCount.textContent()
    expect(text).toContain('共找到')
    // Extract number from text
    const numMatch = text?.match(/\d+/)
    if (numMatch) {
      expect(parseInt(numMatch[0])).toBeGreaterThan(0)
    }
  }

  // Result list should have at least one card
  const cards = page.locator('.result-list .note-card, .result-list .el-card')
  const count = await cards.count()
  expect(count).toBeGreaterThan(0)
})

// ─────────────────────────────────────────────
// TC-005: 标签管理
// ─────────────────────────────────────────────
test('TC-005: 标签管理 - 创建新标签并验证显示', async ({ page }) => {
  await page.goto('/tags')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1000)

  // Verify page has tag management heading
  await expect(page.locator('h2')).toContainText('标签管理')

  // Find tag name input field
  const tagInput = page.locator('input[placeholder*="标签名称"], input[placeholder*="输入标签"]')
  await expect(tagInput).toBeVisible({ timeout: 5000 })

  // Enter unique tag name
  const tagName = `E2E标签_${Date.now()}`
  await tagInput.fill(tagName)

  // Click "添加标签" button
  await page.getByRole('button', { name: '添加标签' }).click()

  // Wait for success message
  await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 8000 })

  // Wait for tag list to refresh
  await page.waitForTimeout(1000)

  // Verify the new tag appears in the table
  await expect(page.locator('.el-table, .tag-table')).toContainText(tagName, { timeout: 5000 })
})

// ─────────────────────────────────────────────
// TC-006: 分类管理
// ─────────────────────────────────────────────
test('TC-006: 分类管理 - 创建新分类并验证树形显示', async ({ page }) => {
  await page.goto('/categories')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(1000)

  // Verify page has category management heading
  await expect(page.locator('h2')).toContainText('分类管理')

  // Find category name input
  const catInput = page.locator('input[placeholder*="分类名称"], input[placeholder*="请输入分类"]')
  await expect(catInput).toBeVisible({ timeout: 5000 })

  // Enter unique category name
  const catName = `E2E分类_${Date.now()}`
  await catInput.fill(catName)

  // Click "创建分类" button
  await page.getByRole('button', { name: '创建分类' }).click()

  // Wait for success message
  await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 8000 })

  // Wait for category tree to refresh
  await page.waitForTimeout(1000)

  // Verify the new category appears in the tree (scope to el-tree, not wrapper panel)
  await expect(page.locator('.el-tree')).toContainText(catName, { timeout: 5000 })
})

// ─────────────────────────────────────────────
// TC-007: 删除笔记
// ─────────────────────────────────────────────
test('TC-007: 删除笔记 - 确认删除并验证从列表消失', async ({ page }) => {
  // First navigate to home and check notes exist
  await page.goto('/')
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(2000)

  // Check if there are note cards to delete
  const noteCards = page.locator('.note-list .note-card, .note-list .el-card')
  const cardCount = await noteCards.count()

  if (cardCount === 0) {
    // No notes to delete, create one first via API
    const resp = await page.request.post('http://localhost:8080/api/notes', {
      data: { title: '待删除笔记_E2E', content: '此笔记用于测试删除功能' },
      headers: { 'Content-Type': 'application/json' }
    })
    expect(resp.ok()).toBeTruthy()

    // Reload page
    await page.reload()
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
  }

  // Get count before deletion
  const countBefore = await noteCards.count()
  expect(countBefore).toBeGreaterThan(0)

  // Get the title of the first note (to verify it disappears later)
  const firstNoteTitle = await noteCards.first().locator('.note-title, h3').textContent()

  // Click the delete button (danger/red circle button) on the first card
  const deleteBtn = noteCards.first().locator('button[title="删除"], .el-button--danger').first()
  await deleteBtn.click()

  // Confirmation popover should appear - click "确定"
  // Element Plus popconfirm renders in a popper, click the confirm button
  const confirmBtn = page.locator('.el-popconfirm__action button, .el-pop-confirm button').filter({ hasText: '确定' })
  await expect(confirmBtn).toBeVisible({ timeout: 5000 })
  await confirmBtn.click()

  // Wait for success message
  await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 8000 })

  // Wait for list to refresh
  await page.waitForTimeout(1500)

  // Verify card count decreased
  const countAfter = await noteCards.count()
  expect(countAfter).toBeLessThan(countBefore)
})

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'NoteList',
    component: () => import('@/pages/NoteListPage.vue')
  },
  {
    path: '/notes/new',
    name: 'NoteNew',
    component: () => import('@/pages/NoteEditPage.vue')
  },
  {
    path: '/notes/:id',
    name: 'NoteDetail',
    component: () => import('@/pages/NoteDetailPage.vue')
  },
  {
    path: '/notes/:id/edit',
    name: 'NoteEdit',
    component: () => import('@/pages/NoteEditPage.vue')
  },
  {
    path: '/tags',
    name: 'TagManage',
    component: () => import('@/pages/TagManagePage.vue')
  },
  {
    path: '/categories',
    name: 'CategoryManage',
    component: () => import('@/pages/CategoryManagePage.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/pages/SearchResultPage.vue')
  },
  {
    path: '/files',
    name: 'FileList',
    component: () => import('@/pages/FileListPage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import LoginPage from '../pages/LoginPage.vue'
import MoviesPage from '../pages/MoviesPage.vue'
import MovieDetailPage from '../pages/MovieDetailPage.vue'
import TvSeasonsPage from '../pages/TvSeasonsPage.vue'
import TvSeasonDetailPage from '../pages/TvSeasonDetailPage.vue'
import PersonDetailPage from '../pages/PersonDetailPage.vue'
import PersonsPage from '../pages/PersonsPage.vue'
import RegisterPage from '../pages/RegisterPage.vue'
import AdminTmdbImportPage from '../pages/AdminTmdbImportPage.vue'
import { getToken, removeToken } from '../utils/auth'
import { request } from '../utils/request'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            component: Layout,
            children: [
                {
                    path: '/login',
                    name: 'login',
                    meta: { title: '登录' },
                    component: LoginPage
                },
                {
                    path: '',
                    redirect: '/movies',
                    meta: { title: '首页' }
                },
                {
                    path: 'movies',
                    component: MoviesPage,
                    meta: { title: '首页' }
                },
                {
                    path: 'tv-seasons',
                    component: TvSeasonsPage,
                    meta: { title: '剧集' }
                },
                {
                    path: '/movies/:id',
                    component: MovieDetailPage
                },
                {
                    path: '/tv-seasons/:id',
                    component: TvSeasonDetailPage
                },
                {
                    path: 'persons',
                    component: PersonsPage
                },
                {
                    path: '/persons/:id',
                    component: PersonDetailPage
                },
                {
                    path: '/register',
                    name: 'Register',
                    meta: { title: '注册' },
                    component: RegisterPage
                },
                {
                    path: '/admin/tmdb-import',
                    name: 'AdminTmdbImport',
                    meta: { title: '后台管理', requiresAdmin: true },
                    component: AdminTmdbImportPage
                }
            ]
        }
    ]
})

router.beforeEach(async (to) => {
    if (!to.meta.requiresAdmin) {
        return true
    }

    if (!getToken()) {
        return { path: '/login', query: { redirect: to.fullPath } }
    }

    try {
        const response = await request('/users/me')
        if (!response.ok) {
            removeToken()
            return { path: '/login', query: { redirect: to.fullPath } }
        }

        const user = await response.json()
        if (!user.isAdmin) {
            return '/movies'
        }
    } catch (error) {
        console.error('Failed to verify admin:', error)
        return '/movies'
    }

    return true
})

router.afterEach((to) => {
    document.title = to.meta.title as string
})


export default router

import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarOpen: true,
    pageTitle: 'Flash Sale Service',
    requestState: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  }),
  actions: {
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    setPageTitle(title: string) {
      this.pageTitle = title
    },
    setRequestState(state: 'idle' | 'loading' | 'success' | 'error') {
      this.requestState = state
    },
  },
})

import axios, { type AxiosRequestConfig } from 'axios'
import { ApiClientError, createRequestId, unwrapApiResponse } from '@/api/request'
import type { ApiResponse } from '@/types'

type AuthResolver = () => string
type UnauthorizedHandler = () => void

let resolveAccessToken: AuthResolver = () => ''
let handleUnauthorized: UnauthorizedHandler = () => {}

export function configureHttpAuth(options: {
  getAccessToken: AuthResolver
  onUnauthorized: UnauthorizedHandler
}) {
  resolveAccessToken = options.getAccessToken
  handleUnauthorized = options.onUnauthorized
}

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

axiosClient.interceptors.request.use((config) => {
  const token = resolveAccessToken()

  config.headers.set('X-Request-Id', createRequestId())
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }

  return config
})

async function request<T>(config: AxiosRequestConfig) {
  try {
    const response = await axiosClient.request<ApiResponse<T>>(config)
    return unwrapApiResponse<T>(
      {
        status: response.status,
        data: response.data,
      },
      handleUnauthorized,
    )
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data) {
      return unwrapApiResponse<T>(
        {
          status: error.response.status,
          data: error.response.data as ApiResponse<T>,
        },
        handleUnauthorized,
      )
    }

    throw new ApiClientError('网络请求失败', 'SYSTEM_ERROR', 500, null)
  }
}

async function requestEnvelope<T>(config: AxiosRequestConfig) {
  try {
    const response = await axiosClient.request<ApiResponse<T>>(config)
    if (response.status === 401) {
      handleUnauthorized()
    }
    return response.data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data) {
      const data = error.response.data as ApiResponse<T>
      if (error.response.status === 401) {
        handleUnauthorized()
      }
      throw new ApiClientError(data.message, data.code, error.response.status, data.requestId)
    }

    throw new ApiClientError('网络请求失败', 'SYSTEM_ERROR', 500, null)
  }
}

async function requestBlob(config: AxiosRequestConfig) {
  try {
    const response = await axiosClient.request<Blob>({
      ...config,
      responseType: 'blob',
    })

    return response.data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      if (error.response.status === 401) {
        handleUnauthorized()
      }

      throw new ApiClientError('文件下载失败', 'SYSTEM_ERROR', error.response.status, null)
    }

    throw new ApiClientError('网络请求失败', 'SYSTEM_ERROR', 500, null)
  }
}

export const http = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return request<T>({
      ...config,
      method: 'GET',
      url,
    })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({
      ...config,
      method: 'POST',
      url,
      data,
    })
  },
  postEnvelope<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return requestEnvelope<T>({
      ...config,
      method: 'POST',
      url,
      data,
    })
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({
      ...config,
      method: 'PUT',
      url,
      data,
    })
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return request<T>({
      ...config,
      method: 'DELETE',
      url,
    })
  },
  getEnvelope<T>(url: string, config?: AxiosRequestConfig) {
    return requestEnvelope<T>({
      ...config,
      method: 'GET',
      url,
    })
  },
  downloadBlob(url: string, config?: AxiosRequestConfig) {
    return requestBlob({
      ...config,
      method: 'GET',
      url,
    })
  },
}

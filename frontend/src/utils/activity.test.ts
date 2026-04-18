import { describe, expect, it } from 'vitest'
import {
  getCodeSourceModeLabel,
  getImportFailureReasonLabel,
  getPublishModeLabel,
  isDeletableActivity,
  isEditableActivity,
  shouldShowCodeImportPanel,
} from '@/utils/activity'

describe('activity utils', () => {
  it('allows deleting unpublished and offline activities', () => {
    expect(isDeletableActivity({ publishStatus: 'UNPUBLISHED' })).toBe(true)
    expect(isDeletableActivity({ publishStatus: 'OFFLINE' })).toBe(true)
  })

  it('disallows deleting published activities', () => {
    expect(isDeletableActivity({ publishStatus: 'PUBLISHED' })).toBe(false)
  })

  it('keeps editability limited to unpublished activities', () => {
    expect(isEditableActivity({ publishStatus: 'UNPUBLISHED' })).toBe(true)
    expect(isEditableActivity({ publishStatus: 'OFFLINE' })).toBe(false)
  })

  it('returns readable labels for code source and publish mode', () => {
    expect(getCodeSourceModeLabel('SYSTEM_GENERATED')).toBe('系统生成')
    expect(getCodeSourceModeLabel('THIRD_PARTY_IMPORTED')).toBe('第三方导入')
    expect(getPublishModeLabel('IMMEDIATE')).toBe('立即发布')
    expect(getPublishModeLabel('SCHEDULED')).toBe('定时发布')
  })

  it('maps import failure reasons to readable labels', () => {
    expect(getImportFailureReasonLabel('EMPTY_CODE')).toBe('空白兑换码')
    expect(getImportFailureReasonLabel('INVALID_FORMAT')).toBe('兑换码格式非法')
    expect(getImportFailureReasonLabel('DUPLICATE_IN_FILE')).toBe('文件内重复')
    expect(getImportFailureReasonLabel('DUPLICATE_IN_SYSTEM')).toBe('系统内已存在')
    expect(getImportFailureReasonLabel('UNKNOWN_REASON')).toBe('UNKNOWN_REASON')
  })

  it('shows code import panel only for third-party imported activities', () => {
    expect(shouldShowCodeImportPanel({ codeSourceMode: 'THIRD_PARTY_IMPORTED' })).toBe(true)
    expect(shouldShowCodeImportPanel({ codeSourceMode: 'SYSTEM_GENERATED' })).toBe(false)
  })
})

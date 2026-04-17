import { describe, expect, it } from 'vitest'
import { isDeletableActivity, isEditableActivity } from '@/utils/activity'

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
})

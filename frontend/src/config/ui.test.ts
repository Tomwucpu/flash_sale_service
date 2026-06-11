import { describe, expect, it } from 'vitest'

import { elementPlusConfig } from './ui'

describe('elementPlusConfig', () => {
  it('shortens the default message duration', () => {
    expect(elementPlusConfig.message?.duration).toBe(1500)
  })
})

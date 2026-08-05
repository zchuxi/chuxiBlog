import assert from 'node:assert/strict'
import test from 'node:test'

import { getVisitorToken, saveVisitorToken, ensureVisitorToken, STORAGE_KEY } from './visitorId.js'

test('getVisitorToken reuses a stored token', () => {
  const expected = 'raw1234567890abcdef.0123456789abcdef'
  globalThis.localStorage = {
    getItem: key => key === STORAGE_KEY ? expected : null,
    setItem: () => assert.fail('valid token should not be replaced')
  }

  assert.equal(getVisitorToken(), expected)
})

test('getVisitorToken returns empty when nothing stored', () => {
  globalThis.localStorage = {
    getItem: () => null,
    setItem: () => {}
  }

  assert.equal(getVisitorToken(), '')
})

test('saveVisitorToken stores token', () => {
  let saved = ''
  globalThis.localStorage = {
    getItem: () => null,
    setItem: (key, value) => {
      assert.equal(key, STORAGE_KEY)
      saved = value
    }
  }

  saveVisitorToken('abc123.def456')
  assert.equal(saved, 'abc123.def456')
})

test('saveVisitorToken ignores empty token', () => {
  globalThis.localStorage = {
    getItem: () => null,
    setItem: () => assert.fail('empty token should not be written')
  }

  saveVisitorToken('')
  saveVisitorToken(null)
})

test('ensureVisitorToken returns existing token without network', async () => {
  const expected = 'abc123.def456'
  globalThis.localStorage = {
    getItem: key => key === STORAGE_KEY ? expected : null,
    setItem: () => assert.fail('should not write when token exists')
  }

  assert.equal(await ensureVisitorToken(), expected)
})

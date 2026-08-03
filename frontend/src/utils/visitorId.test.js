import assert from 'node:assert/strict'
import test from 'node:test'

import { getVisitorId, STORAGE_KEY, VALID_ID } from './visitorId.js'

test('getVisitorId reuses a valid stored id', () => {
  const expected = 'visitor_1234567890abcdef'
  globalThis.localStorage = {
    getItem: key => key === STORAGE_KEY ? expected : null,
    setItem: () => assert.fail('valid id should not be replaced')
  }

  assert.equal(getVisitorId(), expected)
})

test('getVisitorId replaces invalid storage with a valid id', () => {
  let saved = ''
  globalThis.localStorage = {
    getItem: () => 'bad id',
    setItem: (key, value) => {
      assert.equal(key, STORAGE_KEY)
      saved = value
    }
  }

  const id = getVisitorId()
  assert.match(id, VALID_ID)
  assert.equal(saved, id)
})

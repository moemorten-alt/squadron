import { describe, expect, it } from 'vitest';
import { formatLeaverDate } from './formatLeaverDate';

describe('formatLeaverDate', () => {
  it('formats an ISO date as a Left label', () => {
    expect(formatLeaverDate('2026-03-14')).toBe('Left 2026-03-14');
  });

  it('returns null when the field is absent', () => {
    // PersonDto is @JsonInclude(NON_NULL), so the key is missing, not null.
    expect(formatLeaverDate(undefined)).toBeNull();
  });

  it('returns null for an empty string', () => {
    expect(formatLeaverDate('')).toBeNull();
  });
});

import { describe, expect, it } from 'vitest';
import { allocationBadgeClass, cn, formatPercent } from './utils';

describe('cn', () => {
  it('merges class names and resolves Tailwind conflicts', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4');
  });

  it('drops falsy values', () => {
    expect(cn('a', false, undefined, null, 'b')).toBe('a b');
  });
});

describe('formatPercent', () => {
  it('appends a percent sign', () => {
    expect(formatPercent(80)).toBe('80%');
    expect(formatPercent(0)).toBe('0%');
  });
});

// These assert the light-mode base classes only. The badge palette also carries
// dark: variants, which are a styling concern - what matters here is which
// bucket a given percentage lands in.
describe('allocationBadgeClass', () => {
  it('returns the gray/idle class at 0%', () => {
    expect(allocationBadgeClass(0)).toContain('bg-gray-100');
    expect(allocationBadgeClass(0)).toContain('text-gray-500');
  });

  it('returns the yellow/under-allocated class below 80%', () => {
    for (const p of [1, 79]) {
      expect(allocationBadgeClass(p)).toContain('bg-yellow-100');
      expect(allocationBadgeClass(p)).toContain('text-yellow-800');
    }
  });

  it('returns the green/healthy class from 80% up to and including 100%', () => {
    for (const p of [80, 100]) {
      expect(allocationBadgeClass(p)).toContain('bg-green-100');
      expect(allocationBadgeClass(p)).toContain('text-green-800');
    }
  });

  it('returns the red/over-allocated class above 100%', () => {
    for (const p of [101, 150]) {
      expect(allocationBadgeClass(p)).toContain('bg-red-100');
      expect(allocationBadgeClass(p)).toContain('text-red-700');
    }
  });

  it('keeps the buckets mutually exclusive across the boundaries', () => {
    expect(allocationBadgeClass(79)).not.toContain('green');
    expect(allocationBadgeClass(80)).not.toContain('yellow');
    expect(allocationBadgeClass(100)).not.toContain('red');
    expect(allocationBadgeClass(101)).not.toContain('green');
  });
});

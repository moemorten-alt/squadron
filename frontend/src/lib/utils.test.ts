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

describe('allocationBadgeClass', () => {
  it('returns the gray/idle class at 0%', () => {
    expect(allocationBadgeClass(0)).toBe('bg-gray-100 text-gray-500');
  });

  it('returns the yellow/under-allocated class below 80%', () => {
    expect(allocationBadgeClass(1)).toBe('bg-yellow-100 text-yellow-800');
    expect(allocationBadgeClass(79)).toBe('bg-yellow-100 text-yellow-800');
  });

  it('returns the green/healthy class from 80% up to and including 100%', () => {
    expect(allocationBadgeClass(80)).toBe('bg-green-100 text-green-800');
    expect(allocationBadgeClass(100)).toBe('bg-green-100 text-green-800');
  });

  it('returns the red/over-allocated class above 100%', () => {
    expect(allocationBadgeClass(101)).toBe('bg-red-100 text-red-700');
    expect(allocationBadgeClass(150)).toBe('bg-red-100 text-red-700');
  });
});

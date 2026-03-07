import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function allocationBadgeClass(percent: number): string {
  if (percent === 0)   return 'bg-gray-100 text-gray-500';
  if (percent < 80)    return 'bg-yellow-100 text-yellow-800';
  if (percent <= 100)  return 'bg-green-100 text-green-800';
  return 'bg-red-100 text-red-700';
}

export function formatPercent(n: number) {
  return `${n}%`;
}

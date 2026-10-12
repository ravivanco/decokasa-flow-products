import { describe, expect, it } from 'vitest';
import { cn } from '@/lib/utils';

describe('cn', () => {
  it('une clases y descarta valores falsos', () => {
    expect(cn('px-2', false, undefined, null, 'text-sm')).toBe('px-2 text-sm');
  });

  it('resuelve conflictos de Tailwind dejando la última clase', () => {
    expect(cn('px-2 py-1', 'px-4')).toBe('py-1 px-4');
  });

  it('acepta clases condicionales en objeto', () => {
    expect(cn('font-medium', { 'text-primary': true, hidden: false })).toBe('font-medium text-primary');
  });
});

import { describe, it, expect } from 'vitest';
import { isLocalAppTarget, shouldForceComplexTier } from './intent-router';

describe('intent-router tier overrides', () => {
  it('treats Kai local apps as local targets', () => {
    expect(isLocalAppTarget('kainotes')).toBe(true);
    expect(isLocalAppTarget('app:kaiclock')).toBe(true);
    expect(isLocalAppTarget('browser')).toBe(false);
    expect(isLocalAppTarget('x')).toBe(false);
  });

  it('forces COMPLEX for explicit integration targets like browser and x', () => {
    expect(shouldForceComplexTier('browser')).toBe(true);
    expect(shouldForceComplexTier('x')).toBe(true);
    expect(shouldForceComplexTier('gmail')).toBe(true);
    expect(shouldForceComplexTier('app:youtube')).toBe(true);
  });

  it('does not force COMPLEX for local app targets', () => {
    expect(shouldForceComplexTier('kainotes')).toBe(false);
    expect(shouldForceComplexTier('app:kaicalendar')).toBe(false);
    expect(shouldForceComplexTier(null)).toBe(false);
  });
});

import { describe, it, expect } from 'vitest';
import { mapAppTargetToToolkitSlug } from './connection-manager';

describe('mapAppTargetToToolkitSlug', () => {
  it('maps integration aliases to Composio toolkit slugs', () => {
    expect(mapAppTargetToToolkitSlug('x')).toBe('twitter');
    expect(mapAppTargetToToolkitSlug('browser')).toBe('composio_search');
    expect(mapAppTargetToToolkitSlug('search')).toBe('composio_search');
  });

  it('passes through known toolkit slugs unchanged', () => {
    expect(mapAppTargetToToolkitSlug('twitter')).toBe('twitter');
    expect(mapAppTargetToToolkitSlug('github')).toBe('github');
  });
});

import { describe, it, expect } from 'vitest';
import { resolveToolSlugs } from './tool-executor';

describe('resolveToolSlugs', () => {
  it('returns the curated default tool set for googlecalendar', () => {
    const slugs = resolveToolSlugs('googlecalendar');

    expect(slugs).toContain('GOOGLESUPER_CREATE_EVENT');
    expect(slugs).toContain('GOOGLESUPER_EVENTS_LIST');
    expect(slugs).toContain('GOOGLESUPER_LIST_CALENDARS');
    expect(slugs.length).toBeGreaterThan(1);
  });

  it('does not narrow to create-only tools when classifier would say create', () => {
    const slugs = resolveToolSlugs('googlecalendar');

    expect(slugs).not.toEqual(['GOOGLESUPER_CREATE_EVENT']);
  });

  it('returns empty array for unknown app targets', () => {
    expect(resolveToolSlugs('nonexistent_app')).toEqual([]);
  });
});

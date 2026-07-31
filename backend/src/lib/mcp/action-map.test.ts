import { describe, it, expect } from 'vitest';
import { COMPOSIO_ACTION_MAP } from './action-map';

describe('COMPOSIO_ACTION_MAP', () => {
  it('should be defined and not empty', () => {
    expect(COMPOSIO_ACTION_MAP).toBeDefined();
    expect(Object.keys(COMPOSIO_ACTION_MAP).length).toBeGreaterThan(0);
  });

  it('should contain all the default 14 integrations', () => {
    const defaultKeys = [
      'gmail', 'googlecalendar', 'googlesheets', 'googledrive', 'googletasks',
      'spotify', 'todoist', 'notion', 'slackbot', 'slack', 'microsoftteams',
      'onedrive', 'github', 'search'
    ];
    defaultKeys.forEach(key => {
      expect(COMPOSIO_ACTION_MAP).toHaveProperty(key);
      expect(COMPOSIO_ACTION_MAP[key]).toHaveProperty('default');
      expect(Array.isArray(COMPOSIO_ACTION_MAP[key].default)).toBe(true);
      expect(COMPOSIO_ACTION_MAP[key].default.length).toBeGreaterThan(0);
    });
  });

  it('should contain the 34 newly added drawer integrations', () => {
    const newKeys = [
      'composio', 'googledocs', 'googlecontacts', 'googleforms', 'googlemaps',
      'googlechat', 'googleclassroom', 'googleslides', 'googlephotos', 'googlemeet',
      'googlesuper', 'supabase', 'outlook', 'twitter', 'hubspot', 'linear',
      'airtable', 'jira', 'youtube', 'canvas', 'bitbucket', 'discord', 'figma',
      'reddit', 'hackernews', 'asana', 'shopify', 'linkedin', 'docusign',
      'discordbot', 'salesforce', 'calendly', 'trello', 'dropbox'
    ];
    newKeys.forEach(key => {
      expect(COMPOSIO_ACTION_MAP).toHaveProperty(key);
      expect(COMPOSIO_ACTION_MAP[key]).toHaveProperty('default');
      expect(Array.isArray(COMPOSIO_ACTION_MAP[key].default)).toBe(true);
      expect(COMPOSIO_ACTION_MAP[key].default.length).toBeGreaterThan(0);
    });
  });

  it('should map search and composio_search to COMPOSIO_SEARCH tools', () => {
    for (const key of ['search', 'composio_search']) {
      expect(COMPOSIO_ACTION_MAP[key].default).toEqual(
        expect.arrayContaining(['COMPOSIO_SEARCH_WEB', 'COMPOSIO_SEARCH_DUCK_DUCK_GO'])
      );
      expect(COMPOSIO_ACTION_MAP[key].default.every((slug) => slug.startsWith('COMPOSIO_SEARCH_'))).toBe(true);
    }
  });

  it('should map googledocs to GOOGLESUPER docs tools', () => {
    const docs = COMPOSIO_ACTION_MAP.googledocs;
    expect(docs.default).toEqual([
      'GOOGLESUPER_CREATE_DOCUMENT',
      'GOOGLESUPER_GET_DOCUMENT_PLAINTEXT',
      'GOOGLESUPER_SEARCH_DOCUMENTS',
      'GOOGLESUPER_INSERT_TEXT_ACTION',
    ]);
    expect(docs.create).toEqual(['GOOGLESUPER_CREATE_DOCUMENT']);
    expect(docs.default.every((slug) => slug.startsWith('GOOGLESUPER_'))).toBe(true);
    expect(docs.default.some((slug) => slug.startsWith('GOOGLEDOCS_'))).toBe(false);
  });

  it('should map remapped Google integrations to GOOGLESUPER tools', () => {
    const googlesuperApps = ['googlemaps', 'googleslides', 'googlephotos', 'googlemeet'] as const;
    for (const app of googlesuperApps) {
      expect(COMPOSIO_ACTION_MAP[app].default.every((slug) => slug.startsWith('GOOGLESUPER_'))).toBe(true);
    }
    expect(COMPOSIO_ACTION_MAP.googlemaps.default).toContain('GOOGLESUPER_GEOCODE_ADDRESS');
    expect(COMPOSIO_ACTION_MAP.googleslides.default).toContain('GOOGLESUPER_CREATE_PRESENTATION');
    expect(COMPOSIO_ACTION_MAP.googleslides.default).not.toContain('GOOGLESLIDES_CREATE_SLIDES_MARKDOWN');
    expect(COMPOSIO_ACTION_MAP.googlephotos.default).toContain('GOOGLESUPER_SEARCH_MEDIA_ITEMS');
    expect(COMPOSIO_ACTION_MAP.googlemeet.default).toContain('GOOGLESUPER_CREATE_MEET');
  });

  it('should keep standalone toolkit slugs for Google apps without GOOGLESUPER coverage', () => {
    expect(COMPOSIO_ACTION_MAP.googlecontacts.default.every((slug) => slug.startsWith('GOOGLECONTACTS_'))).toBe(true);
    expect(COMPOSIO_ACTION_MAP.googleforms.default.every((slug) => slug.startsWith('GOOGLEFORMS_'))).toBe(true);
    expect(COMPOSIO_ACTION_MAP.googlechat.default.every((slug) => slug.startsWith('GOOGLE_CHAT_'))).toBe(true);
    expect(COMPOSIO_ACTION_MAP.googleclassroom.default.every((slug) => slug.startsWith('GOOGLE_CLASSROOM_'))).toBe(true);
    expect(COMPOSIO_ACTION_MAP.youtube.default.some((slug) => slug.startsWith('YOUTUBE_'))).toBe(true);
  });

  it('should not contain any empty default action mappings', () => {
    Object.keys(COMPOSIO_ACTION_MAP).forEach(key => {
      const mapping = COMPOSIO_ACTION_MAP[key];
      expect(mapping).toBeDefined();
      expect(mapping.default).toBeDefined();
      expect(Array.isArray(mapping.default)).toBe(true);
      expect(mapping.default.length).toBeGreaterThan(0);
    });
  });
});

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

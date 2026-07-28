// Verified action map mapping [appTarget] -> [userIntent] -> [Composio Slugs]
export const COMPOSIO_ACTION_MAP: Record<string, Record<string, string[]>> = {
  'gmail': {
    'default': [
      'GOOGLESUPER_FETCH_EMAILS',
      'GOOGLESUPER_SEND_EMAIL',
      'GOOGLESUPER_CREATE_EMAIL_DRAFT',
      'GOOGLESUPER_SEND_DRAFT',
      'GOOGLESUPER_REPLY_TO_THREAD'
    ],
    'send': ['GOOGLESUPER_SEND_EMAIL', 'GOOGLESUPER_CREATE_EMAIL_DRAFT'],
    'read': ['GOOGLESUPER_FETCH_EMAILS', 'GOOGLESUPER_GET_DRAFT', 'GOOGLESUPER_LIST_DRAFTS'],
    'search': ['GOOGLESUPER_FETCH_EMAILS'],
    'list': ['GOOGLESUPER_FETCH_EMAILS', 'GOOGLESUPER_LIST_DRAFTS'],
    'fetch': ['GOOGLESUPER_FETCH_EMAILS'],
  },
  'googlecalendar': {
    'default': [
      'GOOGLESUPER_CREATE_EVENT',
      'GOOGLESUPER_DELETE_EVENT',
      'GOOGLESUPER_EVENTS_LIST',
      'GOOGLESUPER_LIST_CALENDARS',
      'GOOGLESUPER_UPDATE_EVENT',
      'GOOGLESUPER_PATCH_EVENT'
    ],
    'create': ['GOOGLESUPER_CREATE_EVENT'],
    'read': ['GOOGLESUPER_EVENTS_LIST', 'GOOGLESUPER_LIST_CALENDARS'],
    'update': ['GOOGLESUPER_UPDATE_EVENT', 'GOOGLESUPER_PATCH_EVENT'],
  },
  'googlesheets': {
    'default': [
      'GOOGLESUPER_CREATE_GOOGLE_SHEET1',
      'GOOGLESUPER_SPREADSHEETS_VALUES_APPEND',
      'GOOGLESUPER_VALUES_GET',
      'GOOGLESUPER_BATCH_UPDATE',
      'GOOGLESUPER_GET_SHEET_NAMES',
      'GOOGLESUPER_GET_SPREADSHEET_INFO'
    ],
    'create': ['GOOGLESUPER_CREATE_GOOGLE_SHEET1'],
    'write': ['GOOGLESUPER_SPREADSHEETS_VALUES_APPEND', 'GOOGLESUPER_BATCH_UPDATE'],
    'read': ['GOOGLESUPER_VALUES_GET', 'GOOGLESUPER_GET_SHEET_NAMES'],
  },
  'googledrive': {
    'default': [
      'GOOGLESUPER_FIND_FILE',
      'GOOGLESUPER_FIND_FOLDER',
      'GOOGLESUPER_DOWNLOAD_FILE',
      'GOOGLESUPER_CREATE_FILE',
      'GOOGLESUPER_CREATE_FOLDER',
      'GOOGLESUPER_COPY_FILE',
      'GOOGLESUPER_DELETE_FILE'
    ],
  },
  'googletasks': {
    'default': [
      'GOOGLESUPER_INSERT_TASK',
      'GOOGLESUPER_LIST_TASKS',
      'GOOGLESUPER_LIST_TASK_LISTS',
      'GOOGLESUPER_UPDATE_TASK',
      'GOOGLESUPER_DELETE_TASK',
      'GOOGLESUPER_PATCH_TASK'
    ],
  },
  'spotify': {
    'default': [
      'SPOTIFY_START_RESUME_PLAYBACK',
      'SPOTIFY_PAUSE_PLAYBACK',
      'SPOTIFY_GET_CURRENTLY_PLAYING_TRACK',
      'SPOTIFY_GET_THE_USER_S_QUEUE',
      'SPOTIFY_GET_CURRENT_USER_S_PLAYLISTS',
      'SPOTIFY_ADD_ITEM_TO_PLAYBACK_QUEUE',
      'SPOTIFY_GET_AVAILABLE_DEVICES',
      'SPOTIFY_CREATE_PLAYLIST',
      'SPOTIFY_ADD_ITEMS_TO_PLAYLIST'
    ],
  },
  'todoist': {
    'default': [
      'TODOIST_CREATE_TASK',
      'TODOIST_GET_TASK',
      'TODOIST_GET_ALL_TASKS',
      'TODOIST_DELETE_TASK',
      'TODOIST_CREATE_PROJECT',
      'TODOIST_GET_ALL_PROJECTS'
    ],
  },
  'notion': {
    'default': [
      'NOTION_CREATE_NOTION_PAGE',
      'NOTION_INSERT_ROW_DATABASE',
      'NOTION_APPEND_TEXT_BLOCKS',
      'NOTION_QUERY_DATABASE',
      'NOTION_FETCH_DATA'
    ],
  },
  'slackbot': {
    'default': [
      'SLACKBOT_CREATE_CHANNEL',
      'SLACKBOT_INVITE_USERS_TO_A_CHANNEL',
      'SLACKBOT_DELETES_A_MESSAGE_FROM_A_CHAT',
      'SLACK_CHAT_POST_MESSAGE',
      'SLACK_LIST_UNREAD_CHANNEL_MESSAGES'
    ],
  },
  'slack': {
    'default': [
      'SLACK_CREATE_CHANNEL',
      'SLACK_CHAT_POST_MESSAGE',
      'SLACK_DELETES_A_MESSAGE_FROM_A_CHAT',
      'SLACK_LIST_UNREAD_CHANNEL_MESSAGES',
      'SLACK_INVITE_USER_TO_CHANNEL'
    ],
  },
  'microsoftteams': {
    'default': [
      'MICROSOFT_TEAMS_CREATE_CHANNEL',
      'MICROSOFT_TEAMS_CHATS_GET_ALL_CHATS',
      'MICROSOFT_TEAMS_CHATS_GET_ALL_MESSAGES',
      'MICROSOFT_TEAMS_ADD_TEAM_MEMBER',
      'MICROSOFT_TEAMS_TEAMS_LIST_CHANNELS',
      'MICROSOFT_TEAMS_LIST_MESSAGE_REPLIES'
    ],
  },
  'onedrive': {
    'default': [
      'ONE_DRIVE_DOWNLOAD_FILE',
      'ONE_DRIVE_ONEDRIVE_FIND_FILE',
      'ONE_DRIVE_LIST_ALL_DRIVE_ITEMS',
      'ONE_DRIVE_SEARCH_DRIVE_ITEMS',
      'ONE_DRIVE_GET_ITEM',
      'ONE_DRIVE_DELETE_ITEM'
    ],
  },
  'github': {
    'default': [
      'GITHUB_ADD_ASSIGNEES_TO_AN_ISSUE',
      'GITHUB_ADD_LABELS_TO_AN_ISSUE',
      'GITHUB_ADD_SUB_ISSUE',
      'GITHUB_APPROVE_WORKFLOW_RUN_FOR_FORK_PULL_REQUEST',
      'GITHUB_CREATE_ISSUE',
      'GITHUB_FIND_PULL_REQUESTS',
      'GITHUB_FIND_REPOSITORIES'
    ],
  },
  'search': {
    'default': [
      'EXA_SEARCH',
      'EXA_ANSWER',
      'EXA_GET_CONTENTS_ACTION',
      'EXA_FIND_SIMILAR'
    ]
  },
  'composio': {
    'default': [
      'COMPOSIO_SEARCH_TOOLS',
      'COMPOSIO_LIST_TOOLKITS',
      'COMPOSIO_GET_TOOL_SCHEMAS',
      'COMPOSIO_GET_REQUIRED_PARAMETERS'
    ]
  },
  'googledocs': {
    'default': [
      'GOOGLEDOCS_CREATE_DOCUMENT',
      'GOOGLEDOCS_CREATE_DOCUMENT_MARKDOWN',
      'GOOGLEDOCS_GET_DOCUMENT_PLAINTEXT',
      'GOOGLEDOCS_SEARCH_DOCUMENTS',
      'GOOGLEDOCS_INSERT_TEXT_ACTION'
    ]
  },
  'googlecontacts': {
    'default': [
      'GOOGLECONTACTS_CREATE_CONTACT',
      'GOOGLECONTACTS_GET_PERSON',
      'GOOGLECONTACTS_SEARCH_CONTACTS',
      'GOOGLECONTACTS_BATCH_CREATE_CONTACTS'
    ]
  },
  'googleforms': {
    'default': [
      'GOOGLEFORMS_CREATE_FORM',
      'GOOGLEFORMS_GET_FORM',
      'GOOGLEFORMS_LIST_RESPONSES',
      'GOOGLEFORMS_BATCH_UPDATE_FORM'
    ]
  },
  'googlemaps': {
    'default': [
      'GOOGLE_MAPS_GEOCODE_ADDRESS',
      'GOOGLE_MAPS_GET_DIRECTION',
      'GOOGLE_MAPS_GET_ROUTE',
      'GOOGLE_MAPS_GET_PLACE_DETAILS'
    ]
  },
  'googlechat': {
    'default': [
      'GOOGLE_CHAT_CREATE_MESSAGE',
      'GOOGLE_CHAT_CREATE_SPACE',
      'GOOGLE_CHAT_GET_ATTACHMENT'
    ]
  },
  'googleclassroom': {
    'default': [
      'GOOGLE_CLASSROOM_COURSES_CREATE',
      'GOOGLE_CLASSROOM_COURSES_STUDENTS_CREATE',
      'GOOGLE_CLASSROOM_COURSE_WORK_CREATE',
      'GOOGLE_CLASSROOM_COURSES_ANNOUNCEMENTS_CREATE'
    ]
  },
  'googleslides': {
    'default': [
      'GOOGLESLIDES_CREATE_PRESENTATION',
      'GOOGLESLIDES_CREATE_SLIDES_MARKDOWN',
      'GOOGLESLIDES_PRESENTATIONS_GET'
    ]
  },
  'googlephotos': {
    'default': [
      'GOOGLEPHOTOS_CREATE_ALBUM',
      'GOOGLEPHOTOS_GET_ALBUM',
      'GOOGLEPHOTOS_SEARCH_MEDIA_ITEMS',
      'GOOGLEPHOTOS_BATCH_CREATE_MEDIA_ITEMS'
    ]
  },
  'googlemeet': {
    'default': [
      'GOOGLEMEET_CREATE_MEET',
      'GOOGLEMEET_GET_MEET',
      'GOOGLEMEET_GET_TRANSCRIPT'
    ]
  },
  'googlesuper': {
    'default': [
      'GOOGLESUPER_FETCH_EMAILS',
      'GOOGLESUPER_SEND_EMAIL',
      'GOOGLESUPER_CREATE_EVENT',
      'GOOGLESUPER_EVENTS_LIST',
      'GOOGLESUPER_SPREADSHEETS_VALUES_APPEND',
      'GOOGLESUPER_VALUES_GET',
      'GOOGLESUPER_FIND_FILE',
      'GOOGLESUPER_INSERT_TASK'
    ]
  },
  'supabase': {
    'default': [
      'SUPABASE_CREATE_A_PROJECT',
      'SUPABASE_CREATE_FUNCTION',
      'SUPABASE_CREATE_ORGANIZATION'
    ]
  },
  'outlook': {
    'default': [
      'OUTLOOK_CALENDAR_CREATE_EVENT',
      'OUTLOOK_CREATE_CALENDAR_EVENT',
      'OUTLOOK_CREATE_CALENDAR'
    ]
  },
  'twitter': {
    'default': [
      'TWITTER_CREATE_DM_CONVERSATION',
      'TWITTER_CREATE_LIST',
      'TWITTER_ADD_POST_TO_BOOKMARKS'
    ]
  },
  'hubspot': {
    'default': [
      'HUBSPOT_CREATE_CONTACT',
      'HUBSPOT_CREATE_COMPANY',
      'HUBSPOT_CREATE_DEAL'
    ]
  },
  'linear': {
    'default': [
      'LINEAR_CREATE_LINEAR_ISSUE',
      'LINEAR_CREATE_LINEAR_PROJECT',
      'LINEAR_CREATE_LINEAR_COMMENT'
    ]
  },
  'airtable': {
    'default': [
      'AIRTABLE_CREATE_BASE',
      'AIRTABLE_CREATE_RECORD',
      'AIRTABLE_CREATE_TABLE'
    ]
  },
  'jira': {
    'default': [
      'JIRA_CREATE_ISSUE',
      'JIRA_CREATE_PROJECT',
      'JIRA_CREATE_SPRINT'
    ]
  },
  'youtube': {
    'default': [
      'EXA_SEARCH',
      'YOUTUBE_CREATE_PLAYLIST',
      'YOUTUBE_ADD_VIDEO_TO_PLAYLIST',
      'YOUTUBE_GET_CHANNEL_STATISTICS'
    ],
    'search': ['EXA_SEARCH'],
    'read': ['EXA_SEARCH'],
    'list': ['EXA_SEARCH']
  },
  'canvas': {
    'default': [
      'CANVAS_CREATE_ASSIGNMENT',
      'CANVAS_CREATE_ASSIGNMENT_GROUP'
    ]
  },
  'bitbucket': {
    'default': [
      'BITBUCKET_CREATE_REPOSITORY',
      'BITBUCKET_CREATE_BRANCH',
      'BITBUCKET_CREATE_PULL_REQUEST'
    ]
  },
  'discord': {
    'default': [
      'DISCORD_GET_MY_USER',
      'DISCORD_GET_GATEWAY',
      'DISCORD_GET_INVITE'
    ]
  },
  'figma': {
    'default': [
      'FIGMA_ADD_A_COMMENT_TO_A_FILE',
      'FIGMA_GET_COMMENTS_IN_A_FILE',
      'FIGMA_CREATE_A_WEBHOOK'
    ]
  },
  'reddit': {
    'default': [
      'REDDIT_CREATE_REDDIT_POST',
      'REDDIT_GET_NEW',
      'REDDIT_GET_R_TOP'
    ]
  },
  'hackernews': {
    'default': [
      'HACKERNEWS_GET_TOP_STORIES',
      'HACKERNEWS_GET_LATEST_POSTS',
      'HACKERNEWS_SEARCH_POSTS'
    ]
  },
  'asana': {
    'default': [
      'ASANA_CREATE_A_TASK',
      'ASANA_CREATE_A_PROJECT',
      'ASANA_ADD_MEMBERS_TO_PROJECT'
    ]
  },
  'shopify': {
    'default': [
      'SHOPIFY_CREATES_A_NEW_PRODUCT',
      'SHOPIFY_BULK_CREATE_PRODUCTS',
      'SHOPIFY_CREATE_ARTICLE'
    ]
  },
  'linkedin': {
    'default': [
      'LINKEDIN_CREATE_LINKED_IN_POST',
      'LINKEDIN_CREATE_COMMENT_ON_POST',
      'LINKEDIN_GET_MY_INFO'
    ]
  },
  'docusign': {
    'default': [
      'DOCUSIGN_CREATE_BULK_SEND_REQUEST',
      'DOCUSIGN_ADD_MEMBERS_TO_SIGNING_GROUP',
      'DOCUSIGN_ADD_FILE_TO_WORKSPACE'
    ]
  },
  'discordbot': {
    'default': [
      'DISCORDBOT_CREATE_DM',
      'DISCORDBOT_CREATE_GUILD_CHANNEL'
    ]
  },
  'salesforce': {
    'default': [
      'SALESFORCE_CREATE_ACCOUNT',
      'SALESFORCE_CREATE_CONTACT',
      'SALESFORCE_CREATE_CAMPAIGN'
    ]
  },
  'calendly': {
    'default': [
      'CALENDLY_CREATE_SCHEDULING_LINK',
      'CALENDLY_GET_CURRENT_USER',
      'CALENDLY_CANCEL_SCHEDULED_EVENT'
    ]
  },
  'trello': {
    'default': [
      'TRELLO_ADD_CARDS',
      'TRELLO_ADD_LISTS',
      'TRELLO_ADD_BOARDS'
    ]
  },
  'dropbox': {
    'default': [
      'DROPBOX_CREATE_FOLDER',
      'DROPBOX_CREATE_SHARED_LINK'
    ]
  }
};

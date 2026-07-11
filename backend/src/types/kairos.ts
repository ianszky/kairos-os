export interface KairosResponse {
  type: 'RESPONSE' | 'WIDGET' | 'TEXT' | 'ANDROID_INTENT' | 'DEEP_LINK' | 'ERROR';
  widget?: WidgetPayload;
  text?: string;
  intent?: AndroidIntentPayload;
  deepLink?: string;
  meta?: {
    conversationId: string;
    timestamp: string;
    model: string;
  };
}

export interface WidgetPayload {
  widgetType: 'EMAIL_LIST' | 'CALENDAR_EVENT' | 'ALARM_CONFIRM'
            | 'NOTE_CARD' | 'MUSIC_CARD' | 'SEARCH_RESULTS'
            | 'DIGEST_SUMMARY' | 'GENERIC_CARD';
  title?: string;
  items: WidgetItem[];
  actions?: WidgetAction[];
}

export interface WidgetItem {
  id: string;
  primary: string;
  secondary?: string;
  icon?: string;
  metadata?: Record<string, string>;
}

export interface WidgetAction {
  label: string;
  actionType: 'DEEP_LINK' | 'CALLBACK' | 'DISMISS';
  target: string;
}

export interface AndroidIntentPayload {
  action: string;
  data?: string;
  extras?: Record<string, string>;
}

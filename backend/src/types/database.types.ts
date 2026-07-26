export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export interface Database {
  public: {
    Tables: {
      conversations: {
        Row: {
          id: string
          user_id: string
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          user_id: string
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          created_at?: string
          updated_at?: string
        }
      }
      messages: {
        Row: {
          id: string
          conversation_id: string
          user_id: string
          content: string
          role: 'user' | 'assistant'
          created_at: string
        }
        Insert: {
          id?: string
          conversation_id: string
          user_id: string
          content: string
          role: 'user' | 'assistant'
          created_at?: string
        }
        Update: {
          id?: string
          conversation_id?: string
          user_id?: string
          content?: string
          role?: 'user' | 'assistant'
          created_at?: string
        }
      }
      notifications: {
        Row: {
          id: string
          user_id: string
          title: string
          body: string
          is_read: boolean
          created_at: string
        }
        Insert: {
          id?: string
          user_id: string
          title: string
          body: string
          is_read?: boolean
          created_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          title?: string
          body?: string
          is_read?: boolean
          created_at?: string
        }
      }
      app_configs: {
        Row: {
          id: string
          app_target: string
          settings: Json
        }
        Insert: {
          id?: string
          app_target: string
          settings?: Json
        }
        Update: {
          id?: string
          app_target?: string
          settings?: Json
        }
      }
      profiles: {
        Row: {
          id: string
          name: string
          avatar_url: string | null
        }
        Insert: {
          id: string
          name: string
          avatar_url?: string | null
        }
        Update: {
          id?: string
          name?: string
          avatar_url?: string | null
        }
      }
      user_app_configs: {
        Row: {
          id: string
          user_id: string
          app_identifier: string
          category: 'UTILITY' | 'TRAP'
          intent_gate_enabled: boolean
          default_time_limit: number | null
          vip_contact: boolean
          pending_category: 'UTILITY' | 'TRAP' | null
          pending_change_effective_at: string | null
        }
        Insert: {
          id?: string
          user_id: string
          app_identifier: string
          category?: 'UTILITY' | 'TRAP'
          intent_gate_enabled?: boolean
          default_time_limit?: number | null
          vip_contact?: boolean
          pending_category?: 'UTILITY' | 'TRAP' | null
          pending_change_effective_at?: string | null
        }
        Update: {
          id?: string
          user_id?: string
          app_identifier?: string
          category?: 'UTILITY' | 'TRAP'
          intent_gate_enabled?: boolean
          default_time_limit?: number | null
          vip_contact?: boolean
          pending_category?: 'UTILITY' | 'TRAP' | null
          pending_change_effective_at?: string | null
        }
      }
      intent_logs: {
        Row: {
          id: string
          user_id: string
          app_identifier: string
          app_display_name: string | null
          reason: string
          time_limit_minutes: number
          ai_approved: boolean
          opened_at: string
          closed_at: string | null
          exceeded_time: boolean
        }
        Insert: {
          id?: string
          user_id: string
          app_identifier: string
          app_display_name?: string | null
          reason: string
          time_limit_minutes: number
          ai_approved?: boolean
          opened_at?: string
          closed_at?: string | null
          exceeded_time?: boolean
        }
        Update: {
          id?: string
          user_id?: string
          app_identifier?: string
          app_display_name?: string | null
          reason?: string
          time_limit_minutes?: number
          ai_approved?: boolean
          opened_at?: string
          closed_at?: string | null
          exceeded_time?: boolean
        }
      }
      user_settings: {
        Row: {
          id: string
          user_id: string
          daily_leisure_minutes: number
          daily_leisure_minutes_pending: number | null
          pending_change_effective_at: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          user_id: string
          daily_leisure_minutes?: number
          daily_leisure_minutes_pending?: number | null
          pending_change_effective_at?: string | null
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          user_id?: string
          daily_leisure_minutes?: number
          daily_leisure_minutes_pending?: number | null
          pending_change_effective_at?: string | null
          created_at?: string
          updated_at?: string
        }
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      [_ in never]: never
    }
    Enums: {
      [_ in never]: never
    }
    CompositeTypes: {
      [_ in never]: never
    }
  }
}


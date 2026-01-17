/**
 * Application configuration
 * Values can be overridden via environment variables (prefixed with VITE_)
 */

export interface ModelConfig {
  name: string;
  version: string;
  description: string;
}

export interface AppConfig {
  appName: string;
  appTitle: string;
  model: ModelConfig;
  vectorStore: string;
}

// Default configuration
const defaultConfig: AppConfig = {
  appName: 'BMC Remedy RAG',
  appTitle: 'Damee GPT',
  model: {
    name: 'GLM',
    version: '4',
    description: 'AI-powered IT support assistant with access to your ITSM knowledge base.',
  },
  vectorStore: 'pgvector',
};

// Load configuration with environment variable overrides
export function loadConfig(): AppConfig {
  return {
    appName: import.meta.env.VITE_APP_NAME || defaultConfig.appName,
    appTitle: import.meta.env.VITE_APP_TITLE || defaultConfig.appTitle,
    model: {
      name: import.meta.env.VITE_MODEL_NAME || defaultConfig.model.name,
      version: import.meta.env.VITE_MODEL_VERSION || defaultConfig.model.version,
      description: import.meta.env.VITE_MODEL_DESCRIPTION || defaultConfig.model.description,
    },
    vectorStore: import.meta.env.VITE_VECTOR_STORE || defaultConfig.vectorStore,
  };
}

// Export singleton config
export const appConfig = loadConfig();

// Helper to get formatted model string
export function getModelDisplayString(config: AppConfig = appConfig): string {
  return `${config.model.name} ${config.model.version} + ${config.vectorStore}`;
}

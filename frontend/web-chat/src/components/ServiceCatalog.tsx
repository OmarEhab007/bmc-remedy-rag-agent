import { useState } from 'react';
import { useTranslation } from 'react-i18next';

interface ServiceCategory {
  id: string;
  icon: string;
  nameEn: string;
  nameAr: string;
  count: number;
  color: string;
}

interface ServiceCatalogProps {
  onCategorySelect: (category: string) => void;
  onServiceSearch: (query: string) => void;
  compact?: boolean;
}

// Damee service categories with their metadata
const SERVICE_CATEGORIES: ServiceCategory[] = [
  {
    id: 'it-services',
    icon: '💻',
    nameEn: 'IT Services',
    nameAr: 'خدمات تقنية المعلومات',
    count: 28,
    color: 'bg-blue-500/10 hover:bg-blue-500/20 border-blue-500/30',
  },
  {
    id: 'support-services',
    icon: '🚗',
    nameEn: 'Support Services',
    nameAr: 'الخدمات المساندة',
    count: 13,
    color: 'bg-green-500/10 hover:bg-green-500/20 border-green-500/30',
  },
  {
    id: 'legal-services',
    icon: '⚖️',
    nameEn: 'Legal Services',
    nameAr: 'الاستشارات القانونية',
    count: 9,
    color: 'bg-purple-500/10 hover:bg-purple-500/20 border-purple-500/30',
  },
  {
    id: 'inspection-services',
    icon: '🔍',
    nameEn: 'Inspection',
    nameAr: 'خدمات التفتيش',
    count: 1,
    color: 'bg-orange-500/10 hover:bg-orange-500/20 border-orange-500/30',
  },
  {
    id: 'geospatial-services',
    icon: '🗺️',
    nameEn: 'Geospatial',
    nameAr: 'الخدمات الجيومكانية',
    count: 5,
    color: 'bg-teal-500/10 hover:bg-teal-500/20 border-teal-500/30',
  },
];

// Popular services for quick access
const POPULAR_SERVICES = [
  { id: '10513', name: 'VPN Request', icon: '🔐' },
  { id: '10247', name: 'Software Installation', icon: '💿' },
  { id: '10101', name: 'Report Issue', icon: '⚠️' },
  { id: '10504', name: 'App Permissions', icon: '🔑' },
];

export function ServiceCatalog({
  onCategorySelect,
  onServiceSearch,
  compact = false
}: ServiceCatalogProps) {
  const { t, i18n } = useTranslation();
  const isRTL = i18n.language === 'ar';
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className={`service-catalog ${compact ? 'compact' : ''}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-foreground/80">
          {t('serviceCatalog.quickServices', 'Quick Services')}
        </h3>
        {!compact && (
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="text-xs text-accent hover:text-accent/80 transition-colors"
          >
            {isExpanded ? t('common.showLess', 'Show less') : t('common.showAll', 'Show all')}
          </button>
        )}
      </div>

      {/* Category Grid */}
      <div className={`grid gap-2 ${compact ? 'grid-cols-2' : 'grid-cols-2 sm:grid-cols-3'}`}>
        {SERVICE_CATEGORIES.slice(0, compact ? 4 : (isExpanded ? undefined : 5)).map((category) => (
          <button
            key={category.id}
            onClick={() => onCategorySelect(category.nameEn)}
            className={`flex items-center gap-2 px-3 py-2 rounded-lg border
                       transition-all duration-200 group text-left
                       ${category.color}`}
            dir={isRTL ? 'rtl' : 'ltr'}
          >
            <span className="text-lg">{category.icon}</span>
            <div className="flex-1 min-w-0">
              <div className="text-xs font-medium text-foreground truncate">
                {isRTL ? category.nameAr : category.nameEn}
              </div>
              <div className="text-[10px] text-foreground/60">
                {category.count} {t('serviceCatalog.services', 'services')}
              </div>
            </div>
          </button>
        ))}
      </div>

      {/* Popular Services - Only show in expanded mode */}
      {!compact && isExpanded && (
        <div className="mt-4">
          <h4 className="text-xs font-medium text-foreground/60 mb-2">
            {t('serviceCatalog.popularServices', 'Popular Services')}
          </h4>
          <div className="flex flex-wrap gap-2">
            {POPULAR_SERVICES.map((service) => (
              <button
                key={service.id}
                onClick={() => onServiceSearch(`Service ${service.id} ${service.name}`)}
                className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-full
                         bg-accent/10 hover:bg-accent/20 border border-accent/20
                         text-xs text-foreground transition-colors"
              >
                <span>{service.icon}</span>
                <span>{service.name}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Quick search prompt */}
      {!compact && (
        <div className="mt-4 pt-3 border-t border-foreground/10">
          <p className="text-xs text-foreground/50 text-center">
            {t('serviceCatalog.helpPrompt', "Can't find what you need? Just describe it!")}
          </p>
        </div>
      )}
    </div>
  );
}

/**
 * Compact version for sidebar
 */
export function ServiceCatalogCompact({ onCategorySelect }: { onCategorySelect: (category: string) => void }) {
  const { t, i18n } = useTranslation();
  const isRTL = i18n.language === 'ar';

  return (
    <div className="p-2 space-y-1">
      <div className="text-xs font-medium text-sidebar-muted px-2 mb-2">
        {t('serviceCatalog.quickServices', 'Quick Services')}
      </div>
      {SERVICE_CATEGORIES.slice(0, 4).map((category) => (
        <button
          key={category.id}
          onClick={() => onCategorySelect(category.nameEn)}
          className="w-full flex items-center gap-2 px-2 py-1.5 rounded-md
                     hover:bg-sidebar-hover text-sidebar text-sm text-left
                     transition-colors"
          dir={isRTL ? 'rtl' : 'ltr'}
        >
          <span>{category.icon}</span>
          <span className="flex-1 truncate">
            {isRTL ? category.nameAr : category.nameEn}
          </span>
          <span className="text-xs text-sidebar-muted">{category.count}</span>
        </button>
      ))}
      <button
        onClick={() => onCategorySelect('all')}
        className="w-full text-xs text-accent hover:text-accent/80 py-1 transition-colors"
      >
        {t('serviceCatalog.viewAll', 'View all services →')}
      </button>
    </div>
  );
}

/**
 * Empty state with service catalog
 */
export function ServiceCatalogEmptyState({ onSendMessage }: { onSendMessage: (message: string) => void }) {
  const { t } = useTranslation();

  const handleCategorySelect = (category: string) => {
    onSendMessage(`Show me services in ${category}`);
  };

  const handleServiceSearch = (query: string) => {
    onSendMessage(`I need ${query}`);
  };

  return (
    <div className="max-w-md mx-auto">
      <div className="text-center mb-6">
        <h2 className="text-xl font-semibold text-foreground mb-2">
          {t('serviceCatalog.welcomeTitle', 'How can I help you today?')}
        </h2>
        <p className="text-sm text-foreground/60">
          {t('serviceCatalog.welcomeSubtitle', 'Select a category or describe what you need')}
        </p>
      </div>

      <ServiceCatalog
        onCategorySelect={handleCategorySelect}
        onServiceSearch={handleServiceSearch}
      />

      {/* Quick actions */}
      <div className="mt-6 grid grid-cols-2 gap-2">
        <button
          onClick={() => onSendMessage('I want to report a technical issue')}
          className="flex items-center gap-2 px-4 py-3 rounded-lg
                     bg-error/10 hover:bg-error/20 border border-error/20
                     text-sm text-foreground transition-colors"
        >
          <span>🚨</span>
          <span>{t('serviceCatalog.reportIssue', 'Report Issue')}</span>
        </button>
        <button
          onClick={() => onSendMessage('Check status of my request')}
          className="flex items-center gap-2 px-4 py-3 rounded-lg
                     bg-accent/10 hover:bg-accent/20 border border-accent/20
                     text-sm text-foreground transition-colors"
        >
          <span>📋</span>
          <span>{t('serviceCatalog.checkStatus', 'Check Status')}</span>
        </button>
      </div>
    </div>
  );
}

import type { ReactNode } from 'react';

interface SkeletonProps {
  className?: string;
  width?: string | number;
  height?: string | number;
}

export function Skeleton({ className = '', width, height }: SkeletonProps) {
  const style: React.CSSProperties = {};
  if (width) style.width = typeof width === 'number' ? `${width}px` : width;
  if (height) style.height = typeof height === 'number' ? `${height}px` : height;

  return (
    <div
      className={`skeleton ${className}`}
      style={style}
      aria-hidden="true"
    />
  );
}

// Pre-built skeleton variants
export function SkeletonText({
  lines = 1,
  className = '',
}: {
  lines?: number;
  className?: string;
}) {
  return (
    <div className={`space-y-2 ${className}`}>
      {Array.from({ length: lines }).map((_, index) => (
        <Skeleton
          key={index}
          className="skeleton-text"
          width={index === lines - 1 && lines > 1 ? '75%' : '100%'}
        />
      ))}
    </div>
  );
}

export function SkeletonAvatar({ size = 30 }: { size?: number }) {
  return (
    <Skeleton
      className="skeleton-avatar"
      width={size}
      height={size}
    />
  );
}

export function SkeletonButton({
  width = 80,
  height = 36,
}: {
  width?: number;
  height?: number;
}) {
  return (
    <Skeleton
      className="skeleton-button"
      width={width}
      height={height}
    />
  );
}

// Message skeleton for chat loading
export function MessageSkeleton({ isUser = false }: { isUser?: boolean }) {
  return (
    <div
      className={`message-row ${isUser ? 'message-row-user' : 'message-row-assistant'}`}
      aria-label="Loading message"
    >
      <SkeletonAvatar />
      <div className="message-content flex-1">
        <SkeletonText lines={isUser ? 1 : 3} />
      </div>
    </div>
  );
}

// Sidebar item skeleton
export function SidebarItemSkeleton() {
  return (
    <div className="sidebar-item flex items-center gap-3 p-3">
      <Skeleton width={16} height={16} className="rounded" />
      <div className="flex-1">
        <Skeleton height={14} width="80%" className="mb-1" />
        <Skeleton height={10} width="50%" />
      </div>
    </div>
  );
}

// Full chat skeleton for initial load
export function ChatSkeleton() {
  return (
    <div className="flex-1 flex flex-col" aria-label="Loading chat">
      {/* Header skeleton */}
      <div className="header-minimal">
        <SkeletonButton width={120} height={32} />
        <div className="flex gap-2">
          <Skeleton width={36} height={36} className="rounded-lg" />
          <Skeleton width={36} height={36} className="rounded-lg" />
          <Skeleton width={32} height={32} className="rounded" />
        </div>
      </div>

      {/* Messages skeleton */}
      <div className="flex-1 p-4 space-y-4">
        <MessageSkeleton isUser />
        <MessageSkeleton />
        <MessageSkeleton isUser />
        <MessageSkeleton />
      </div>

      {/* Input skeleton */}
      <div className="p-4">
        <div className="input-box flex items-center gap-3">
          <Skeleton height={24} className="flex-1 rounded" />
          <Skeleton width={44} height={44} className="rounded-full" />
        </div>
      </div>
    </div>
  );
}

// Conditional skeleton wrapper
interface SkeletonWrapperProps {
  loading: boolean;
  skeleton: ReactNode;
  children: ReactNode;
}

export function SkeletonWrapper({
  loading,
  skeleton,
  children,
}: SkeletonWrapperProps) {
  return loading ? <>{skeleton}</> : <>{children}</>;
}

export default Skeleton;

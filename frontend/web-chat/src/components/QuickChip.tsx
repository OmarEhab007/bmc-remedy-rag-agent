interface QuickChipProps {
  label: string;
  onClick: () => void;
  disabled?: boolean;
}

export function QuickChip({ label, onClick, disabled = false }: QuickChipProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className="quick-chip"
    >
      {label}
    </button>
  );
}

export default QuickChip;

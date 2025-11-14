import React from 'react';

const InterestChip = ({ interest, isSelected, onToggle }) => {
  return (
    <button
      onClick={onToggle}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        padding: '10px 16px',
        background: isSelected
          ? 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)'
          : 'white',
        color: isSelected ? 'white' : '#666',
        border: `2px solid ${isSelected ? '#ff6b35' : '#e0e0e0'}`,
        borderRadius: '24px',
        fontSize: '14px',
        fontWeight: '600',
        cursor: 'pointer',
        transition: 'all 0.2s',
        boxShadow: isSelected ? '0 4px 12px rgba(255, 107, 53, 0.3)' : 'none'
      }}
      onMouseEnter={(e) => {
        if (!isSelected) {
          e.currentTarget.style.borderColor = '#ff6b35';
          e.currentTarget.style.color = '#ff6b35';
          e.currentTarget.style.background = '#fff5eb';
        }
      }}
      onMouseLeave={(e) => {
        if (!isSelected) {
          e.currentTarget.style.borderColor = '#e0e0e0';
          e.currentTarget.style.color = '#666';
          e.currentTarget.style.background = 'white';
        }
      }}
    >
      <span style={{ fontSize: '18px' }}>{interest.subcategoryIcon}</span>
      <span>{interest.subcategoryName}</span>
    </button>
  );
};

export default InterestChip;
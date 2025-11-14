import React, { useState } from 'react';
import { Plus, Trash2, ChevronDown, ExternalLink } from 'lucide-react';

const VideoTile = ({ video, isInLibrary, isRecentlyAdded, currentStatus, onAddToLibrary, onChangeStatus, onDelete }) => {
  const [showStatusMenu, setShowStatusMenu] = useState(false);

  const statusOptions = [
    { value: 'WATCH_LATER', label: 'Watch Later', emoji: '📌' },
    { value: 'CURRENTLY_WATCHING', label: 'Currently Watching', emoji: '▶️' },
    { value: 'FINISHED', label: 'Finished', emoji: '✅' }
  ];

  const getYouTubeUrl = (videoId) => {
    return `https://www.youtube.com/watch?v=${videoId}`;
  };

  const truncateText = (text, maxLength) => {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  };

  const handleWatchClick = (e) => {
    // If video is in "Watch Later" status, automatically move to "Currently Watching"
    if (isInLibrary && currentStatus === 'WATCH_LATER' && onChangeStatus) {
      onChangeStatus('CURRENTLY_WATCHING');
    }
  };

  return (
    <div
      style={{
        background: 'white',
        borderRadius: '12px',
        overflow: 'hidden',
        border: '1px solid #e5e5e5',
        boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
        transition: 'transform 0.2s, box-shadow 0.2s, opacity 0.3s',
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        opacity: isInLibrary && !onChangeStatus ? 0.6 : 1,
        position: 'relative'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-4px)';
        e.currentTarget.style.boxShadow = '0 8px 24px rgba(0,0,0,0.12)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.boxShadow = '0 2px 12px rgba(0,0,0,0.08)';
      }}
    >
      <div style={{ position: 'relative', paddingTop: '56.25%', background: '#f0f0f0' }}>
        <img
          src={video.thumbnailUrl}
          alt={video.title}
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            objectFit: 'cover'
          }}
        />
        {video.matchedInterest && (
          <div style={{
            position: 'absolute',
            top: '8px',
            left: '8px',
            background: 'rgba(255, 107, 53, 0.95)',
            color: 'white',
            padding: '4px 12px',
            borderRadius: '20px',
            fontSize: '12px',
            fontWeight: '500'
          }}>
            {video.matchedInterest}
          </div>
        )}
        {isInLibrary && !onChangeStatus && (
          <div style={{
            position: 'absolute',
            top: '8px',
            right: '8px',
            background: 'rgba(76, 175, 80, 0.95)',
            color: 'white',
            padding: '4px 12px',
            borderRadius: '20px',
            fontSize: '12px',
            fontWeight: '500'
          }}>
            In Library
          </div>
        )}
        {isRecentlyAdded && (
          <div style={{
            position: 'absolute',
            top: '8px',
            right: '8px',
            background: 'rgba(255, 193, 7, 0.95)',
            color: 'white',
            padding: '4px 12px',
            borderRadius: '20px',
            fontSize: '12px',
            fontWeight: '500',
            animation: 'fadeIn 0.3s ease-in'
          }}>
            ✨ Just Added!
          </div>
        )}
      </div>

      <div style={{ padding: '16px', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <h3 style={{
          margin: '0 0 8px 0',
          fontSize: '16px',
          fontWeight: '600',
          color: '#333',
          lineHeight: '1.4',
          minHeight: '44px'
        }}>
          {truncateText(video.title, 70)}
        </h3>

        <p style={{
          margin: '0 0 12px 0',
          fontSize: '14px',
          color: '#666',
          lineHeight: '1.4'
        }}>
          {video.channelTitle}
        </p>

        {video.description && (
          <p style={{
            margin: '0 0 16px 0',
            fontSize: '13px',
            color: '#999',
            lineHeight: '1.5',
            flex: 1
          }}>
            {truncateText(video.description, 100)}
          </p>
        )}

        <div style={{ display: 'flex', gap: '8px', marginTop: 'auto' }}>

          <a
            href={getYouTubeUrl(video.videoId)}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              padding: '10px',
              background: '#fff',
              border: '2px solid #ff6b35',
              borderRadius: '8px',
              color: '#ff6b35',
              fontSize: '14px',
              fontWeight: '600',
              textDecoration: 'none',
              cursor: 'pointer',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#ff6b35';
              e.currentTarget.style.color = 'white';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = '#fff';
              e.currentTarget.style.color = '#ff6b35';
            }}
            onClick={handleWatchClick}
          >
            <ExternalLink size={16} />
            Watch
          </a>

          {!isInLibrary && (
            <button
              onClick={() => onAddToLibrary(video)}
              style={{
                flex: 1,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                padding: '10px',
                background: 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
                border: 'none',
                borderRadius: '8px',
                color: 'white',
                fontSize: '14px',
                fontWeight: '600',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.02)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
                e.currentTarget.style.boxShadow = 'none';
              }}
            >
              <Plus size={16} />
              Add
            </button>
          )}

          {isInLibrary && onChangeStatus && onDelete && (
            <React.Fragment>
              <div style={{ position: 'relative', flex: 1 }}>
                <button
                  onClick={() => setShowStatusMenu(!showStatusMenu)}
                  style={{
                    width: '100%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    padding: '10px',
                    background: '#fff',
                    border: '2px solid #4CAF50',
                    borderRadius: '8px',
                    color: '#4CAF50',
                    fontSize: '14px',
                    fontWeight: '600',
                    cursor: 'pointer',
                    transition: 'all 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.background = '#4CAF50';
                    e.currentTarget.style.color = 'white';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.background = '#fff';
                    e.currentTarget.style.color = '#4CAF50';
                  }}
                >
                  Move
                  <ChevronDown size={16} />
                </button>

                {showStatusMenu && (
                  <div style={{
                    position: 'absolute',
                    bottom: '100%',
                    left: 0,
                    right: 0,
                    marginBottom: '8px',
                    background: 'white',
                    borderRadius: '8px',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
                    zIndex: 10,
                    overflow: 'hidden'
                  }}>
                    {statusOptions
                      .filter(option => option.value !== currentStatus)
                      .map((option) => (
                        <button
                          key={option.value}
                          onClick={() => {
                            onChangeStatus(option.value);
                            setShowStatusMenu(false);
                          }}
                          style={{
                            width: '100%',
                            padding: '12px',
                            background: 'white',
                            border: 'none',
                            textAlign: 'left',
                            fontSize: '14px',
                            color: '#333',
                            cursor: 'pointer',
                            transition: 'background 0.2s',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '8px'
                          }}
                          onMouseEnter={(e) => e.currentTarget.style.background = '#f5f5f5'}
                          onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                        >
                          <span>{option.emoji}</span>
                          <span>{option.label}</span>
                        </button>
                      ))}
                  </div>
                )}
              </div>

              <button
                onClick={onDelete}
                style={{
                  padding: '10px',
                  background: '#fff',
                  border: '2px solid #dc3545',
                  borderRadius: '8px',
                  color: '#dc3545',
                  cursor: 'pointer',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#dc3545';
                  e.currentTarget.style.color = 'white';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = '#fff';
                  e.currentTarget.style.color = '#dc3545';
                }}
              >
                <Trash2 size={16} />
              </button>
            </React.Fragment>
          )}
        </div>
      </div>
    </div>
  );
};

export default VideoTile;


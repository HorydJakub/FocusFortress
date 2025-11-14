import React, { useEffect, useState } from 'react';
import api from '../../services/api';
import { Image, RefreshCw, BookmarkPlus, Play, CheckCircle } from 'lucide-react';
import VideoTile from './VideoTile';
import InterestChip from './InterestChip';

const MyMedia = () => {
  const [library, setLibrary] = useState({
    WATCH_LATER: [],
    CURRENTLY_WATCHING: [],
    FINISHED: []
  });
  const [loadingLibrary, setLoadingLibrary] = useState(true);
  const [recommendations, setRecommendations] = useState([]);
  const [interests, setInterests] = useState([]);
  const [selectedInterests, setSelectedInterests] = useState(new Set());
  const [loadingRecommendations, setLoadingRecommendations] = useState(false);
  const [recentlyAddedVideos, setRecentlyAddedVideos] = useState(new Set());
  const [error, setError] = useState(null);
  const [refreshCount, setRefreshCount] = useState(0);
  const [refreshLimitReached, setRefreshLimitReached] = useState(false);
  const MAX_DAILY_REFRESHES = 3;

  // Cache key for localStorage
  const RECOMMENDATIONS_CACHE_KEY = 'focusfortress_recommendations_cache';
  const REFRESH_COUNT_KEY = 'focusfortress_refresh_count';
  const REFRESH_DATE_KEY = 'focusfortress_refresh_date';
  const INITIAL_FETCH_FLAG_KEY = 'focusfortress_initial_tiles_fetched'; // Track if we've already fetched initial tiles

  useEffect(() => {
    fetchLibrary();
    fetchInterests();
    loadCachedRecommendations();
    loadRefreshCount();
  }, []);

  const getTodayDateString = () => {
    return new Date().toISOString().split('T')[0];
  };

  const loadRefreshCount = () => {
    const storedDate = localStorage.getItem(REFRESH_DATE_KEY);
    const today = getTodayDateString();

    // Reset counter if it's a new day
    if (storedDate !== today) {
      localStorage.setItem(REFRESH_COUNT_KEY, '0');
      localStorage.setItem(REFRESH_DATE_KEY, today);
      setRefreshCount(0);
      setRefreshLimitReached(false);
    } else {
      const count = parseInt(localStorage.getItem(REFRESH_COUNT_KEY) || '0', 10);
      setRefreshCount(count);
      setRefreshLimitReached(count >= MAX_DAILY_REFRESHES);
    }
  };

  const loadCachedRecommendations = () => {
    try {
      const cached = localStorage.getItem(RECOMMENDATIONS_CACHE_KEY);
      if (cached) {
        const parsed = JSON.parse(cached);
        setRecommendations(parsed);
        setLoadingRecommendations(false);
      } else {
        // Check if we've already attempted initial fetch
        const hasAttemptedInitialFetch = localStorage.getItem(INITIAL_FETCH_FLAG_KEY);

        if (!hasAttemptedInitialFetch) {
          // First login after account creation - auto-load recommendations from YouTube API
          fetchRecommendationsOnFirstLogin();
        } else {
          // Already fetched before but cache was cleared - don't fetch again automatically
          setLoadingRecommendations(false);
        }
      }
    } catch (e) {
      console.error('Failed to load cached recommendations:', e);
      const hasAttemptedInitialFetch = localStorage.getItem(INITIAL_FETCH_FLAG_KEY);
      if (!hasAttemptedInitialFetch) {
        fetchRecommendationsOnFirstLogin();
      } else {
        setLoadingRecommendations(false);
      }
    }
  };

  const fetchRecommendationsOnFirstLogin = async () => {
    setLoadingRecommendations(true);
    try {
      const res = await api.get('/media/tiles?limit=12');
      const recs = res.data || [];
      setRecommendations(recs);
      saveCachedRecommendations(recs);
    } catch (e) {
      console.error('Failed to fetch initial recommendations:', e);
    } finally {
      // Mark that we've completed the initial fetch (even if failed)
      localStorage.setItem(INITIAL_FETCH_FLAG_KEY, 'true');
      setLoadingRecommendations(false);
    }
  };

  const saveCachedRecommendations = (recs) => {
    try {
      localStorage.setItem(RECOMMENDATIONS_CACHE_KEY, JSON.stringify(recs));
    } catch (e) {
      console.error('Failed to cache recommendations:', e);
    }
  };

  const incrementRefreshCount = () => {
    const today = getTodayDateString();
    const storedDate = localStorage.getItem(REFRESH_DATE_KEY);

    let newCount;
    if (storedDate !== today) {
      newCount = 1;
      localStorage.setItem(REFRESH_DATE_KEY, today);
    } else {
      newCount = (parseInt(localStorage.getItem(REFRESH_COUNT_KEY) || '0', 10)) + 1;
    }

    localStorage.setItem(REFRESH_COUNT_KEY, newCount.toString());
    setRefreshCount(newCount);
    setRefreshLimitReached(newCount >= MAX_DAILY_REFRESHES);
  };

  const fetchLibrary = async () => {
    setLoadingLibrary(true);
    try {
      const res = await api.get('/media/library');
      const items = res.data || [];

      const grouped = {
        WATCH_LATER: items.filter(item => item.status === 'WATCH_LATER'),
        CURRENTLY_WATCHING: items.filter(item => item.status === 'CURRENTLY_WATCHING'),
        FINISHED: items.filter(item => item.status === 'FINISHED')
      };

      setLibrary(grouped);
      setError(null);
    } catch (e) {
      console.error('Failed to fetch library:', e);
      setError('Failed to load your media library');
    } finally {
      setLoadingLibrary(false);
    }
  };

  const fetchInterests = async () => {
    try {
      const res = await api.get('/user/interests');
      const userInterests = res.data || [];
      setInterests(userInterests);
      // Store subcategory NAMES instead of IDs, since backend expects names
      setSelectedInterests(new Set(userInterests.map(i => i.subcategoryName)));
    } catch (e) {
      console.error('Failed to fetch interests:', e);
    }
  };

  const handleAddToLibrary = async (video, status = 'WATCH_LATER') => {
    try {
      await api.post('/media/library', {
        videoId: video.videoId,
        title: video.title,
        channelTitle: video.channelTitle,
        thumbnailUrl: video.thumbnailUrl,
        description: video.description,
        matchedInterest: video.matchedInterest,
        status: status
      });

      setRecentlyAddedVideos(prev => new Set([...prev, video.videoId]));

      setTimeout(() => {
        setRecentlyAddedVideos(prev => {
          const updated = new Set(prev);
          updated.delete(video.videoId);
          return updated;
        });
      }, 3000);

      await fetchLibrary();
    } catch (e) {
      console.error('Failed to add to library:', e);
      if (e.response?.data?.message) {
        alert(e.response.data.message);
      } else {
        alert('Failed to add video to library');
      }
    }
  };

  const handleChangeStatus = async (itemId, newStatus) => {
    try {
      await api.patch(`/media/library/${itemId}/status`, {
        status: newStatus
      });
      await fetchLibrary();
    } catch (e) {
      console.error('Failed to change status:', e);
      alert('Failed to update video status');
    }
  };

  const handleDeleteFromLibrary = async (itemId) => {
    if (!window.confirm('Remove this video from your library?')) {
      return;
    }

    try {
      await api.delete(`/media/library/${itemId}`);
      await fetchLibrary();
    } catch (e) {
      console.error('Failed to delete from library:', e);
      alert('Failed to remove video');
    }
  };

  const handleToggleInterest = (interestId) => {
    // Find the interest object by ID to get its subcategoryName
    const interest = interests.find(i => i.id === interestId);
    if (!interest) return;

    setSelectedInterests(prev => {
      const newSet = new Set(prev);
      const subcategoryName = interest.subcategoryName;

      if (newSet.has(subcategoryName)) {
        newSet.delete(subcategoryName);
      } else {
        newSet.add(subcategoryName);
      }
      return newSet;
    });
  };

  const handleRefreshContent = async () => {
    if (refreshLimitReached) {
      alert('Daily refresh limit reached. Please try again tomorrow.');
      return;
    }

    setLoadingRecommendations(true);
    incrementRefreshCount();

    try {
      const subcategoryNames = Array.from(selectedInterests);

      if (subcategoryNames.length === 0) {
        alert('Please select at least one interest');
        setLoadingRecommendations(false);
        return;
      }

      // Call the new custom tiles endpoint with selected interests
      const params = new URLSearchParams();
      subcategoryNames.forEach(name => params.append('subcategories', name));
      params.append('limit', '12');

      const res = await api.get(`/media/tiles/custom?${params.toString()}`);
      setRecommendations(res.data || []);
      saveCachedRecommendations(res.data || []);

    } catch (e) {
      console.error('Failed to refresh content:', e);
      alert('Failed to refresh recommendations');
    } finally {
      setLoadingRecommendations(false);
    }
  };

  const isVideoInLibrary = (videoId) => {
    return Object.values(library).flat().some(item => item.videoId === videoId);
  };

  const isRecentlyAdded = (videoId) => {
    return recentlyAddedVideos.has(videoId);
  };

  const getTotalLibraryCount = () => {
    return Object.values(library).flat().length;
  };

  const sortedRecommendations = [...recommendations].sort((a, b) => {
    const aInLib = isVideoInLibrary(a.videoId);
    const bInLib = isVideoInLibrary(b.videoId);
    if (aInLib === bInLib) return 0;
    return aInLib ? 1 : -1;
  });

  const renderLibrarySection = (status, title, icon, emptyMessage) => {
    const items = library[status] || [];
    const Icon = icon;

    return (
      <div style={{ marginBottom: '40px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
          <Icon size={24} color="#ff6b35" />
          <h2 style={{ margin: 0, fontSize: '24px', color: '#333' }}>
            {title} ({items.length})
          </h2>
        </div>

        {items.length === 0 ? (
          <div style={{
            padding: '40px',
            textAlign: 'center',
            background: 'white',
            borderRadius: '12px',
            border: '2px dashed #e0e0e0'
          }}>
            <p style={{ margin: 0, color: '#999', fontSize: '15px' }}>{emptyMessage}</p>
          </div>
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
            gap: '20px'
          }}>
            {items.map((item) => (
              <VideoTile
                key={item.id}
                video={item}
                isInLibrary={true}
                isRecentlyAdded={false}
                currentStatus={item.status}
                onChangeStatus={(newStatus) => handleChangeStatus(item.id, newStatus)}
                onDelete={() => handleDeleteFromLibrary(item.id)}
              />
            ))}
          </div>
        )}
      </div>
    );
  };

  if (loadingLibrary) {
    return (
      <div style={{ padding: '80px 40px', maxWidth: '1400px', margin: '0 auto', textAlign: 'center' }}>
        <div style={{ fontSize: '18px', color: '#666' }}>Loading your media...</div>
      </div>
    );
  }

  return (
    <div style={{ padding: '80px 40px', maxWidth: '1400px', margin: '0 auto' }}>
      <div style={{ marginBottom: '40px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '8px' }}>
          <Image size={36} color="#ff6b35" />
          <h1 style={{ margin: 0, color: '#ff6b35', fontSize: '36px' }}>My Media</h1>
        </div>
        <p style={{ margin: 0, color: '#666', fontSize: '16px' }}>
          Your personalized learning library
        </p>
      </div>

      {error && (
        <div style={{
          padding: '16px',
          background: '#ffe8d6',
          border: '2px solid #ff6b35',
          borderRadius: '12px',
          marginBottom: '32px',
          color: '#ff6b35',
          fontWeight: '600'
        }}>
          {error}
        </div>
      )}

      <div style={{
        background: 'white',
        borderRadius: '20px',
        padding: '40px',
        marginBottom: '48px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
      }}>
        <h2 style={{
          fontSize: '28px',
          color: '#333',
          marginBottom: '32px',
          fontWeight: 'bold'
        }}>
          📚 My Library
        </h2>

        {getTotalLibraryCount() === 0 ? (
          <div style={{
            padding: '60px 40px',
            textAlign: 'center',
            background: '#f8f8f8',
            borderRadius: '16px'
          }}>
            <Image size={64} color="#ff6b35" style={{ opacity: 0.3, marginBottom: '16px' }} />
            <h3 style={{ color: '#333', marginBottom: '8px' }}>It's a bit empty here...</h3>
            <p style={{ margin: '0 0 24px 0', color: '#666' }}>
              Discover educational content below and add videos to your library!
            </p>
          </div>
        ) : (
          <>
            {renderLibrarySection('WATCH_LATER', 'Watch Later', BookmarkPlus, 'No videos saved for later')}
            {renderLibrarySection('CURRENTLY_WATCHING', 'Currently Watching', Play, 'Not watching anything right now')}
            {renderLibrarySection('FINISHED', 'Finished', CheckCircle, 'No completed videos yet')}
          </>
        )}
      </div>

      <div style={{
        background: 'white',
        borderRadius: '20px',
        padding: '40px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
      }}>
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
          flexWrap: 'wrap',
          gap: '16px'
        }}>
          <div>
            <h2 style={{
              fontSize: '28px',
              color: '#333',
              margin: '0 0 8px 0',
              fontWeight: 'bold'
            }}>
              🎯 Discover Education Media
            </h2>
            <p style={{
              fontSize: '13px',
              color: refreshLimitReached ? '#dc3545' : '#999',
              margin: 0,
              fontWeight: '500'
            }}>
              Refreshes today: {refreshCount}/{MAX_DAILY_REFRESHES} {refreshLimitReached && '(Limit reached)'}
            </p>
          </div>

          <button
            onClick={handleRefreshContent}
            disabled={loadingRecommendations || refreshLimitReached}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '12px 24px',
              background: (loadingRecommendations || refreshLimitReached)
                ? '#e0e0e0'
                : 'linear-gradient(135deg, #ff8c42 0%, #ff6b35 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '12px',
              fontSize: '16px',
              fontWeight: 'bold',
              cursor: (loadingRecommendations || refreshLimitReached) ? 'not-allowed' : 'pointer',
              boxShadow: '0 4px 12px rgba(255, 107, 53, 0.3)',
              transition: 'transform 0.2s, box-shadow 0.2s',
              opacity: (loadingRecommendations || refreshLimitReached) ? 0.7 : 1
            }}
            onMouseEnter={(e) => {
              if (!loadingRecommendations && !refreshLimitReached) {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 6px 16px rgba(255, 107, 53, 0.4)';
              }
            }}
            onMouseLeave={(e) => {
              if (!loadingRecommendations && !refreshLimitReached) {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(255, 107, 53, 0.3)';
              }
            }}
            title={refreshLimitReached ? 'Daily refresh limit reached. Try again tomorrow.' : 'Refresh to get new recommendations'}
          >
            <RefreshCw size={20} style={{
              animation: loadingRecommendations ? 'spin 1s linear infinite' : 'none'
            }} />
            Refresh Content
          </button>
        </div>

        <div style={{ marginBottom: '32px' }}>
          <p style={{ fontSize: '15px', color: '#666', marginBottom: '16px', fontWeight: '600' }}>
            Filter by interests:
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', marginBottom: '12px' }}>
            {interests.map((interest) => (
              <InterestChip
                key={interest.id}
                interest={interest}
                isSelected={selectedInterests.has(interest.subcategoryName)}
                onToggle={() => handleToggleInterest(interest.id)}
              />
            ))}
          </div>
          <p style={{ fontSize: '12px', color: '#999', margin: 0, fontStyle: 'italic' }}>
            * Manage interests in Profile Settings
          </p>
        </div>

        {loadingRecommendations ? (
          <div style={{ textAlign: 'center', padding: '60px', color: '#666' }}>
            Loading recommendations...
          </div>
        ) : recommendations.length === 0 ? (
          <div style={{
            padding: '60px 40px',
            textAlign: 'center',
            background: '#f8f8f8',
            borderRadius: '16px'
          }}>
            <p style={{ margin: 0, color: '#999' }}>
              No recommendations available. Try selecting different interests!
            </p>
          </div>
        ) : (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
            gap: '20px'
          }}>
            {sortedRecommendations.map((video) => (
              <VideoTile
                key={video.videoId}
                video={video}
                isInLibrary={isVideoInLibrary(video.videoId)}
                isRecentlyAdded={isRecentlyAdded(video.videoId)}
                onAddToLibrary={handleAddToLibrary}
              />
            ))}
          </div>
        )}
      </div>

      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
};

export default MyMedia;


import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Flag } from 'lucide-react';

const FeatureFlags = () => {
  const [flags, setFlags] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchFlags = async () => {
    try {
      setLoading(true);
      const res = await axios.get('http://localhost:8080/api/admin/flags');
      setFlags(res.data);
    } catch (error) {
      console.error('Error fetching feature flags', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFlags();
  }, []);

  const toggleFlag = async (name, currentStatus) => {
    try {
      await axios.put(`http://localhost:8080/api/admin/flags/${name}`, { enabled: !currentStatus });
      // Update local state to reflect change instantly
      setFlags(flags.map(f => f.name === name ? { ...f, enabled: !currentStatus } : f));
    } catch (error) {
      console.error('Error toggling feature flag', error);
      alert('Failed to toggle flag');
    }
  };

  return (
    <div>
      <h1 className="page-title">Feature Flags</h1>
      
      <div className="glass-panel" style={{ padding: '2rem' }}>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
          Instantly enable or disable features across the entire platform without deploying code.
        </p>

        {loading ? (
          <p>Loading flags...</p>
        ) : flags.length === 0 ? (
          <p>No feature flags found in the database.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {flags.map(flag => (
              <div 
                key={flag.id} 
                style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'space-between',
                  padding: '1.5rem',
                  background: 'rgba(0,0,0,0.2)',
                  borderRadius: '12px',
                  border: '1px solid var(--panel-border)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem' }}>
                  <div style={{ padding: '0.75rem', background: 'rgba(99, 102, 241, 0.1)', color: 'var(--accent-color)', borderRadius: '8px' }}>
                    <Flag size={24} />
                  </div>
                  <div>
                    <h3 style={{ margin: '0 0 0.25rem 0', fontSize: '1.1rem', color: 'white' }}>{flag.name}</h3>
                    <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{flag.description || 'No description provided.'}</p>
                  </div>
                </div>

                <button 
                  onClick={() => toggleFlag(flag.name, flag.enabled)}
                  style={{
                    padding: '0.5rem 1.5rem',
                    borderRadius: '999px',
                    border: 'none',
                    fontWeight: 'bold',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    background: flag.enabled ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                    color: flag.enabled ? '#10b981' : '#ef4444',
                    minWidth: '100px'
                  }}
                >
                  {flag.enabled ? 'ENABLED' : 'DISABLED'}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default FeatureFlags;

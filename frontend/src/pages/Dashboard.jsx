import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { MousePointerClick, Link as LinkIcon } from 'lucide-react';

const Dashboard = () => {
  const [stats, setStats] = useState({ totalSystemClicks: 0, totalUrls: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const clicksRes = await axios.get('/api/stats/total');
        const urlsRes = await axios.get('/api/admin/urls?size=1');
        
        setStats({
          totalSystemClicks: clicksRes.data.totalSystemClicks || 0,
          totalUrls: urlsRes.data.totalElements || 0
        });
      } catch (error) {
        console.error('Error fetching stats', error);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  return (
    <div>
      <h1 className="page-title">Analytics Overview</h1>
      
      {loading ? (
        <p>Loading analytics...</p>
      ) : (
        <div className="kpi-grid">
          <div className="glass-panel kpi-card">
            <div className="kpi-icon">
              <MousePointerClick size={24} />
            </div>
            <div className="kpi-info">
              <h3>Total Global Clicks</h3>
              <p>{stats.totalSystemClicks.toLocaleString()}</p>
            </div>
          </div>
          
          <div className="glass-panel kpi-card">
            <div className="kpi-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: '#10b981' }}>
              <LinkIcon size={24} />
            </div>
            <div className="kpi-info">
              <h3>Active URLs</h3>
              <p>{stats.totalUrls.toLocaleString()}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;

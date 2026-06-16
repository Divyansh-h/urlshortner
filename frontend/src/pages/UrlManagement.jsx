import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Trash2, ExternalLink } from 'lucide-react';

const UrlManagement = () => {
  const [urls, setUrls] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchUrls = async () => {
    try {
      setLoading(true);
      const res = await axios.get(`http://localhost:8080/api/admin/urls?page=${page}&size=10`);
      setUrls(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (error) {
      console.error('Error fetching URLs', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUrls();
  }, [page]);

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this URL? This action cannot be undone.')) {
      try {
        await axios.delete(`http://localhost:8080/api/admin/urls/${id}`);
        fetchUrls(); // Refresh list
      } catch (error) {
        console.error('Error deleting URL', error);
        alert('Failed to delete URL');
      }
    }
  };

  const isExpired = (expiryTime) => {
    if (!expiryTime) return false;
    return new Date(expiryTime) < new Date();
  };

  return (
    <div>
      <h1 className="page-title">URL Management</h1>
      
      <div className="glass-panel" style={{ padding: '1rem' }}>
        {loading ? (
          <p style={{ padding: '1rem' }}>Loading URLs...</p>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Short Code</th>
                  <th>Original Destination</th>
                  <th>Clicks</th>
                  <th>Status</th>
                  <th>Created At</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {urls.length === 0 ? (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center' }}>No URLs found.</td>
                  </tr>
                ) : (
                  urls.map(url => (
                    <tr key={url.id}>
                      <td>
                        <a href={`http://localhost:8080/${url.shortCode}`} target="_blank" rel="noreferrer" className="link-text" style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          {url.shortCode} <ExternalLink size={12} />
                        </a>
                      </td>
                      <td style={{ maxWidth: '300px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {url.originalUrl}
                      </td>
                      <td>{url.clickCount}</td>
                      <td>
                        {isExpired(url.expiryTime) ? (
                          <span className="badge badge-expired">Expired</span>
                        ) : (
                          <span className="badge badge-active">Active</span>
                        )}
                      </td>
                      <td>{new Date(url.createdAt).toLocaleDateString()}</td>
                      <td>
                        <button 
                          className="btn btn-danger" 
                          onClick={() => handleDelete(url.id)}
                          title="Delete URL"
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
        
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem', padding: '0 1rem' }}>
            <button 
              className="btn" 
              style={{ background: 'rgba(255,255,255,0.1)', color: 'white' }}
              disabled={page === 0} 
              onClick={() => setPage(p => p - 1)}
            >
              Previous
            </button>
            <span style={{ display: 'flex', alignItems: 'center', fontSize: '0.875rem' }}>
              Page {page + 1} of {totalPages}
            </span>
            <button 
              className="btn" 
              style={{ background: 'rgba(255,255,255,0.1)', color: 'white' }}
              disabled={page >= totalPages - 1} 
              onClick={() => setPage(p => p + 1)}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default UrlManagement;

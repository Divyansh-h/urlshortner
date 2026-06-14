import { useState } from 'react';
import { urlService } from './api/urlService';
import './App.css';

function App() {
  const [originalUrl, setOriginalUrl] = useState('');
  const [shortUrl, setShortUrl] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleShorten = async (e) => {
    e.preventDefault();
    setError('');
    setShortUrl('');

    if (!originalUrl) {
      setError('Please enter a valid URL.');
      return;
    }

    try {
      setIsLoading(true);
      
      // Call the API service layer instead of inline Axios
      const data = await urlService.shortenUrl(originalUrl);
      
      setShortUrl(data.shortUrl);
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else {
        setError('Failed to shorten the URL. Please try again later.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container">
      <div className="card">
        <h1>URL Shortener</h1>
        <p>Paste your long URL below to generate a short, shareable link.</p>
        
        <form onSubmit={handleShorten} className="form-group">
          <input
            type="text"
            placeholder="Enter long link here (e.g., https://example.com/very/long/path)"
            value={originalUrl}
            onChange={(e) => setOriginalUrl(e.target.value)}
            disabled={isLoading}
            className="url-input"
          />
          <button type="submit" disabled={isLoading} className="submit-btn">
            {isLoading ? 'Shortening...' : 'Shorten'}
          </button>
        </form>

        {error && <div className="error-message">{error}</div>}

        {shortUrl && (
          <div className="result-container">
            <h3>Your Short URL:</h3>
            <div className="short-url-box">
              <a href={shortUrl} target="_blank" rel="noopener noreferrer">
                {shortUrl}
              </a>
              <button 
                onClick={() => navigator.clipboard.writeText(shortUrl)}
                className="copy-btn"
                title="Copy to clipboard"
              >
                📋 Copy
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
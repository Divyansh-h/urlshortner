import axios from 'axios';

// Create an Axios instance with base configuration
const apiClient = axios.create({
    baseURL: '/api', // Proxied by Vite in development
    headers: {
        'Content-Type': 'application/json',
    },
});

export const urlService = {
    /**
     * Sends a request to shorten a URL.
     * @param {string} originalUrl The long URL to shorten.
     * @returns {Promise<Object>} The response data containing the short URL.
     */
    shortenUrl: async (originalUrl) => {
        // Ensure the URL has a protocol
        let urlToSubmit = originalUrl;
        if (!/^https?:\/\//i.test(urlToSubmit)) {
            urlToSubmit = 'http://' + urlToSubmit;
        }

        const response = await apiClient.post('/shorten', {
            originalUrl: urlToSubmit
        });
        
        return response.data;
    }
};
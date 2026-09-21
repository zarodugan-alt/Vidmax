import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { DownloadProvider } from './context/DownloadContext.jsx'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <DownloadProvider>
      <App />
    </DownloadProvider>
  </React.StrictMode>,
)

import { createContext, useContext, useEffect, useState, useRef, useCallback } from 'react'
import { parseSizeToMB } from '../engine/downloadEngine'

const DownloadContext = createContext(null)
export const useDownloads = () => useContext(DownloadContext)

const STORAGE_KEY = 'vidmax_downloads_v2'

function loadInitial(){
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if(raw) return JSON.parse(raw)
  } catch {}
  // seed with 2 completed for demo empty state richness
  return [
    {
      id: 'seed1',
      url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      title: 'Sunset Timelapse 4K — Maldives Drone Footage',
      thumbnail: 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=640&h=360&fit=crop',
      platform: 'youtube',
      platformName: 'YouTube',
      duration: '3:42',
      quality: '1080p',
      format: 'mp4',
      size: '420 MB',
      progress: 100,
      status: 'completed',
      speed: 0,
      eta: '—',
      createdAt: Date.now() - 1000*60*60*24,
      completedAt: Date.now() - 1000*60*60*2,
    },
    {
      id: 'seed2',
      url: 'https://www.tiktok.com/@creator/video/123',
      title: 'Mix — Best of Deep House 2024 | Chill Mix',
      thumbnail: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=640&h=360&fit=crop',
      platform: 'tiktok',
      platformName: 'TikTok',
      duration: '0:47',
      quality: '720p',
      format: 'mp4',
      size: '18 MB',
      progress: 100,
      status: 'completed',
      speed: 0,
      eta: '—',
      createdAt: Date.now() - 1000*60*60*5,
      completedAt: Date.now() - 1000*60*60*1,
    },
  ]
}

export function DownloadProvider({ children }){
  const [downloads, setDownloads] = useState(loadInitial)
  const [toasts, setToasts] = useState([])
  const intervals = useRef(new Map())

  // persist
  useEffect(()=>{
    localStorage.setItem(STORAGE_KEY, JSON.stringify(downloads))
  }, [downloads])

  const pushToast = useCallback((msg, type='info')=>{
    const id = Math.random().toString(36).slice(2,9)
    setToasts(t => [...t, {id, msg, type}])
    setTimeout(()=> setToasts(t => t.filter(x=>x.id!==id)), 3000)
  },[])

  const addDownload = useCallback((meta, selectedQuality) => {
    const fmt = meta.formats.find(f=>f.id===selectedQuality) || meta.formats.find(f=>f.recommended) || meta.formats[3]
    const item = {
      id: Math.random().toString(36).slice(2,9),
      url: meta.url,
      title: meta.title,
      thumbnail: meta.thumbnail,
      platform: meta.platform,
      platformName: meta.platformName,
      duration: meta.duration,
      uploader: meta.uploader,
      quality: fmt.id,
      format: fmt.ext,
      size: fmt.size,
      progress: 0,
      status: 'queued',
      speed: 0,
      eta: 'Calculating…',
      createdAt: Date.now(),
    }
    setDownloads(d => [item, ...d])
    pushToast(`Added to queue — ${fmt.label}`, 'success')

    // auto start after short queue delay if not too many active
    setTimeout(()=> startDownload(item.id), 600)
    return item.id
  }, [pushToast])

  const updateDownload = useCallback((id, patch)=>{
    setDownloads(d => d.map(x=> x.id===id ? {...x, ...patch} : x))
  }, [])

  const startDownload = useCallback((id)=>{
    setDownloads(d => d.map(x=> {
      if(x.id!==id) return x
      if(x.status==='downloading') return x
      return {...x, status:'downloading'}
    }))

    // clear existing interval if any
    if(intervals.current.has(id)){
      clearInterval(intervals.current.get(id))
    }
    const interval = setInterval(()=>{
      setDownloads(curr => curr.map(item=>{
        if(item.id!==id) return item
        if(item.status!=='downloading') return item
        // increment progress with variable speed
        const totalMB = parseSizeToMB(item.size)
        // simulate speed 2-8 MB/s
        const speed = 1.5 + Math.random()*4.5
        const chunkMB = speed * 0.3 // because interval 300ms
        const chunkPercent = (chunkMB / totalMB) * 100
        let next = item.progress + chunkPercent + Math.random()*0.6
        if(next >= 100){
          next = 100
          clearInterval(interval)
          intervals.current.delete(id)
          // success toast delayed
          setTimeout(()=> pushToast(`Download complete — ${item.title.slice(0,28)}…`, 'success'), 100)
          return {
            ...item,
            progress: 100,
            status: 'completed',
            speed: 0,
            eta: 'Done',
            completedAt: Date.now(),
          }
        }
        const remainingPercent = 100 - next
        const remainingMB = (remainingPercent/100)* totalMB
        const etaSec = remainingMB / speed
        let eta = '—'
        if(etaSec < 60) eta = `${Math.ceil(etaSec)}s left`
        else if(etaSec < 3600) eta = `${Math.floor(etaSec/60)}m ${Math.ceil(etaSec%60)}s left`
        else eta = `${(etaSec/60).toFixed(0)}m left`
        return {
          ...item,
          progress: Math.min(99.6, next),
          speed: Number(speed.toFixed(1)),
          eta,
        }
      }))
    }, 300)
    intervals.current.set(id, interval)
  }, [pushToast])

  const pauseDownload = useCallback((id)=>{
    const iv = intervals.current.get(id)
    if(iv){ clearInterval(iv); intervals.current.delete(id) }
    updateDownload(id, {status:'paused', speed:0, eta:'Paused'})
    pushToast('Download paused', 'info')
  }, [updateDownload, pushToast])

  const resumeDownload = useCallback((id)=>{
    pushToast('Resuming…', 'info')
    startDownload(id)
  }, [startDownload, pushToast])

  const cancelDownload = useCallback((id)=>{
    const iv = intervals.current.get(id)
    if(iv){ clearInterval(iv); intervals.current.delete(id) }
    setDownloads(d => d.filter(x=>x.id!==id))
    pushToast('Download cancelled', 'info')
  }, [pushToast])

  const retryDownload = useCallback((id)=>{
    updateDownload(id, {progress:0, status:'queued', speed:0, eta:'Queued'})
    setTimeout(()=> startDownload(id), 400)
  }, [updateDownload, startDownload])

  const deleteDownload = useCallback((id)=>{
    setDownloads(d => d.filter(x=>x.id!==id))
    pushToast('Removed from library', 'info')
  }, [pushToast])

  const clearCompleted = useCallback(()=>{
    setDownloads(d => d.filter(x=> x.status!=='completed'))
    pushToast('Cleared completed', 'info')
  }, [pushToast])

  // auto-manage queue: max 2 concurrent downloading
  useEffect(()=>{
    const active = downloads.filter(d=>d.status==='downloading').length
    if(active >= 2) return
    const queued = downloads.filter(d=>d.status==='queued')
    if(queued.length===0) return
    // start oldest queued first ( FIFO )
    const toStart = queued.slice(-1) // newest queued is at top? we added at top, so oldest is last
    // Actually we want to start in order they were added: last in array is oldest
    // But simpler: start the first queued we find that hasn't started
    const next = [...queued].reverse()[0]
    if(next) startDownload(next.id)
  }, [downloads, startDownload])

  // cleanup intervals on unmount
  useEffect(()=> ()=> {
    intervals.current.forEach(clearInterval)
  }, [])

  const activeCount = downloads.filter(d=> d.status==='downloading' || d.status==='queued' || d.status==='paused').length
  const completedCount = downloads.filter(d=> d.status==='completed').length

  // storage used mock
  const storageUsed = downloads.filter(d=>d.status==='completed').reduce((acc,d)=> acc + parseSizeToMB(d.size), 0)
  const storageTotal = 64 * 1024 // 64 GB mock
  const storagePercent = Math.min(98, (storageUsed / storageTotal)*100)

  return (
    <DownloadContext.Provider value={{
      downloads,
      activeCount,
      completedCount,
      storageUsed,
      storageTotal,
      storagePercent,
      addDownload,
      updateDownload,
      startDownload,
      pauseDownload,
      resumeDownload,
      cancelDownload,
      retryDownload,
      deleteDownload,
      clearCompleted,
      toasts,
      pushToast,
    }}>
      {children}
    </DownloadContext.Provider>
  )
}

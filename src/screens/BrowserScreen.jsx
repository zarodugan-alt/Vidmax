import { useState, useEffect } from 'react'
import { ClipboardPaste, X, Search, Globe, Bookmark, History, Download, Link2, Sparkles, Play, ExternalLink, ShieldCheck, Zap, ChevronRight, Loader2, Check, AlertCircle, Clock, Trash2, ArrowUpRight, FileVideo, Music, Settings2, FolderDown } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import PlatformGrid from '../components/PlatformGrid'
import { isValidUrl, mockFetchMetadata } from '../engine/downloadEngine'
import { useDownloads } from '../context/DownloadContext'

export default function BrowserScreen({ onNavigateProgress }){
  const [url, setUrl] = useState('')
  const [meta, setMeta] = useState(null)
  const [loadingMeta, setLoadingMeta] = useState(false)
  const [metaError, setMetaError] = useState('')
  const [selectedQuality, setSelectedQuality] = useState('1080p')
  const [history, setHistory] = useState(()=>{
    try{ const r=localStorage.getItem('vidmax_hist'); return r? JSON.parse(r): ["https://www.youtube.com/watch?v=9bZkp7q19f0","https://www.tiktok.com/@khaby.lame/video/710","https://www.instagram.com/reel/C123/"] }catch{return []}
  })
  const [browserUrl, setBrowserUrl] = useState('https://www.youtube.com')
  const [showQualitySheet, setShowQualitySheet] = useState(false)
  const { addDownload, pushToast } = useDownloads()

  useEffect(()=>{
    localStorage.setItem('vidmax_hist', JSON.stringify(history.slice(0,8)))
  },[history])

  const handlePaste = async ()=>{
    try{
      const text = await navigator.clipboard.readText()
      if(text){ setUrl(text.trim()); fetchMeta(text.trim()) }
    }catch{
      pushToast('Clipboard not accessible — paste manually', 'info')
    }
  }

  const fetchMeta = async (u)=>{
    if(!u) return
    if(!isValidUrl(u)){
      setMetaError('Please enter a valid https:// link')
      return
    }
    setMetaError('')
    setLoadingMeta(true)
    setMeta(null)
    try{
      const data = await mockFetchMetadata(u)
      setMeta(data)
      setSelectedQuality(data.formats.find(f=>f.recommended)?.id || '1080p')
      setHistory(h=>{
        const n = [u, ...h.filter(x=>x!==u)].slice(0,8)
        return n
      })
    }catch(e){
      setMetaError(e.message || 'Failed to fetch')
    }finally{ setLoadingMeta(false)}
  }

  const handleGo = ()=>{
    if(!url) return
    fetchMeta(url)
  }

  const handleDownload = ()=>{
    if(!meta) return
    addDownload(meta, selectedQuality)
    // go to progress after small delay
    setTimeout(()=> onNavigateProgress?.(), 450)
  }

  const clearUrl = ()=>{
    setUrl('')
    setMeta(null)
    setMetaError('')
  }

  return (
    <div className="max-w-[960px] mx-auto px-4 sm:px-6 pb-28 pt-5 space-y-5">
      {/* Hero Paste Card */}
      <div className="rounded-[24px] bg-gradient-to-br from-[#101834] via-[#111B37] to-[#0E1630] border border-white/[0.08] p-4 sm:p-6 shadow-card overflow-hidden relative">
        <div className="absolute inset-0 bg-gradient-to-br from-[#6C5CFF]/[0.08] via-transparent to-[#00D9FF]/[0.06] pointer-events-none" />
        <div className="absolute -top-24 -right-24 h-[280px] w-[280px] bg-[#6C5CFF]/20 blur-[70px] rounded-full pointer-events-none" />
        <div className="absolute -bottom-24 -left-24 h-[240px] w-[240px] bg-[#00D9FF]/15 blur-[60px] rounded-full pointer-events-none" />
        <div className="relative">
          <div className="flex items-start justify-between gap-4 mb-4">
            <div>
              <div className="inline-flex items-center gap-2 text-[11px] font-bold tracking-widest uppercase text-[#00D9FF] mb-2">
                <span className="h-1.5 w-1.5 rounded-full bg-[#00D9FF] animate-pulse" /> Paste & Download in 1 tap
              </div>
              <h1 className="text-[22px] sm:text-[26px] font-extrabold leading-tight tracking-tight">
                Paste video link to<br />
                <span className="gradient-text">download instantly</span>
              </h1>
              <p className="text-[13px] text-slate-400 mt-2 max-w-[520px] leading-relaxed">Supports YouTube, TikTok, Instagram, Facebook, Twitter, Vimeo & 1000+ sites. Auto-detects quality, thumbnails & audio.</p>
            </div>
            <div className="hidden sm:flex h-11 w-11 rounded-2xl bg-white text-[#0F1423] items-center justify-center shadow">
              <ShieldCheck size={20} />
            </div>
          </div>

          {/* Input */}
          <div className={`group relative flex items-center gap-2 p-2 rounded-2xl bg-[#060A14] border ${metaError? 'border-red-500/50' : 'border-white/10 focus-within:border-[#6C5CFF]/50'} shadow-inner transition`}>
            <div className="hidden sm:flex h-10 w-10 rounded-xl bg-white/[0.06] border border-white/10 items-center justify-center text-slate-400 shrink-0">
              <Link2 size={16} />
            </div>
            <input
              value={url}
              onChange={e=>{ setUrl(e.target.value); if(metaError) setMetaError('') }}
              onKeyDown={e=> e.key==='Enter' && handleGo()}
              placeholder="Paste video URL here — https://..."
              className="flex-1 bg-transparent outline-none text-[14px] sm:text-[15px] placeholder:text-slate-500 text-white py-2 px-2 sm:px-0 min-w-0"
            />
            {url && (
              <button onClick={clearUrl} className="h-9 w-9 rounded-xl bg-white/10 hover:bg-white/15 text-slate-300 flex items-center justify-center shrink-0 transition">
                <X size={16} />
              </button>
            )}
            <button
              onClick={handlePaste}
              className="hidden sm:inline-flex items-center gap-2 h-10 px-4 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-white text-[13px] font-semibold shrink-0 transition"
            >
              <ClipboardPaste size={14} /> Paste
            </button>
            <button
              onClick={handleGo}
              disabled={!url || loadingMeta}
              className="h-10 sm:h-11 px-5 sm:px-6 rounded-xl bg-gradient-to-r from-[#6C5CFF] to-[#5B4CF0] hover:from-[#5A4AE6] hover:to-[#4F46E5] text-white text-[13px] sm:text-[14px] font-bold shadow-glow shrink-0 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transition"
            >
              {loadingMeta ? <Loader2 size={16} className="animate-spin" /> : <Download size={16} />}
              <span className="hidden sm:inline">Download</span>
              <span className="sm:hidden">Go</span>
            </button>
          </div>

          {metaError && (
            <div className="mt-3 flex items-center gap-2 text-[12px] font-medium text-red-300 bg-red-500/10 border border-red-500/20 px-3 py-2.5 rounded-xl">
              <AlertCircle size={14} /> {metaError}
            </div>
          )}

          {/* History chips */}
          {history.length>0 && !meta && !loadingMeta && (
            <div className="mt-4 flex items-center gap-2 flex-wrap">
              <span className="text-[11px] font-bold tracking-widest uppercase text-slate-500 flex items-center gap-1.5"><History size={12}/> Recent</span>
              {history.slice(0,4).map(h=>(
                <button key={h} onClick={()=>{setUrl(h); fetchMeta(h)}} className="group flex items-center gap-2 pl-3 pr-2 py-1.5 rounded-full bg-white/[0.06] border border-white/10 hover:bg-white/10 text-[12px] text-slate-300 transition max-w-[220px]">
                  <span className="truncate">{h.replace('https://','')}</span>
                  <span className="h-6 w-6 rounded-full bg-white/10 flex items-center justify-center group-hover:bg-[#6C5CFF] group-hover:text-white transition shrink-0"><ArrowUpRight size={12}/></span>
                </button>
              ))}
              <button onClick={()=>setHistory([])} className="text-[11px] text-slate-500 hover:text-slate-300 flex items-center gap-1"><Trash2 size={12}/> Clear</button>
            </div>
          )}

          {/* Loading skeleton */}
          <AnimatePresence>
            {loadingMeta && (
              <motion.div initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} exit={{opacity:0}} className="mt-5 rounded-2xl bg-[#0F1423] border border-white/10 p-4 flex gap-4">
                <div className="h-[86px] w-[152px] rounded-xl bg-white/5 animate-pulse shrink-0" />
                <div className="flex-1 space-y-3 py-1">
                  <div className="h-3 w-3/4 bg-white/10 rounded animate-pulse" />
                  <div className="h-3 w-1/2 bg-white/5 rounded animate-pulse" />
                  <div className="h-8 w-32 bg-[#6C5CFF]/20 rounded-xl animate-pulse mt-2" />
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Preview Card */}
          <AnimatePresence>
            {meta && !loadingMeta && (
              <motion.div initial={{opacity:0,y:10, scale:0.98}} animate={{opacity:1,y:0, scale:1}} exit={{opacity:0}} className="mt-5 rounded-2xl bg-[#0F1423] border border-white/10 overflow-hidden shadow-card">
                <div className="p-3 sm:p-4 flex flex-col sm:flex-row gap-4">
                  <div className="relative shrink-0">
                    <img src={meta.thumbnail} alt="" className="h-[168px] sm:h-[122px] w-full sm:w-[216px] object-cover rounded-xl border border-white/10" />
                    <span className="absolute bottom-2 right-2 text-[11px] font-bold bg-black/80 backdrop-blur px-2 py-1 rounded-full border border-white/10 text-white">{meta.duration}</span>
                    <span className="absolute top-2 left-2 text-[10px] font-extrabold tracking-widest uppercase px-2 py-1 rounded-full bg-[#00D9FF] text-[#06101A]">{meta.platformName}</span>
                    <button className="absolute inset-0 m-auto h-10 w-10 rounded-full bg-white/90 backdrop-blur flex items-center justify-center text-[#0F1423] shadow-lg opacity-0 hover:opacity-0 sm:opacity-100 transition">
                      <Play size={16} className="fill-[#0F1423] ml-0.5" />
                    </button>
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-[15px] font-bold leading-snug line-clamp-2">{meta.title}</h3>
                    <div className="text-[12px] text-slate-400 mt-1 flex items-center gap-2 flex-wrap">
                      <span className="inline-flex items-center gap-1.5"><span className="h-5 w-5 rounded-full bg-white/10 flex items-center justify-center text-[10px] font-bold">{meta.platformName[0]}</span> {meta.uploader}</span>
                      <span className="h-1 w-1 rounded-full bg-white/20" />
                      <span>{meta.viewCount}</span>
                    </div>

                    <div className="mt-3 flex flex-wrap gap-2">
                      <button onClick={()=>setShowQualitySheet(true)} className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-white text-[#0F1423] text-[13px] font-bold hover:bg-slate-100 transition">
                        <Settings2 size={14} />
                        {meta.formats.find(f=>f.id===selectedQuality)?.label || selectedQuality}
                        <ChevronRight size={14} className="opacity-50" />
                      </button>
                      <span className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white/10 border border-white/10 text-[12px] font-medium text-slate-300">
                        <FileVideo size={14} /> {meta.formats.find(f=>f.id===selectedQuality)?.ext.toUpperCase()}
                        <span className="h-3 w-px bg-white/15 mx-1" />
                        {meta.formats.find(f=>f.id===selectedQuality)?.size}
                      </span>
                    </div>

                    <div className="mt-3 flex gap-2">
                      <button onClick={handleDownload} className="flex-1 sm:flex-none inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-[#6C5CFF] to-[#00D9FF] text-white font-extrabold text-[14px] shadow-glow hover:opacity-95 transition">
                        <Download size={18} /> Download Now
                      </button>
                      <button onClick={clearUrl} className="hidden sm:inline-flex items-center gap-2 px-4 py-3 rounded-xl bg-white/10 border border-white/10 text-white font-semibold hover:bg-white/15 transition">
                        <X size={16} /> Cancel
                      </button>
                    </div>
                  </div>
                </div>
                <div className="px-4 py-3 bg-[#060A14]/60 border-t border-white/[0.06] flex items-center justify-between">
                  <span className="text-[11px] font-medium text-slate-400 flex items-center gap-1.5"><ShieldCheck size={12} className="text-[#10B981]" /> Safe & private • No watermark</span>
                  <span className="text-[11px] text-slate-500 hidden sm:inline">MP4 • H.264 • AAC</span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Platforms */}
      <div className="rounded-[20px] bg-[#0F1423] border border-white/[0.06] p-4 sm:p-5">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-[13px] font-extrabold tracking-widest uppercase text-slate-300 flex items-center gap-2"><Sparkles size={14} className="text-[#6C5CFF]" /> Popular sites</h3>
          <span className="text-[11px] font-medium text-slate-500 hidden sm:inline">Tap to open in browser</span>
        </div>
        <PlatformGrid onSelect={(p)=>{
          const url = `https://www.${p.handle}`
          setBrowserUrl(url)
          pushToast(`Opened ${p.name} in browser`, 'info')
          document.getElementById('browser-anchor')?.scrollIntoView({behavior:'smooth', block:'start'})
        }} />
      </div>

      {/* In-app Browser */}
      <div id="browser-anchor" className="rounded-[20px] bg-[#0F1423] border border-white/[0.06] overflow-hidden shadow-card">
        <div className="h-[52px] flex items-center gap-2 px-3 border-b border-white/[0.06] bg-[#060A14]/50 backdrop-blur">
          <div className="flex items-center gap-1.5 shrink-0">
            <span className="h-3 w-3 rounded-full bg-[#FF5F57] border border-black/10" />
            <span className="h-3 w-3 rounded-full bg-[#FFBD2E] border border-black/10" />
            <span className="h-3 w-3 rounded-full bg-[#28CA42] border border-black/10" />
          </div>
          <div className="flex items-center gap-2 flex-1 min-w-0">
            <button className="h-8 w-8 rounded-lg bg-white/10 border border-white/10 flex items-center justify-center text-slate-400 hover:text-white transition shrink-0">
              <Clock size={14} />
            </button>
            <div className="flex-1 flex items-center gap-2 h-9 px-3 rounded-xl bg-white/[0.06] border border-white/10 min-w-0">
              <Globe size={14} className="text-[#00D9FF] shrink-0" />
              <input
                value={browserUrl}
                onChange={e=>setBrowserUrl(e.target.value)}
                className="flex-1 bg-transparent outline-none text-[13px] text-slate-200 placeholder:text-slate-500 min-w-0"
                placeholder="Search or enter website URL"
              />
              <button className="h-6 w-6 rounded-full bg-[#6C5CFF] flex items-center justify-center text-white shrink-0">
                <Search size={12} />
              </button>
            </div>
            <button className="hidden sm:flex h-8 w-8 rounded-lg bg-white text-[#0F1423] items-center justify-center shrink-0">
              <Bookmark size={14} />
            </button>
          </div>
        </div>

        {/* Browser content mock */}
        <div className="relative bg-[#F8FAFC] text-slate-900">
          {/* Toolbar */}
          <div className="h-10 flex items-center gap-2 px-3 border-b border-slate-200 bg-white">
            <div className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-slate-300" />
              <span className="text-[11px] font-bold tracking-widest uppercase text-slate-500">Secure browsing</span>
              <ShieldCheck size={12} className="text-emerald-500" />
            </div>
            <div className="ml-auto flex items-center gap-2">
              <span className="hidden sm:inline text-[11px] font-medium text-slate-600">Detect videos automatically</span>
              <span className="inline-flex items-center gap-1 text-[11px] font-bold px-2 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200"><span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" /> Active</span>
            </div>
          </div>

          {/* Iframe placeholder - realistic */}
          <div className="h-[360px] sm:h-[420px] relative overflow-hidden bg-white">
            {/* Fake YouTube-like grid when youtube, else generic */}
            {browserUrl.includes('youtube') ? (
              <div className="p-4 space-y-4 overflow-auto h-full no-scrollbar">
                <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
                  <div className="h-8 w-20 rounded bg-red-600 flex items-center justify-center text-white font-black text-[13px]">YouTube</div>
                  <div className="flex-1 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center px-3 text-[13px] text-slate-500 gap-2"><Search size={14}/> Search</div>
                  <div className="h-8 w-8 rounded-full bg-slate-900" />
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {[1,2,3,4].map(i=>(
                    <div key={i} className="group cursor-pointer" onClick={()=>{
                      const fake = `https://www.youtube.com/watch?v=demo${i}${Math.floor(Math.random()*1000)}`
                      setUrl(fake)
                      fetchMeta(fake)
                    }}>
                      <div className="relative aspect-video rounded-xl overflow-hidden bg-slate-900">
                        <img src={`https://images.unsplash.com/photo-${i===1 ? '1611224923853-80b023f02d71' : i===2 ? '1498050108023-c5249f4df085' : i===3 ? '1550745165-9bc0b252726f' : '1511671782779-c97d3d27a1d4'}?w=640&h=360&fit=crop`} className="h-full w-full object-cover group-hover:scale-[1.02] transition" alt="" />
                        <span className="absolute bottom-1.5 right-1.5 text-[11px] font-bold bg-black/80 text-white px-1.5 py-0.5 rounded">12:34</span>
                        <span className="absolute top-2 left-2 text-[10px] font-bold px-2 py-1 rounded-full bg-[#6C5CFF] text-white hidden group-hover:inline-flex">Tap to download</span>
                        <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition flex items-center justify-center">
                          <div className="h-10 w-10 rounded-full bg-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition"><Play size={14} className="fill-slate-900 ml-0.5" /></div>
                        </div>
                      </div>
                      <div className="pt-2">
                        <div className="text-[13px] font-semibold leading-tight line-clamp-2">Amazing video #{i} — Tap to auto-paste link and download instantly</div>
                        <div className="text-[11px] text-slate-500">Channel • 1.2M views • 2 days ago</div>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="flex justify-center pt-2">
                  <span className="text-[11px] font-medium px-3 py-1.5 rounded-full bg-[#6C5CFF]/10 text-[#6C5CFF] border border-[#6C5CFF]/20 flex items-center gap-1.5"><Zap size={12}/> Videos detected • Tap any to download</span>
                </div>
              </div>
            ) : (
              <div className="h-full flex flex-col">
                <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
                  <div className="h-14 w-14 rounded-2xl bg-slate-900 flex items-center justify-center text-white mb-4"><Globe size={22} /></div>
                  <div className="font-bold text-slate-900">Browsing {new URL(browserUrl.startsWith('http')? browserUrl : 'https://'+browserUrl).hostname.replace('www.','')}</div>
                  <div className="text-[13px] text-slate-500 max-w-[420px] mt-1">Vidmax detects videos on this page automatically. When a video is found, a floating download button will appear. Tap it or copy the link above.</div>
                  <div className="mt-4 flex gap-2">
                    <button onClick={()=>{
                      const fake = browserUrl
                      setUrl(fake)
                      fetchMeta(fake)
                    }} className="px-4 py-2 rounded-xl bg-slate-900 text-white text-[13px] font-bold flex items-center gap-2"><Download size={14}/> Detect & Download</button>
                    <a href={browserUrl} target="_blank" rel="noreferrer" className="px-4 py-2 rounded-xl bg-white border border-slate-200 text-slate-700 text-[13px] font-semibold flex items-center gap-2"><ExternalLink size={14}/> Open externally</a>
                  </div>
                  <div className="mt-6 grid grid-cols-3 gap-2 w-full max-w-[420px]">
                    {['Highest quality','No watermark','Fast & private'].map(t=>(
                      <div key={t} className="rounded-xl bg-slate-50 border border-slate-200 p-3 text-center">
                        <div className="text-[11px] font-bold text-slate-900">{t}</div>
                        <div className="text-[10px] text-slate-500">Included</div>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="h-10 bg-slate-50 border-t border-slate-200 flex items-center justify-center gap-2 text-[11px] font-medium text-slate-600">
                  <ShieldCheck size={14} className="text-emerald-500" /> Browsing is private • No tracking
                </div>
              </div>
            )}

            {/* Floating detect button */}
            <motion.div
              initial={{y:20, opacity:0}} animate={{y:0, opacity:1}}
              className="absolute bottom-3 right-3 flex items-center gap-2 pl-3 pr-2 py-1.5 rounded-full bg-[#0F1423] border border-white/15 shadow-xl text-white"
            >
              <span className="h-2 w-2 rounded-full bg-[#00D9FF] animate-pulse" />
              <span className="text-[11px] font-bold">3 videos found</span>
              <button onClick={()=>{
                const u = browserUrl.includes('youtube') ? 'https://www.youtube.com/watch?v=autoDetect123' : browserUrl
                setUrl(u); fetchMeta(u)
              }} className="h-7 px-3 rounded-full bg-[#6C5CFF] text-white text-[11px] font-bold">Download</button>
            </motion.div>
          </div>
        </div>

        <div className="px-3 py-2.5 bg-[#060A14] border-t border-white/[0.06] flex items-center justify-between text-[11px]">
          <span className="flex items-center gap-2 text-slate-400"><History size={12}/> History • Bookmarks • Downloads auto-saved</span>
          <span className="hidden sm:flex items-center gap-1.5 text-slate-500"><Zap size={12} className="text-[#6C5CFF]" /> Auto-detect enabled</span>
        </div>
      </div>

      {/* How it works */}
      <div className="grid sm:grid-cols-3 gap-3">
        {[
          { step:'01', title:'Copy or browse', desc:'Paste any link or browse inside Vidmax browser', icon: Link2 },
          { step:'02', title:'Pick quality', desc:'Choose 4K, HD, MP3 — preview & size included', icon: Settings2 },
          { step:'03', title:'Download & manage', desc:'Track progress, pause, resume, save to library', icon: FolderDown },
        ].map(s=>{
          const Icon=s.icon
          return (
            <div key={s.step} className="rounded-2xl bg-[#0F1423] border border-white/[0.06] p-4 flex gap-3">
              <div className="h-9 w-9 rounded-xl bg-white/[0.06] border border-white/10 flex items-center justify-center text-slate-300 shrink-0"><Icon size={16}/></div>
              <div>
                <div className="text-[11px] font-bold tracking-widest text-[#6C5CFF]">{s.step}</div>
                <div className="text-[13px] font-bold text-white leading-tight">{s.title}</div>
                <div className="text-[12px] text-slate-400 leading-snug mt-0.5">{s.desc}</div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Quality Sheet */}
      <AnimatePresence>
        {showQualitySheet && meta && (
          <>
            <motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} onClick={()=>setShowQualitySheet(false)} className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm" />
            <motion.div initial={{y:'100%'}} animate={{y:0}} exit={{y:'100%'}} transition={{type:'spring', damping:28, stiffness:320}} className="fixed bottom-0 inset-x-0 z-50 max-w-[560px] mx-auto bg-[#0F1423] rounded-t-[24px] border border-white/10 shadow-2xl max-h-[86vh] flex flex-col">
              <div className="h-1.5 w-10 bg-white/20 rounded-full mx-auto mt-3" />
              <div className="p-5 pb-3 border-b border-white/10">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="text-[13px] font-extrabold tracking-widest uppercase text-slate-400 flex items-center gap-2"><Music size={14}/> Select format & quality</div>
                    <div className="text-[11px] text-slate-500 mt-1">Original source preserved • Fast muxing</div>
                  </div>
                  <button onClick={()=>setShowQualitySheet(false)} className="h-8 w-8 rounded-full bg-white/10 flex items-center justify-center text-slate-300"><X size={14}/></button>
                </div>
              </div>
              <div className="overflow-auto p-3 space-y-2 no-scrollbar flex-1">
                <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500 px-2">Video • MP4</div>
                {meta.formats.filter(f=>!f.audioOnly).map(f=>{
                  const active = f.id===selectedQuality
                  return (
                    <button key={f.id} onClick={()=>{setSelectedQuality(f.id); setShowQualitySheet(false)}} className={`w-full flex items-center gap-3 p-3 rounded-2xl border text-left transition ${active? 'bg-[#6C5CFF] border-[#6C5CFF] text-white shadow-glow' : 'bg-white/[0.04] border-white/10 hover:bg-white/[0.07] text-white'}`}>
                      <div className={`h-10 w-10 rounded-xl flex items-center justify-center shrink-0 ${active? 'bg-white text-[#6C5CFF]' : 'bg-white/10 text-slate-300'}`}><FileVideo size={16}/></div>
                      <div className="flex-1 min-w-0">
                        <div className="text-[13px] font-bold flex items-center gap-2">{f.label} {f.recommended && <span className={`text-[10px] px-1.5 py-0.5 rounded-full font-extrabold tracking-widest uppercase ${active? 'bg-white text-[#6C5CFF]' : 'bg-[#00D9FF] text-[#06101A]'}`}>Recommended</span>}</div>
                        <div className={`text-[11px] ${active? 'text-white/80' : 'text-slate-400'}`}>{f.ext.toUpperCase()} • {f.fps}fps • {f.size}</div>
                      </div>
                      <div className={`h-6 w-6 rounded-full border-2 flex items-center justify-center shrink-0 ${active? 'bg-white border-white text-[#6C5CFF]' : 'border-white/20'}`}>{active && <Check size={12} strokeWidth={3} />}</div>
                    </button>
                  )
                })}
                <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500 px-2 pt-2">Audio only</div>
                {meta.formats.filter(f=>f.audioOnly).map(f=>{
                  const active = f.id===selectedQuality
                  return (
                    <button key={f.id} onClick={()=>{setSelectedQuality(f.id); setShowQualitySheet(false)}} className={`w-full flex items-center gap-3 p-3 rounded-2xl border text-left transition ${active? 'bg-[#6C5CFF] border-[#6C5CFF] text-white' : 'bg-white/[0.04] border-white/10 hover:bg-white/[0.07] text-white'}`}>
                      <div className={`h-10 w-10 rounded-xl flex items-center justify-center shrink-0 ${active? 'bg-white text-[#6C5CFF]' : 'bg-white/10 text-slate-300'}`}><Music size={16}/></div>
                      <div className="flex-1">
                        <div className="text-[13px] font-bold">{f.label}</div>
                        <div className={`text-[11px] ${active? 'text-white/80' : 'text-slate-400'}`}>{f.ext.toUpperCase()} • {f.size} • Best for music</div>
                      </div>
                      <div className={`h-6 w-6 rounded-full border-2 flex items-center justify-center ${active? 'bg-white border-white text-[#6C5CFF]' : 'border-white/20'}`}>{active && <Check size={12} strokeWidth={3} />}</div>
                    </button>
                  )
                })}
              </div>
              <div className="p-3 border-t border-white/10 bg-[#060A14] rounded-t-none">
                <button onClick={()=>setShowQualitySheet(false)} className="w-full py-3 rounded-xl bg-white text-[#0F1423] font-bold">Done</button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  )
}



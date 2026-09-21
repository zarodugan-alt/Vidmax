import { useState, useMemo } from 'react'
import { Pause, Play, X, RotateCcw, Trash2, Download, Zap, Clock, HardDrive, Wifi, Gauge, ChevronDown, AlertTriangle, CheckCircle2, Timer, Layers, Sparkles, ArrowRight } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { useDownloads } from '../context/DownloadContext'

function ProgressBar({ value, status }){
  const color = status==='paused' ? 'bg-amber-400' : status==='failed' ? 'bg-red-500' : 'bg-gradient-to-r from-[#6C5CFF] to-[#00D9FF]'
  return (
    <div className="h-1.5 w-full bg-white/10 rounded-full overflow-hidden">
      <motion.div
        initial={{width:0}}
        animate={{width:`${value}%`}}
        transition={{duration:0.4}}
        className={`h-full rounded-full ${color} relative overflow-hidden`}
      >
        {status==='downloading' && <div className="absolute inset-0 bg-white/20 animate-[shimmer_1.2s_infinite] -skew-x-12" style={{background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)', backgroundSize:'200% 100%'}} />}
      </motion.div>
    </div>
  )
}

export default function ProgressScreen({ onGoBrowse }){
  const { downloads, pauseDownload, resumeDownload, cancelDownload, retryDownload, storagePercent, storageUsed } = useDownloads()
  const [filter, setFilter] = useState('all')

  const active = useMemo(()=> downloads.filter(d=> ['queued','downloading','paused','failed'].includes(d.status)), [downloads])
  const filtered = useMemo(()=>{
    if(filter==='all') return active
    return active.filter(d=> d.status===filter)
  }, [active, filter])

  const stats = {
    downloading: downloads.filter(d=>d.status==='downloading').length,
    queued: downloads.filter(d=>d.status==='queued').length,
    paused: downloads.filter(d=>d.status==='paused').length,
  }

  // speed mock total
  const totalSpeed = downloads.filter(d=>d.status==='downloading').reduce((a,c)=> a + (c.speed||0), 0).toFixed(1)

  if(active.length===0){
    return (
      <div className="max-w-[960px] mx-auto px-4 sm:px-6 pb-28 pt-5 space-y-5">
        {/* Header stats - even when empty we show storage */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="rounded-2xl bg-[#0F1423] border border-white/[0.06] p-4 flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-[#6C5CFF]/15 border border-[#6C5CFF]/20 flex items-center justify-center text-[#6C5CFF]"><Download size={18}/></div>
            <div>
              <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500">Active</div>
              <div className="text-[18px] font-extrabold leading-none">0 files</div>
              <div className="text-[11px] text-slate-500">No active downloads</div>
            </div>
            <div className="ml-auto hidden sm:flex h-8 px-3 rounded-full bg-emerald-500/10 border border-emerald-500/20 items-center gap-1.5 text-emerald-400 text-[11px] font-bold"><Wifi size={12}/> Idle</div>
          </div>
          <div className="rounded-2xl bg-[#0F1423] border border-white/[0.06] p-4 flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-[#00D9FF]/15 border border-[#00D9FF]/20 flex items-center justify-center text-[#00D9FF]"><Gauge size={18}/></div>
            <div>
              <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500">Speed</div>
              <div className="text-[18px] font-extrabold leading-none">— MB/s</div>
              <div className="text-[11px] text-slate-500">Waiting for jobs</div>
            </div>
          </div>
          <div className="rounded-2xl bg-[#0F1423] border border-white/[0.06] p-4 flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-white/[0.06] border border-white/10 flex items-center justify-center text-slate-300"><HardDrive size={18}/></div>
            <div className="flex-1">
              <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500">Storage • 64 GB</div>
              <div className="text-[13px] font-bold">{storageUsed.toFixed(0)} MB used • {storagePercent.toFixed(1)}%</div>
              <div className="h-1.5 w-full bg-white/10 rounded-full overflow-hidden mt-1.5"><div className="h-full bg-gradient-to-r from-[#6C5CFF] to-[#00D9FF]" style={{width:`${Math.min(100, storagePercent*3)}%`}} /></div>
            </div>
          </div>
        </div>

        <div className="rounded-[24px] bg-[#0F1423] border border-white/[0.06] p-8 sm:p-12 flex flex-col items-center text-center">
          <div className="h-20 w-20 rounded-[20px] bg-gradient-to-br from-[#6C5CFF]/20 to-[#00D9FF]/20 border border-white/10 flex items-center justify-center mb-5">
            <Download size={28} className="text-white" />
          </div>
          <h3 className="text-[18px] font-extrabold">No active downloads</h3>
          <p className="text-[13px] text-slate-400 max-w-[420px] mt-1.5 leading-relaxed">Paste a link in Browser tab and downloads will appear here with live progress, speed, ETA and pause/resume controls.</p>
          <button onClick={onGoBrowse} className="mt-6 inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-white text-[#0F1423] font-bold hover:bg-slate-100 transition">
            <Sparkles size={16} /> Go to Browser <ArrowRight size={16} />
          </button>
          <div className="mt-8 grid grid-cols-3 gap-3 w-full max-w-[560px] text-left">
            {[
              {k:'Queue', v:'Up to 2 parallel', icon: Layers},
              {k:'Auto-resume', v:'On network restore', icon: Wifi},
              {k:'Background', v:'Keep app open', icon: Timer},
            ].map(f=>{
              const I=f.icon
              return (
                <div key={f.k} className="rounded-xl bg-white/[0.04] border border-white/10 p-3 flex gap-2">
                  <div className="h-8 w-8 rounded-lg bg-white/10 flex items-center justify-center text-slate-300 shrink-0"><I size={14}/></div>
                  <div><div className="text-[11px] font-bold text-white">{f.k}</div><div className="text-[11px] text-slate-400 leading-tight">{f.v}</div></div>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-[960px] mx-auto px-4 sm:px-6 pb-28 pt-5 space-y-5">
      {/* Top stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="rounded-2xl bg-gradient-to-br from-[#101834] to-[#0E1630] border border-white/[0.08] p-4 flex items-center gap-3 shadow-card">
          <div className="h-10 w-10 rounded-xl bg-[#6C5CFF] flex items-center justify-center text-white shadow-glow"><Download size={18}/></div>
          <div>
            <div className="text-[11px] font-bold tracking-widest uppercase text-[#8B90B5]">Active queue</div>
            <div className="text-[18px] font-extrabold leading-none">{active.length} files</div>
            <div className="text-[11px] text-slate-400">{stats.downloading} downloading • {stats.queued} queued • {stats.paused} paused</div>
          </div>
        </div>
        <div className="rounded-2xl bg-[#0F1423] border border-white/[0.06] p-4 flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-[#00D9FF]/15 border border-[#00D9FF]/20 flex items-center justify-center text-[#00D9FF]"><Gauge size={18}/></div>
          <div>
            <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500">Live speed</div>
            <div className="text-[18px] font-extrabold leading-none flex items-baseline gap-1">{totalSpeed} <span className="text-[11px] font-bold text-slate-400">MB/s</span></div>
            <div className="text-[11px] text-emerald-400 flex items-center gap-1"><span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" /> Accelerated • 2 threads</div>
          </div>
        </div>
        <div className="rounded-2xl bg-[#0F1423] border border-white/[0.06] p-4 flex items-center gap-3">
          <div className="relative h-12 w-12 shrink-0">
            <svg className="h-12 w-12 -rotate-90" viewBox="0 0 36 36">
              <circle cx="18" cy="18" r="14" fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth="4" />
              <circle cx="18" cy="18" r="14" fill="none" stroke="url(#g)" strokeWidth="4" strokeLinecap="round" strokeDasharray={`${storagePercent*3}, 100`} />
              <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stopColor="#6C5CFF"/><stop offset="100%" stopColor="#00D9FF"/></linearGradient></defs>
            </svg>
            <div className="absolute inset-0 flex items-center justify-center"><HardDrive size={14} className="text-slate-300" /></div>
          </div>
          <div className="flex-1">
            <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500">Storage used</div>
            <div className="text-[13px] font-bold">{storageUsed.toFixed(0)} MB / 64 GB • {storagePercent.toFixed(1)}%</div>
            <div className="text-[11px] text-slate-500">Auto-clean disabled • Wi-Fi only off</div>
          </div>
        </div>
      </div>

      {/* Filter */}
      <div className="flex items-center gap-2 overflow-auto no-scrollbar py-1">
        {[
          {id:'all', label:`All • ${active.length}`},
          {id:'downloading', label:`Downloading • ${stats.downloading}`},
          {id:'queued', label:`Queued • ${stats.queued}`},
          {id:'paused', label:`Paused • ${stats.paused}`},
        ].map(f=>(
          <button key={f.id} onClick={()=>setFilter(f.id)} className={`whitespace-nowrap px-4 py-2 rounded-full text-[13px] font-bold border transition ${filter===f.id? 'bg-white text-[#0F1423] border-white' : 'bg-white/[0.06] text-slate-300 border-white/10 hover:bg-white/10'}`}>
            {f.label}
          </button>
        ))}
        <span className="ml-auto hidden sm:inline-flex items-center gap-1.5 text-[11px] font-medium text-slate-500"><Clock size={12}/> Auto-sorts by ETA</span>
      </div>

      {/* List */}
      <div className="space-y-3">
        <AnimatePresence initial={false}>
          {filtered.map(item=>(
            <motion.div key={item.id} layout initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} exit={{opacity:0, scale:0.98}} className="rounded-[18px] bg-[#0F1423] border border-white/[0.06] overflow-hidden shadow-card">
              <div className="p-3 sm:p-4 flex gap-3 sm:gap-4">
                <div className="relative shrink-0">
                  <img src={item.thumbnail} alt="" className="h-[86px] w-[152px] sm:h-[92px] sm:w-[164px] object-cover rounded-xl border border-white/10" />
                  <span className={`absolute top-2 left-2 text-[10px] font-extrabold tracking-widest uppercase px-2 py-1 rounded-full border ${item.status==='downloading'? 'bg-[#00D9FF] text-[#06101A] border-[#00D9FF]' : item.status==='paused'? 'bg-amber-400 text-[#1A1200] border-amber-400' : item.status==='queued'? 'bg-white/15 text-white border-white/20 backdrop-blur' : 'bg-red-500 text-white border-red-500'}`}>
                    {item.status}
                  </span>
                  <span className="absolute bottom-2 right-2 text-[11px] font-bold bg-black/80 backdrop-blur px-2 py-1 rounded-full border border-white/10 text-white flex items-center gap-1">
                    {item.status==='downloading' ? <><Zap size={10} className="text-[#00D9FF]" /> {item.speed} MB/s</> : item.quality}
                  </span>
                </div>
                <div className="flex-1 min-w-0 flex flex-col">
                  <div className="flex items-start justify-between gap-3">
                    <h3 className="text-[13px] sm:text-[14px] font-bold leading-snug line-clamp-2 flex-1">{item.title}</h3>
                    <span className="hidden sm:inline-flex items-center gap-1 text-[11px] font-bold px-2 py-1 rounded-full bg-white/10 border border-white/10 text-slate-300 shrink-0">{item.format.toUpperCase()} • {item.size}</span>
                  </div>
                  <div className="text-[11px] text-slate-400 mt-1 flex items-center gap-2">
                    <span className="inline-flex items-center gap-1.5"><span className="h-4 w-4 rounded-full bg-white/10 flex items-center justify-center text-[9px] font-bold">{item.platformName[0]}</span> {item.platformName}</span>
                    <span className="h-1 w-1 rounded-full bg-white/20" />
                    <span>{item.duration}</span>
                    <span className="hidden sm:inline h-1 w-1 rounded-full bg-white/20" />
                    <span className="hidden sm:inline text-slate-500 truncate max-w-[180px]">{item.url.replace('https://','')}</span>
                  </div>

                  <div className="mt-3">
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-[11px] font-bold tracking-widest uppercase text-slate-500 flex items-center gap-1.5">
                        {item.status==='downloading' ? <Timer size={12} className="text-[#00D9FF]" /> : item.status==='paused' ? <Pause size={12} className="text-amber-400" /> : <Clock size={12} />}
                        {item.status==='downloading' ? `${item.progress.toFixed(1)}% • ${item.eta}` : item.status==='queued' ? 'Queued • Waiting for slot' : item.status==='paused' ? 'Paused • Tap resume' : item.status==='failed' ? 'Failed • Tap retry' : item.eta}
                      </span>
                      <span className="text-[11px] font-mono font-bold text-slate-300">{item.progress.toFixed(0)}%</span>
                    </div>
                    <ProgressBar value={item.progress} status={item.status} />
                  </div>

                  <div className="mt-3 flex items-center gap-2 flex-wrap">
                    {item.status==='downloading' && (
                      <>
                        <button onClick={()=>pauseDownload(item.id)} className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-amber-400 text-[#1A1200] text-[12px] font-bold hover:bg-amber-300 transition"><Pause size={14}/> Pause</button>
                        <button onClick={()=>cancelDownload(item.id)} className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white/10 border border-white/10 text-white text-[12px] font-semibold hover:bg-white/15 transition"><X size={14}/> Cancel</button>
                      </>
                    )}
                    {item.status==='paused' && (
                      <>
                        <button onClick={()=>resumeDownload(item.id)} className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#6C5CFF] text-white text-[12px] font-bold hover:bg-[#5A4AE6] transition"><Play size={14} className="fill-white" /> Resume</button>
                        <button onClick={()=>cancelDownload(item.id)} className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white/10 border border-white/10 text-white text-[12px] font-semibold hover:bg-white/15 transition"><Trash2 size={14}/> Cancel</button>
                      </>
                    )}
                    {item.status==='queued' && (
                      <>
                        <span className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-slate-400 text-[12px] font-medium"><Clock size={14}/> Queued • 2 parallel max</span>
                        <button onClick={()=>cancelDownload(item.id)} className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white/10 border border-white/10 text-white text-[12px] font-semibold hover:bg-white/15 transition"><X size={14}/> Cancel</button>
                      </>
                    )}
                    {item.status==='failed' && (
                      <>
                        <button onClick={()=>retryDownload(item.id)} className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white text-[#0F1423] text-[12px] font-bold hover:bg-slate-100 transition"><RotateCcw size={14}/> Retry</button>
                        <button onClick={()=>cancelDownload(item.id)} className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white/10 border border-white/10 text-white text-[12px] font-semibold hover:bg-white/15 transition"><Trash2 size={14}/> Remove</button>
                      </>
                    )}
                    <span className="ml-auto hidden sm:inline-flex items-center gap-1 text-[11px] text-slate-500"><HardDrive size={12}/> /Downloads/Vidmax • {item.quality}</span>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>

      {filtered.length===0 && active.length>0 && (
        <div className="rounded-2xl bg-white/[0.04] border border-dashed border-white/15 p-8 text-center">
          <div className="text-[13px] font-semibold text-slate-300">No {filter} downloads</div>
          <div className="text-[12px] text-slate-500">Try another filter or wait for queue</div>
        </div>
      )}

      {/* Speed graph mock + tips */}
      <div className="grid sm:grid-cols-3 gap-3">
        <div className="sm:col-span-2 rounded-2xl bg-[#0F1423] border border-white/[0.06] p-4">
          <div className="flex items-center justify-between mb-3">
            <h4 className="text-[12px] font-extrabold tracking-widest uppercase text-slate-400 flex items-center gap-2"><Gauge size={14} className="text-[#00D9FF]" /> Live throughput</h4>
            <span className="text-[11px] font-bold px-2 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">{totalSpeed} MB/s avg</span>
          </div>
          <div className="h-[84px] flex items-end gap-[3px]">
            {Array.from({length: 32}).map((_,i)=>{
              const h = 18 + Math.sin(i/2.5)*12 + Math.random()*22
              const active = i>24
              return <div key={i} className={`flex-1 rounded-t-md transition ${active? 'bg-gradient-to-t from-[#6C5CFF] to-[#00D9FF]' : 'bg-white/10'}`} style={{height: `${h}%`}} />
            })}
          </div>
          <div className="flex justify-between text-[10px] font-medium text-slate-500 mt-2">
            <span>60s ago</span><span>30s ago</span><span className="text-[#00D9FF] font-bold">Now</span>
          </div>
        </div>
        <div className="rounded-2xl bg-gradient-to-br from-[#6C5CFF] to-[#4F46E5] p-4 text-white flex flex-col">
          <div className="h-8 w-8 rounded-xl bg-white/15 border border-white/20 flex items-center justify-center mb-3"><Zap size={16}/></div>
          <div className="text-[13px] font-extrabold leading-tight">Keep app in foreground for fastest speed</div>
          <div className="text-[12px] text-white/80 leading-snug mt-1">Background downloads may be throttled by OS. Disable battery optimization for Vidmax.</div>
          <button className="mt-3 inline-flex items-center gap-1.5 text-[12px] font-bold px-3 py-2 rounded-xl bg-white text-[#4F46E5] self-start">Learn more <ChevronDown size={12} className="-rotate-90" /></button>
        </div>
      </div>

      <div className="rounded-2xl bg-amber-400/10 border border-amber-400/20 p-3 flex gap-3">
        <div className="h-8 w-8 rounded-xl bg-amber-400 flex items-center justify-center text-[#1A1200] shrink-0"><AlertTriangle size={14}/></div>
        <div>
          <div className="text-[12px] font-bold text-amber-200">Smart queue • 2 simultaneous max</div>
          <div className="text-[12px] text-amber-200/70 leading-snug">Queued items start automatically. You can pause any download — we keep partial files for instant resume.</div>
        </div>
      </div>
    </div>
  )
}

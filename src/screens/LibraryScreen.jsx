import { useState, useMemo } from 'react'
import { Search, Play, Share2, Trash2, MoreVertical, Grid3X3, List, Filter, SlidersHorizontal, CheckCircle2, HardDrive, Clock, FileVideo, Music, Download, Sparkles, ArrowUpRight, Eye, PenLine, FolderOpen, ChevronDown, X, Zap } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { useDownloads } from '../context/DownloadContext'

function formatDate(ts){
  const d = new Date(ts)
  const now = new Date()
  const diff = Date.now() - ts
  if(diff < 1000*60*60*24) return d.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})
  if(diff < 1000*60*60*24*7) return d.toLocaleDateString([], {weekday:'short'})
  return d.toLocaleDateString([], {month:'short', day:'numeric'})
}

export default function LibraryScreen({ onGoBrowse }){
  const { downloads, deleteDownload, clearCompleted, pushToast, storageUsed } = useDownloads()
  const [query, setQuery] = useState('')
  const [filterPlatform, setFilterPlatform] = useState('all')
  const [view, setView] = useState('grid') // grid | list
  const [sort, setSort] = useState('date') // date | size | name
  const [selected, setSelected] = useState(null)

  const completed = useMemo(()=> downloads.filter(d=> d.status==='completed'), [downloads])
  const platforms = useMemo(()=> ['all', ...Array.from(new Set(completed.map(c=> c.platform)))], [completed])

  const filtered = useMemo(()=>{
    let out = [...completed]
    if(query) out = out.filter(c=> c.title.toLowerCase().includes(query.toLowerCase()) || c.platformName.toLowerCase().includes(query.toLowerCase()))
    if(filterPlatform!=='all') out = out.filter(c=> c.platform===filterPlatform)
    if(sort==='date') out.sort((a,b)=> (b.completedAt||b.createdAt) - (a.completedAt||a.createdAt))
    if(sort==='size') out.sort((a,b)=> parseFloat(b.size) - parseFloat(a.size))
    if(sort==='name') out.sort((a,b)=> a.title.localeCompare(b.title))
    return out
  }, [completed, query, filterPlatform, sort])

  const totalSize = useMemo(()=> completed.reduce((a,c)=>{
    const v = parseFloat(c.size)
    const mult = c.size.includes('GB') ? 1024 : 1
    return a + v*mult
  }, 0), [completed])

  if(completed.length===0){
    return (
      <div className="max-w-[960px] mx-auto px-4 sm:px-6 pb-28 pt-5 space-y-5">
        <div className="rounded-[20px] bg-[#0F1423] border border-white/[0.06] p-4 flex flex-col sm:flex-row sm:items-center gap-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-white text-[#0F1423] flex items-center justify-center"><HardDrive size={18}/></div>
            <div>
              <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500">Library</div>
              <div className="text-[16px] font-extrabold">0 videos • 0 MB</div>
            </div>
          </div>
          <div className="sm:ml-auto flex items-center gap-2">
            <span className="text-[11px] font-medium px-3 py-1.5 rounded-full bg-white/10 border border-white/10 text-slate-300">No files yet</span>
          </div>
        </div>
        <div className="rounded-[24px] bg-[#0F1423] border border-white/[0.06] p-8 sm:p-12 flex flex-col items-center text-center">
          <div className="h-20 w-20 rounded-[20px] bg-white/[0.06] border border-white/10 flex items-center justify-center mb-5">
            <FolderOpen size={28} className="text-slate-400" />
          </div>
          <h3 className="text-[18px] font-extrabold">Your library is empty</h3>
          <p className="text-[13px] text-slate-400 max-w-[420px] mt-1.5 leading-relaxed">Completed downloads will appear here. Manage, play, share or delete — everything is saved locally.</p>
          <button onClick={onGoBrowse} className="mt-6 inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-[#6C5CFF] to-[#00D9FF] text-white font-bold shadow-glow">
            <Download size={16}/> Browse & Download
          </button>
          <div className="mt-8 flex gap-2 text-[11px] font-medium">
            <span className="px-3 py-1.5 rounded-full bg-[#6C5CFF]/15 border border-[#6C5CFF]/20 text-[#A99DFF]">MP4 • MKV</span>
            <span className="px-3 py-1.5 rounded-full bg-[#00D9FF]/15 border border-[#00D9FF]/20 text-[#7EE8FF]">MP3 • M4A</span>
            <span className="px-3 py-1.5 rounded-full bg-white/10 border border-white/10 text-slate-300">4K supported</span>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-[960px] mx-auto px-4 sm:px-6 pb-28 pt-5 space-y-4">
      {/* Header card */}
      <div className="rounded-[20px] bg-gradient-to-br from-[#0F1423] to-[#101A35] border border-white/[0.06] p-4 flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-[#6C5CFF] to-[#00D9FF] flex items-center justify-center text-white shadow-glow"><CheckCircle2 size={18}/></div>
            <div>
              <div className="text-[16px] font-extrabold leading-none">{completed.length} videos • {totalSize.toFixed(0)} MB</div>
              <div className="text-[11px] font-medium text-slate-400 flex items-center gap-1.5"><HardDrive size={11}/> Saved to /Downloads/Vidmax • {storageUsed.toFixed(0)} MB total</div>
            </div>
          </div>
          <div className="ml-auto flex items-center gap-2">
            <button onClick={()=>{
              if(confirm(`Clear ${completed.length} completed files?`)) clearCompleted()
            }} className="hidden sm:inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white/10 border border-white/10 text-[12px] font-semibold text-slate-300 hover:bg-white/15"><Trash2 size={14}/> Clear all</button>
            <div className="flex items-center rounded-xl bg-[#060A14] border border-white/10 p-1">
              <button onClick={()=>setView('grid')} className={`h-7 w-7 rounded-lg flex items-center justify-center transition ${view==='grid' ? 'bg-white text-[#0F1423]' : 'text-slate-400 hover:text-white'}`}><Grid3X3 size={14}/></button>
              <button onClick={()=>setView('list')} className={`h-7 w-7 rounded-lg flex items-center justify-center transition ${view==='list' ? 'bg-white text-[#0F1423]' : 'text-slate-400 hover:text-white'}`}><List size={14}/></button>
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 flex items-center gap-2 h-11 px-3.5 rounded-xl bg-[#060A14] border border-white/10 focus-within:border-[#6C5CFF]/40">
            <Search size={16} className="text-slate-500 shrink-0" />
            <input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search in library — title, platform…" className="flex-1 bg-transparent outline-none text-[13px] placeholder:text-slate-500 text-white" />
            {query && <button onClick={()=>setQuery('')} className="h-7 w-7 rounded-full bg-white/10 flex items-center justify-center text-slate-300"><X size={12}/></button>}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <div className="relative">
              <select value={sort} onChange={e=>setSort(e.target.value)} className="appearance-none h-11 pl-3 pr-8 rounded-xl bg-[#060A14] border border-white/10 text-[13px] font-semibold text-slate-200 outline-none">
                <option value="date">Newest first</option>
                <option value="size">Largest first</option>
                <option value="name">Name A–Z</option>
              </select>
              <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
            </div>
            <button className="h-11 w-11 rounded-xl bg-[#060A14] border border-white/10 flex items-center justify-center text-slate-400"><SlidersHorizontal size={16}/></button>
          </div>
        </div>

        <div className="flex items-center gap-2 overflow-auto no-scrollbar">
          {platforms.map(p=>(
            <button key={p} onClick={()=>setFilterPlatform(p)} className={`whitespace-nowrap px-3.5 py-2 rounded-full text-[12px] font-bold border capitalize transition ${filterPlatform===p ? 'bg-white text-[#0F1423] border-white' : 'bg-white/[0.06] text-slate-300 border-white/10 hover:bg-white/10'}`}>
              {p==='all' ? `All • ${completed.length}` : `${p} • ${completed.filter(c=>c.platform===p).length}`}
            </button>
          ))}
          <span className="ml-auto hidden sm:inline text-[11px] font-medium text-slate-500">{filtered.length} results</span>
        </div>
      </div>

      {/* Grid / List */}
      {view==='grid' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {filtered.map(item=>(
            <motion.div key={item.id} layout initial={{opacity:0, scale:0.98}} animate={{opacity:1, scale:1}} className="group rounded-2xl bg-[#0F1423] border border-white/[0.06] overflow-hidden hover:border-white/15 transition shadow-card flex flex-col">
              <div className="relative">
                <img src={item.thumbnail} alt="" className="h-[168px] w-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/0 to-transparent" />
                <span className="absolute top-2 left-2 text-[10px] font-extrabold tracking-widest uppercase px-2 py-1 rounded-full bg-white text-[#0F1423]">{item.platformName}</span>
                <span className="absolute top-2 right-2 text-[11px] font-bold px-2 py-1 rounded-full bg-black/70 backdrop-blur border border-white/15 text-white">{item.duration}</span>
                <span className="absolute bottom-2 left-2 inline-flex items-center gap-1 text-[11px] font-bold px-2 py-1 rounded-full bg-[#00D9FF] text-[#06101A]"><CheckCircle2 size={12}/> {item.quality}</span>
                <button
                  onClick={()=>setSelected(item)}
                  className="absolute bottom-2 right-2 h-8 w-8 rounded-full bg-white text-[#0F1423] flex items-center justify-center shadow opacity-0 group-hover:opacity-100 transition"
                >
                  <Play size={14} className="fill-[#0F1423] ml-0.5" />
                </button>
              </div>
              <div className="p-3 flex-1 flex flex-col">
                <h3 className="text-[13px] font-bold leading-snug line-clamp-2 min-h-[36px]">{item.title}</h3>
                <div className="flex items-center gap-2 mt-2 text-[11px] text-slate-400">
                  <span className="inline-flex items-center gap-1.5"><FileVideo size={12}/> {item.format.toUpperCase()}</span>
                  <span className="h-1 w-1 rounded-full bg-white/20" />
                  <span>{item.size}</span>
                  <span className="h-1 w-1 rounded-full bg-white/20" />
                  <span className="flex items-center gap-1"><Clock size={11}/> {formatDate(item.completedAt || item.createdAt)}</span>
                </div>
                <div className="mt-3 flex items-center gap-1.5">
                  <button onClick={()=>setSelected(item)} className="flex-1 inline-flex items-center justify-center gap-1.5 py-2 rounded-xl bg-white text-[#0F1423] text-[12px] font-bold hover:bg-slate-100 transition"><Play size={12} className="fill-[#0F1423]" /> Play</button>
                  <button onClick={()=>{pushToast('Shared — link copied','success')}} className="h-9 w-9 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center text-slate-300 hover:bg-white/15 transition"><Share2 size={14}/></button>
                  <button onClick={()=>deleteDownload(item.id)} className="h-9 w-9 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center text-slate-300 hover:bg-red-500/20 hover:text-red-300 hover:border-red-500/30 transition"><Trash2 size={14}/></button>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map(item=>(
            <div key={item.id} className="rounded-2xl bg-[#0F1423] border border-white/[0.06] p-3 flex gap-3 hover:border-white/10 transition">
              <img src={item.thumbnail} alt="" className="h-[86px] w-[152px] object-cover rounded-xl border border-white/10 shrink-0" />
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="text-[13px] font-bold leading-snug line-clamp-2">{item.title}</h3>
                  <button onClick={()=>setSelected(item)} className="hidden sm:flex h-8 w-8 rounded-full bg-white text-[#0F1423] items-center justify-center shrink-0"><MoreVertical size={14}/></button>
                </div>
                <div className="text-[11px] text-slate-400 mt-1 flex items-center gap-2 flex-wrap">
                  <span className="px-2 py-1 rounded-full bg-white/10 border border-white/10 text-slate-200 font-semibold">{item.platformName}</span>
                  <span>{item.quality} • {item.format.toUpperCase()} • {item.size}</span>
                  <span className="hidden sm:inline-flex items-center gap-1"><Clock size={11}/> {formatDate(item.completedAt||item.createdAt)}</span>
                </div>
                <div className="mt-2.5 flex items-center gap-2">
                  <button onClick={()=>setSelected(item)} className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-[#6C5CFF] text-white text-[12px] font-bold hover:bg-[#5A4AE6] transition"><Play size={12} className="fill-white"/> Play</button>
                  <button onClick={()=>pushToast('Opening folder…','info')} className="hidden sm:inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white/10 border border-white/10 text-[12px] font-semibold text-white hover:bg-white/15"><FolderOpen size={12}/> Show in folder</button>
                  <button onClick={()=>pushToast('Link copied','success')} className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white/10 border border-white/10 text-[12px] font-semibold text-white hover:bg-white/15"><Share2 size={12}/> Share</button>
                  <button onClick={()=>deleteDownload(item.id)} className="ml-auto h-8 w-8 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-slate-400 hover:text-red-300"><Trash2 size={14}/></button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {filtered.length===0 && completed.length>0 && (
        <div className="rounded-2xl bg-white/[0.04] border border-dashed border-white/15 p-10 text-center">
          <div className="text-[13px] font-bold text-slate-300">No results for “{query}”</div>
          <div className="text-[12px] text-slate-500">Try a different search or filter</div>
          <button onClick={()=>{setQuery(''); setFilterPlatform('all')}} className="mt-3 px-4 py-2 rounded-xl bg-white text-[#0F1423] text-[12px] font-bold">Clear filters</button>
        </div>
      )}

      {/* Detail drawer */}
      <AnimatePresence>
        {selected && (
          <>
            <motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} onClick={()=>setSelected(null)} className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm" />
            <motion.div initial={{y:'100%'}} animate={{y:0}} exit={{y:'100%'}} transition={{type:'spring', damping:28, stiffness:300}} className="fixed bottom-0 inset-x-0 z-50 max-w-[560px] mx-auto bg-[#0F1423] rounded-t-[24px] border border-white/10 shadow-2xl overflow-hidden max-h-[88vh] flex flex-col">
              <div className="h-1.5 w-10 bg-white/20 rounded-full mx-auto mt-3" />
              <div className="relative">
                <img src={selected.thumbnail} alt="" className="h-[220px] w-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-t from-[#0F1423] via-black/10 to-transparent" />
                <button onClick={()=>setSelected(null)} className="absolute top-3 right-3 h-8 w-8 rounded-full bg-black/60 backdrop-blur border border-white/15 flex items-center justify-center text-white"><X size={16}/></button>
                <div className="absolute bottom-3 left-4 right-4">
                  <div className="inline-flex items-center gap-2 text-[11px] font-bold px-2.5 py-1 rounded-full bg-[#00D9FF] text-[#06101A]"><CheckCircle2 size={12}/> Completed • {selected.quality}</div>
                  <h3 className="text-[16px] font-extrabold leading-tight text-white mt-2 drop-shadow">{selected.title}</h3>
                  <div className="text-[12px] text-white/80 mt-1 flex items-center gap-2"><span>{selected.platformName}</span><span className="h-1 w-1 rounded-full bg-white/50"/><span>{selected.size} • {selected.duration}</span></div>
                </div>
              </div>
              <div className="p-4 space-y-3 overflow-auto no-scrollbar">
                <div className="grid grid-cols-3 gap-2">
                  <button className="flex flex-col items-center gap-2 p-3 rounded-2xl bg-white text-[#0F1423] font-bold hover:bg-slate-100 transition"><Play size={18} className="fill-[#0F1423]" /> Play</button>
                  <button onClick={()=>pushToast('Shared — link copied','success')} className="flex flex-col items-center gap-2 p-3 rounded-2xl bg-white/10 border border-white/10 text-white font-semibold hover:bg-white/15 transition"><Share2 size={18}/> Share</button>
                  <button onClick={()=>{deleteDownload(selected.id); setSelected(null)}} className="flex flex-col items-center gap-2 p-3 rounded-2xl bg-red-500/10 border border-red-500/20 text-red-300 font-semibold hover:bg-red-500/20 transition"><Trash2 size={18}/> Delete</button>
                </div>

                <div className="rounded-2xl bg-[#060A14] border border-white/10 p-3 space-y-2.5">
                  <div className="flex justify-between text-[12px]"><span className="text-slate-400 flex items-center gap-1.5"><HardDrive size={12}/> Location</span><span className="font-medium text-slate-200">/Downloads/Vidmax</span></div>
                  <div className="flex justify-between text-[12px]"><span className="text-slate-400 flex items-center gap-1.5"><FileVideo size={12}/> Format</span><span className="font-medium text-slate-200">{selected.format.toUpperCase()} • {selected.quality} • {selected.size}</span></div>
                  <div className="flex justify-between text-[12px]"><span className="text-slate-400 flex items-center gap-1.5"><Clock size={12}/> Saved</span><span className="font-medium text-slate-200">{new Date(selected.completedAt||selected.createdAt).toLocaleString()}</span></div>
                  <div className="flex justify-between text-[12px]"><span className="text-slate-400 flex items-center gap-1.5"><Eye size={12}/> Source</span><a href={selected.url} target="_blank" rel="noreferrer" className="font-medium text-[#00D9FF] inline-flex items-center gap-1 hover:underline">Open original <ArrowUpRight size={12}/></a></div>
                </div>

                <div className="flex gap-2">
                  <button onClick={()=>pushToast('Renamed — demo','info')} className="flex-1 inline-flex items-center justify-center gap-2 py-3 rounded-xl bg-white/10 border border-white/10 text-white font-semibold"><PenLine size={14}/> Rename</button>
                  <button onClick={()=>pushToast('Opening folder…','info')} className="flex-1 inline-flex items-center justify-center gap-2 py-3 rounded-xl bg-white text-[#0F1423] font-bold"><FolderOpen size={14}/> Show in Files</button>
                </div>

                <div className="rounded-xl bg-[#6C5CFF]/10 border border-[#6C5CFF]/20 p-3 flex gap-2.5">
                  <div className="h-8 w-8 rounded-xl bg-[#6C5CFF] flex items-center justify-center text-white shrink-0"><Zap size={14}/></div>
                  <div>
                    <div className="text-[12px] font-bold text-white">Keep your engine — same yt-dlp core</div>
                    <div className="text-[11px] text-slate-400 leading-snug">This UI is decoupled from the download engine. Replace mockFetchMetadata with your real backend — no screen changes needed.</div>
                  </div>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  )
}

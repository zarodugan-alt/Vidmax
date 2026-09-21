import { useState } from 'react'
import Header from './components/Header'
import BottomNav from './components/BottomNav'
import BrowserScreen from './screens/BrowserScreen'
import ProgressScreen from './screens/ProgressScreen'
import LibraryScreen from './screens/LibraryScreen'
import { useDownloads } from './context/DownloadContext'
import { motion, AnimatePresence } from 'framer-motion'
import { X, ShieldCheck, Zap, HardDrive, Wifi, Bell, Moon, Sun, Smartphone, Info, ChevronRight, ExternalLink } from 'lucide-react'

function SettingsDrawer({ open, onClose, storagePercent, completedCount }){
  const [wifiOnly, setWifiOnly] = useState(false)
  const [notifications, setNotifications] = useState(true)
  const [darkMode, setDarkMode] = useState(true)
  return (
    <AnimatePresence>
      {open && (
        <>
          <motion.div initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} onClick={onClose} className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm" />
          <motion.div initial={{x:'100%'}} animate={{x:0}} exit={{x:'100%'}} transition={{type:'spring', damping:28, stiffness:300}} className="fixed right-0 top-0 bottom-0 z-50 w-[360px] max-w-[86vw] bg-[#0F1423] border-l border-white/10 shadow-2xl flex flex-col">
            <div className="h-[64px] flex items-center justify-between px-5 border-b border-white/10 shrink-0">
              <div className="font-extrabold">Settings</div>
              <button onClick={onClose} className="h-8 w-8 rounded-full bg-white/10 flex items-center justify-center text-slate-300"><X size={16}/></button>
            </div>
            <div className="flex-1 overflow-auto p-5 space-y-5 no-scrollbar">
              <div className="rounded-2xl bg-gradient-to-br from-[#6C5CFF] to-[#00D9FF] p-4 text-white">
                <div className="h-8 w-8 rounded-xl bg-white/20 flex items-center justify-center mb-3"><Zap size={16}/></div>
                <div className="font-extrabold">Vidmax Pro</div>
                <div className="text-[12px] text-white/80">Engine v2.4.1 • yt-dlp core • Fast mux • No limits</div>
                <div className="mt-3 inline-flex items-center gap-2 text-[11px] font-bold px-2.5 py-1 rounded-full bg-white text-[#4F46E5]">PRO • Lifetime</div>
              </div>

              <div>
                <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500 mb-2">Storage</div>
                <div className="rounded-2xl bg-[#060A14] border border-white/10 p-4">
                  <div className="flex justify-between text-[13px] font-bold"><span className="flex items-center gap-2"><HardDrive size={14}/> 64 GB device</span><span className="text-[#00D9FF]">{storagePercent.toFixed(1)}% used</span></div>
                  <div className="h-2 w-full bg-white/10 rounded-full overflow-hidden mt-2"><div className="h-full bg-gradient-to-r from-[#6C5CFF] to-[#00D9FF]" style={{width:`${Math.min(100, storagePercent*3)}%`}} /></div>
                  <div className="text-[11px] text-slate-500 mt-1.5">{completedCount} files • Auto-clean off • /Downloads/Vidmax</div>
                </div>
              </div>

              <div>
                <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500 mb-2">Preferences</div>
                <div className="rounded-2xl bg-[#060A14] border border-white/10 divide-y divide-white/5 overflow-hidden">
                  {[
                    {icon: Wifi, label:'Wi-Fi only downloads', desc:'Pause on mobile data', value:wifiOnly, setter:setWifiOnly},
                    {icon: Bell, label:'Download notifications', desc:'Show progress & done', value:notifications, setter:setNotifications},
                    {icon: Moon, label:'Dark theme', desc:'Professional OLED black', value:darkMode, setter:setDarkMode},
                  ].map(row=>{
                    const Icon=row.icon
                    return (
                      <div key={row.label} className="flex items-center gap-3 p-4">
                        <div className="h-9 w-9 rounded-xl bg-white/10 flex items-center justify-center text-slate-300"><Icon size={16}/></div>
                        <div className="flex-1">
                          <div className="text-[13px] font-bold">{row.label}</div>
                          <div className="text-[11px] text-slate-500">{row.desc}</div>
                        </div>
                        <button onClick={()=>row.setter(v=>!v)} className={`relative h-6 w-11 rounded-full transition ${row.value? 'bg-[#6C5CFF]' : 'bg-white/15'}`}>
                          <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition ${row.value? 'right-0.5' : 'left-0.5'}`} />
                        </button>
                      </div>
                    )
                  })}
                </div>
              </div>

              <div>
                <div className="text-[11px] font-bold tracking-widest uppercase text-slate-500 mb-2">About</div>
                <div className="rounded-2xl bg-[#060A14] border border-white/10 p-4 space-y-3">
                  <div className="flex items-center justify-between text-[13px]"><span className="text-slate-400 flex items-center gap-2"><Smartphone size={14}/> Version</span><span className="font-mono font-bold">2.4.1 (Build 418)</span></div>
                  <div className="flex items-center justify-between text-[13px]"><span className="text-slate-400 flex items-center gap-2"><ShieldCheck size={14}/> Engine</span><span className="font-bold">yt-dlp • ffmpeg</span></div>
                  <button className="w-full mt-1 flex items-center justify-between p-3 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition text-[12px] font-semibold"><span className="flex items-center gap-2"><Info size={14}/> Privacy & Terms</span><ChevronRight size={14}/></button>
                </div>
              </div>
            </div>
            <div className="p-4 border-t border-white/10">
              <a href="https://github.com" target="_blank" rel="noreferrer" className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-white text-[#0F1423] font-bold">View on GitHub <ExternalLink size={14}/></a>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}

function ToastStack(){
  const { toasts } = useDownloads()
  return (
    <div className="fixed top-[72px] inset-x-0 z-50 flex flex-col items-center gap-2 px-4 pointer-events-none">
      <AnimatePresence>
        {toasts.map(t=>(
          <motion.div key={t.id} initial={{opacity:0, y:-10, scale:0.98}} animate={{opacity:1, y:0, scale:1}} exit={{opacity:0, y:-10}} className={`pointer-events-auto px-4 py-3 rounded-2xl shadow-xl border text-[13px] font-semibold flex items-center gap-2 ${t.type==='success' ? 'bg-[#0F1F1A] border-emerald-500/30 text-emerald-100' : t.type==='error' ? 'bg-[#1F1212] border-red-500/30 text-red-100' : 'bg-[#0F1423] border-white/15 text-white'} backdrop-blur-xl`}>
            {t.type==='success' ? <ShieldCheck size={16} className="text-emerald-400" /> : <Zap size={16} className="text-[#00D9FF]" />}
            {t.msg}
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  )
}

export default function App(){
  const [tab, setTab] = useState('browser')
  const [settingsOpen, setSettingsOpen] = useState(false)
  const { activeCount, storagePercent, completedCount } = useDownloads()

  return (
    <div className="min-h-screen bg-[#060A14] text-white selection:bg-[#6C5CFF]/30">
      {/* Background glows */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute inset-0 bg-[#060A14]" />
        <div className="absolute top-[-120px] left-1/2 -translate-x-1/2 h-[600px] w-[900px] bg-[#6C5CFF]/10 blur-[80px] rounded-full" />
        <div className="absolute top-[300px] right-[-200px] h-[500px] w-[500px] bg-[#00D9FF]/5 blur-[70px] rounded-full" />
      </div>

      <div className="relative">
        <Header onSettings={()=>setSettingsOpen(true)} storagePercent={storagePercent} completedCount={completedCount} />
        <ToastStack />
        <SettingsDrawer open={settingsOpen} onClose={()=>setSettingsOpen(false)} storagePercent={storagePercent} completedCount={completedCount} />

        <main>
          <AnimatePresence mode="wait">
            {tab==='browser' && (
              <motion.div key="browser" initial={{opacity:0, y:8}} animate={{opacity:1, y:0}} exit={{opacity:0, y:-8}} transition={{duration:0.2}}>
                <BrowserScreen onNavigateProgress={()=>setTab('progress')} />
              </motion.div>
            )}
            {tab==='progress' && (
              <motion.div key="progress" initial={{opacity:0, y:8}} animate={{opacity:1, y:0}} exit={{opacity:0, y:-8}} transition={{duration:0.2}}>
                <ProgressScreen onGoBrowse={()=>setTab('browser')} />
              </motion.div>
            )}
            {tab==='library' && (
              <motion.div key="library" initial={{opacity:0, y:8}} animate={{opacity:1, y:0}} exit={{opacity:0, y:-8}} transition={{duration:0.2}}>
                <LibraryScreen onGoBrowse={()=>setTab('browser')} />
              </motion.div>
            )}
          </AnimatePresence>
        </main>

        <BottomNav active={tab} onChange={setTab} activeCount={activeCount} />

        {/* Desktop footer */}
        <div className="hidden lg:block border-t border-white/[0.06] bg-[#060A14]/80 backdrop-blur">
          <div className="max-w-[960px] mx-auto px-6 py-4 flex items-center justify-between text-[11px] font-medium text-slate-500">
            <span>© 2026 Vidmax • Professional video downloader • Engine: yt-dlp + ffmpeg • Theme: Production dark</span>
            <span className="flex items-center gap-2"><ShieldCheck size={12} className="text-emerald-500" /> No ads • No watermark • Privacy first</span>
          </div>
        </div>
      </div>
    </div>
  )
}

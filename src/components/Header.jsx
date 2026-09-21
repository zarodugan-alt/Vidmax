import { Zap, Search, Bell, Settings2, HardDrive, Sparkles } from 'lucide-react'

export default function Header({ onSettings, storagePercent, completedCount }){
  return (
    <header className="sticky top-0 z-30 backdrop-blur-xl bg-[#060A14]/80 border-b border-white/[0.06]">
      <div className="max-w-[960px] mx-auto px-4 sm:px-6 h-[64px] flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-[#6C5CFF] to-[#00D9FF] flex items-center justify-center shadow-glow shrink-0">
            <Zap size={18} className="text-white fill-white" />
          </div>
          <div className="leading-tight">
            <div className="flex items-center gap-2">
              <span className="font-extrabold tracking-tight text-[17px]">Vidmax</span>
              <span className="hidden sm:inline-flex items-center gap-1 text-[10px] font-bold tracking-widest uppercase px-2 py-1 rounded-full bg-white/10 border border-white/10 text-white/80">
                <Sparkles size={10} /> Pro
              </span>
            </div>
            <div className="text-[11px] font-medium text-slate-400 -mt-0.5 hidden sm:block tracking-wide">Professional Downloader • v2.4.1</div>
          </div>
        </div>

        <div className="flex items-center gap-2 sm:gap-3">
          <div className="hidden lg:flex items-center gap-3 pl-3 pr-3 py-1.5 rounded-full bg-white/[0.06] border border-white/[0.08]">
            <div className="h-7 w-7 rounded-full bg-[#161E36] flex items-center justify-center border border-white/10">
              <HardDrive size={14} className="text-slate-300" />
            </div>
            <div className="leading-tight pr-1">
              <div className="text-[11px] font-semibold text-slate-200">{storagePercent.toFixed(1)}% used</div>
              <div className="text-[10px] text-slate-400">{completedCount} files • Auto-clean off</div>
            </div>
            <div className="h-1.5 w-16 bg-white/10 rounded-full overflow-hidden ml-1">
              <div className="h-full bg-gradient-to-r from-[#6C5CFF] to-[#00D9FF]" style={{width: `${Math.min(100, storagePercent*6)}%`}} />
            </div>
          </div>

          <button className="hidden sm:flex h-9 w-9 items-center justify-center rounded-full bg-white/[0.06] border border-white/10 text-slate-300 hover:bg-white/10 transition">
            <Search size={16} />
          </button>
          <button className="hidden sm:flex h-9 w-9 items-center justify-center rounded-full bg-white/[0.06] border border-white/10 text-slate-300 hover:bg-white/10 transition relative">
            <Bell size={16} />
            <span className="absolute top-1.5 right-1.5 h-2 w-2 bg-[#00D9FF] rounded-full border-2 border-[#060A14]" />
          </button>
          <button onClick={onSettings} className="h-9 w-9 flex items-center justify-center rounded-full bg-white text-[#060A14] hover:bg-slate-100 transition shadow">
            <Settings2 size={16} />
          </button>
        </div>
      </div>
    </header>
  )
}

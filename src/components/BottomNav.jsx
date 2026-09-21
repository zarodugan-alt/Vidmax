import { motion } from 'framer-motion'
import { Globe, Download, FolderDown, Search } from 'lucide-react'

const tabs = [
  { id: 'browser', label: 'Browser', icon: Globe, short: 'Browse' },
  { id: 'progress', label: 'Downloads', icon: Download, short: 'Progress' },
  { id: 'library', label: 'Library', icon: FolderDown, short: 'Library' },
]

export default function BottomNav({ active, onChange, activeCount }){
  return (
    <div className="fixed bottom-0 inset-x-0 z-40 flex justify-center pb-safe pointer-events-none">
      <div className="pointer-events-auto w-full max-w-[560px] mx-auto px-4 pb-3">
        <div className="glass rounded-[22px] p-1.5 flex items-center justify-between shadow-glass border border-white/[0.08] backdrop-blur-2xl">
          {tabs.map(tab=>{
            const isActive = active === tab.id
            const Icon = tab.icon
            return (
              <button
                key={tab.id}
                onClick={()=>onChange(tab.id)}
                className={`relative flex-1 flex items-center justify-center gap-2.5 py-3.5 rounded-[16px] text-[13px] font-semibold transition-all ${isActive ? 'text-white' : 'text-slate-400 hover:text-slate-200'}`}
              >
                {isActive && (
                  <motion.div
                    layoutId="active-pill"
                    className="absolute inset-0 bg-gradient-to-br from-[#6C5CFF] to-[#4F46E5] rounded-[16px] shadow-glow"
                    transition={{ type:'spring', bounce:0.2, duration:0.5 }}
                  />
                )}
                <span className="relative flex items-center gap-2.5">
                  <span className={`relative ${isActive ? '' : 'opacity-80'}`}>
                    <Icon size={18} strokeWidth={isActive? 2.4 : 1.9} />
                    {tab.id==='progress' && activeCount>0 && (
                      <span className="absolute -top-2 -right-2 bg-[#00D9FF] text-[#06101A] text-[10px] leading-none font-extrabold px-1.5 py-1 rounded-full min-w-[18px] text-center border-2 border-[#0F1423] ">
                        {activeCount}
                      </span>
                    )}
                  </span>
                  <span className="hidden sm:inline tracking-wide">{tab.label}</span>
                  <span className="sm:hidden tracking-wide">{tab.short}</span>
                </span>
              </button>
            )
          })}
        </div>
        {/* home indicator */}
        <div className="flex justify-center mt-3">
          <div className="h-[5px] w-[134px] bg-white/80 rounded-full hidden sm:block opacity-0" />
          <div className="h-[5px] w-[100px] bg-white/30 rounded-full sm:hidden" />
        </div>
      </div>
    </div>
  )
}

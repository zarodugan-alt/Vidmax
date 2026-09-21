const platforms = [
  { name: 'YouTube', handle: 'youtube.com', color: '#FF0033', icon: '▶', bg: 'from-[#FF0033]/20 to-[#FF0033]/5' },
  { name: 'TikTok', handle: 'tiktok.com', color: '#000000', icon: '♪', bg: 'from-white/15 to-white/5', iconBg: 'bg-white text-black' },
  { name: 'Instagram', handle: 'instagram.com', color: '#E4405F', icon: '◎', bg: 'from-[#E4405F]/20 to-[#833AB4]/10' },
  { name: 'Facebook', handle: 'facebook.com', color: '#1877F2', icon: 'f', bg: 'from-[#1877F2]/20 to-[#1877F2]/5' },
  { name: 'Twitter / X', handle: 'x.com', color: '#FFFFFF', icon: '𝕏', bg: 'from-white/10 to-white/[0.03]' },
  { name: 'Vimeo', handle: 'vimeo.com', color: '#1AB7EA', icon: 'V', bg: 'from-[#1AB7EA]/20 to-[#1AB7EA]/5' },
  { name: 'Reddit', handle: 'reddit.com', color: '#FF4500', icon: 'R', bg: 'from-[#FF4500]/20 to-[#FF4500]/5' },
  { name: 'Dailymotion', handle: 'dailymotion.com', color: '#0066DC', icon: 'D', bg: 'from-[#0066DC]/20 to-[#0066DC]/5' },
]

export default function PlatformGrid({ onSelect }){
  return (
    <div className="grid grid-cols-4 sm:grid-cols-8 gap-2 sm:gap-3">
      {platforms.map(p=> (
        <button
          key={p.name}
          onClick={()=>onSelect?.(p)}
          className="group flex flex-col items-center gap-2 p-3 rounded-2xl bg-[#0F1423] border border-white/[0.06] hover:border-[#6C5CFF]/30 hover:bg-[#161E36] transition text-center"
        >
          <div className={`h-11 w-11 rounded-2xl bg-gradient-to-br ${p.bg} border border-white/[0.06] flex items-center justify-center text-[15px] font-black tracking-tight shadow-sm group-hover:scale-105 transition ${p.iconBg || 'text-white'}`} style={{color: p.iconBg? undefined : p.color}}>
            {p.icon}
          </div>
          <div>
            <div className="text-[11px] font-bold leading-none text-slate-100">{p.name.split(' ')[0]}</div>
            <div className="text-[10px] text-slate-500 hidden sm:block">{p.handle}</div>
          </div>
        </button>
      ))}
    </div>
  )
}

// Vidmax Download Engine
// Abstracted engine - keep this layer stable. UI never talks to yt-dlp directly.
// Swap mockFetchMetadata / mockStartDownload with real backend when available.

export const PLATFORMS = {
  youtube: { name: 'YouTube', color: '#FF0000', domain: 'youtube.com', short: 'YT' },
  youtu_be: { name: 'YouTube', color: '#FF0000', domain: 'youtu.be', short: 'YT' },
  tiktok: { name: 'TikTok', color: '#000000', domain: 'tiktok.com', short: 'TT' },
  instagram: { name: 'Instagram', color: '#E4405F', domain: 'instagram.com', short: 'IG' },
  facebook: { name: 'Facebook', color: '#1877F2', domain: 'facebook.com', short: 'FB' },
  twitter: { name: 'X / Twitter', color: '#000000', domain: 'twitter.com', short: 'X' },
  x_com: { name: 'X / Twitter', color: '#000000', domain: 'x.com', short: 'X' },
  vimeo: { name: 'Vimeo', color: '#1AB7EA', domain: 'vimeo.com', short: 'VM' },
  dailymotion: { name: 'Dailymotion', color: '#0066DC', domain: 'dailymotion.com', short: 'DM' },
  reddit: { name: 'Reddit', color: '#FF4500', domain: 'reddit.com', short: 'RD' },
}

export function detectPlatform(url) {
  try {
    const u = new URL(url)
    const host = u.hostname.replace('www.', '').toLowerCase()
    for (const [key, p] of Object.entries(PLATFORMS)) {
      if (host.includes(p.domain)) return key
    }
    return 'generic'
  } catch { return 'generic' }
}

export function isValidUrl(string) {
  try {
    const url = new URL(string)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch { return false }
}

export function getPlatformInfo(platformKey) {
  return PLATFORMS[platformKey] || { name: 'Web Video', color: '#6C5CFF', domain: 'generic', short: 'WEB' }
}

// Mock metadata database for demo - simulates yt-dlp --dump-json
const MOCK_TITLES = [
  "Learn UI Design in 12 Minutes - Complete Beginner Guide",
  "Lo-fi Hip Hop Radio — Beats to Relax/Study to [LIVE 24/7]",
  "I Built a $0 Startup to $10M — Here's How (Documentary)",
  "The Future of AI is Here — OpenAI's Latest Breakthrough",
  "Calisthenics For Beginners — Full Body Workout (No Equipment)",
  "Mix — Best of Deep House 2024 | Chill Mix",
  "How I Mastered Figma in 30 Days — Time-lapse",
  "Sunset Timelapse 4K — Maldives Drone Footage",
  "Funny Cat Compilation — Try Not To Laugh Challenge",
  "Chill Vibes Mix — Summer 2024 Playlist"
]
const THUMBS = [
  "https://images.unsplash.com/photo-1611224923853-80b023f02d71?w=640&h=360&fit=crop",
  "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=640&h=360&fit=crop",
  "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=640&h=360&fit=crop",
  "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=640&h=360&fit=crop",
  "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=640&h=360&fit=crop",
  "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=640&h=360&fit=crop",
  "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=640&h=360&fit=crop",
  "https://images.unsplash.com/photo-1526379095098-d400fd0bf935?w=640&h=360&fit=crop",
]

function randomPick(arr){ return arr[Math.floor(Math.random()*arr.length)] }

export async function mockFetchMetadata(url) {
  // Simulate network latency 800-1400ms and occasional failure
  await new Promise(r => setTimeout(r, 800 + Math.random()*600))
  if (Math.random() < 0.05) throw new Error("Unable to fetch video info. Check link or try again.")
  const platform = detectPlatform(url)
  const info = getPlatformInfo(platform)
  const isAudioOnly = false // will be selected by user later
  return {
    id: Math.random().toString(36).slice(2, 9),
    url,
    platform,
    platformName: info.name,
    title: randomPick(MOCK_TITLES),
    uploader: platform === 'youtube' ? 'Vidmax Studio' : '@creator.handle',
    thumbnail: randomPick(THUMBS),
    duration: `${Math.floor(1+Math.random()*59)}:${String(Math.floor(Math.random()*60)).padStart(2,'0')}`,
    durationSeconds:  Math.floor(80 + Math.random()* 1200),
    viewCount: `${(Math.random()*10).toFixed(1)}M views`,
    formats: [
      { id: '2160p', label: '4K  •  2160p', ext: 'mp4', size: '1.2 GB', fps: 60 },
      { id: '1440p', label: '2K  •  1440p', ext: 'mp4', size: '720 MB', fps: 60 },
      { id: '1080p', label: 'Full HD  •  1080p', ext: 'mp4', size: '420 MB', fps: 60, recommended: true },
      { id: '720p', label: 'HD  •  720p', ext: 'mp4', size: '210 MB', fps: 30 },
      { id: '480p', label: 'SD  •  480p', ext: 'mp4', size: '95 MB', fps: 30 },
      { id: 'mp3', label: 'Audio  •  MP3 320kbps', ext: 'mp3', size: '14 MB', audioOnly: true },
      { id: 'm4a', label: 'Audio  •  M4A 128kbps', ext: 'm4a', size: '8 MB', audioOnly: true },
    ]
  }
}

export const DEFAULT_QUALITY = '1080p'

// File size parser (for mock speed/eta)
export function parseSizeToMB(sizeStr){
  const [val, unit] = sizeStr.split(' ')
  const n = parseFloat(val)
  if(unit === 'GB') return n*1024
  if(unit === 'MB') return n
  if(unit === 'KB') return n/1024
  return n
}

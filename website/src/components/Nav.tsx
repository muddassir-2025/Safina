import { Link } from 'react-router-dom'

export default function Nav() {
  return (
    <header className="sticky top-0 z-50 backdrop-blur bg-slate-950/70 border-b border-slate-800/60">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 font-semibold tracking-tight">
          <span className="w-8 h-8 rounded-full bg-teal-500/20 border border-teal-400/40 flex items-center justify-center text-teal-300">S</span>
          <span className="text-lg">Safina</span>
        </Link>
        <nav className="flex items-center gap-6 text-sm text-slate-300">
          <a href="#features" className="hover:text-white transition">Features</a>
          <a href="#scenes" className="hover:text-white transition">Scenes</a>
          <Link to="/privacy" className="hover:text-white transition">Privacy</Link>
          <a
            href="#download"
            className="hidden sm:inline-flex px-4 py-2 rounded-full bg-teal-500 text-slate-950 font-medium hover:bg-teal-400 transition"
          >
            Download
          </a>
        </nav>
      </div>
    </header>
  )
}

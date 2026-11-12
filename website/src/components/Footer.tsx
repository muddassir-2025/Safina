export default function Footer() {
  return (
    <footer className="border-t border-slate-800/60 mt-24">
      <div className="max-w-6xl mx-auto px-6 py-10 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm text-slate-400">
        <p>© {new Date().getFullYear()} Safina. Count dhikr in peace.</p>
        <div className="flex gap-6">
          <a href="#download" className="hover:text-white transition">Download</a>
          <a href="/privacy" className="hover:text-white transition">Privacy Policy</a>
        </div>
      </div>
    </footer>
  )
}

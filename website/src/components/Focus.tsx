export default function Focus() {
  return (
    <section className="max-w-6xl mx-auto px-6 py-20">
      <div className="text-center max-w-2xl mx-auto">
        <p className="text-teal-300 text-sm tracking-[0.2em] uppercase mb-3">Made for focus</p>
        <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight">
          A clean, gentle interface.
        </h2>
        <p className="mt-4 text-slate-400 leading-relaxed">
          Large Arabic text, soft colours and nothing to distract you from your remembrance.
        </p>
      </div>
      <div className="mt-12 grid sm:grid-cols-3 gap-4 max-w-4xl mx-auto">
        {[
          { t: 'Large Arabic', d: 'Clear, readable Arabic with English meaning.' },
          { t: 'Soft colours', d: 'Calm palettes that never tire the eyes.' },
          { t: 'No clutter', d: 'Just the count and the scene.' },
        ].map((c) => (
          <div key={c.t} className="rounded-2xl border border-slate-800 p-6 bg-slate-900/30">
            <h3 className="font-medium">{c.t}</h3>
            <p className="text-sm text-slate-400 mt-2">{c.d}</p>
          </div>
        ))}
      </div>
    </section>
  )
}

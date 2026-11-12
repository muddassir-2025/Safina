export default function Studio() {
  return (
    <section className="max-w-6xl mx-auto px-6 py-20">
      <div className="rounded-3xl border border-slate-800 bg-slate-900/40 p-8 sm:p-12 grid md:grid-cols-2 gap-8 items-center">
        <div>
          <p className="text-teal-300 text-sm tracking-[0.2em] uppercase mb-3">Scene Studio</p>
          <h2 className="text-3xl font-semibold tracking-tight">Make it yours.</h2>
          <p className="mt-4 text-slate-400 leading-relaxed">
            Pick from the built-in scenes, upload your own private scenes, or explore public scenes shared
            by the community and save them to your device.
          </p>
        </div>
        <div className="aspect-video rounded-2xl overflow-hidden border border-slate-800">
          <img src="/photos/studio.jpeg" alt="Scene Studio" className="w-full h-full object-cover" loading="lazy" />
        </div>
      </div>
    </section>
  )
}

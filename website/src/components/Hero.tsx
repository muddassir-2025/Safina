export default function Hero() {
  return (
    <section className="relative overflow-hidden">
      <div className="absolute inset-0 -z-10">
        <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-[40rem] h-[40rem] rounded-full bg-teal-500/10 blur-3xl" />
      </div>
      <div className="max-w-6xl mx-auto px-6 pt-24 pb-20 text-center">
        <p className="text-teal-300 text-sm tracking-[0.2em] uppercase mb-4">Dhikr counter</p>
        <h1 className="text-5xl sm:text-7xl font-semibold tracking-tight">
          Safina
        </h1>
        <p className="mt-6 text-xl sm:text-2xl text-slate-300 max-w-2xl mx-auto">
          Count dhikr in peace with calm, living scenes — garden, winter, ocean &amp; night.
        </p>
        <p className="mt-4 text-slate-400 max-w-xl mx-auto">
          A calm, beautiful dhikr counter. Each dhikr has its own scene that grows with your
          remembrance.
        </p>
        <div className="mt-10 flex items-center justify-center gap-4">
          <a
            href="#download"
            className="px-6 py-3 rounded-full bg-teal-500 text-slate-950 font-medium hover:bg-teal-400 transition"
          >
            Get the app
          </a>
          <a
            href="#scenes"
            className="px-6 py-3 rounded-full border border-slate-700 text-slate-200 hover:border-slate-500 transition"
          >
            See the scenes
          </a>
        </div>
        <div className="mt-16 grid grid-cols-2 sm:grid-cols-4 gap-3 max-w-4xl mx-auto">
          {['/photos/garden.jpeg', '/photos/ocean.jpeg', '/photos/winter.jpeg', '/photos/twilight.jpeg'].map(
            (src, i) => (
              <div
                key={i}
                className="aspect-[9/16] rounded-2xl overflow-hidden border border-slate-800 shadow-lg"
              >
                <img src={src} alt="Safina scene" className="w-full h-full object-cover" loading="lazy" />
              </div>
            ),
          )}
        </div>
      </div>
    </section>
  )
}

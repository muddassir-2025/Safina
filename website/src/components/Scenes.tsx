const scenes = [
  { src: '/photos/garden.jpeg', name: 'Garden', note: 'Trees bloom, flowers spread, a rainbow appears.' },
  { src: '/photos/ocean.jpeg', name: 'Ocean', note: 'Light rays, fish and jellyfish in deep water.' },
  { src: '/photos/winter.jpeg', name: 'Winter', note: 'Snow, stars and a quiet house.' },
  { src: '/photos/twilight.jpeg', name: 'Twilight', note: 'Mountains, clouds and a warm sky.' },
  { src: '/photos/studio.jpeg', name: 'Scene Studio', note: 'Browse, upload and share scenes.' },
  { src: '/photos/dashboard.jpeg', name: 'Your Dhikr', note: 'All your dhikr in one place.' },
  { src: '/photos/start.jpeg', name: 'Begin', note: 'Bare trees waiting to grow.' },
]

export default function Scenes() {
  return (
    <section id="scenes" className="max-w-6xl mx-auto px-6 py-20">
      <div className="mb-10">
        <p className="text-teal-300 text-sm tracking-[0.2em] uppercase mb-3">Living scenes</p>
        <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight">A scene for every remembrance.</h2>
      </div>
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {scenes.map((s) => (
          <figure
            key={s.src}
            className="group relative aspect-[9/16] rounded-2xl overflow-hidden border border-slate-800"
          >
            <img
              src={s.src}
              alt={s.name}
              className="w-full h-full object-cover transition duration-500 group-hover:scale-105"
              loading="lazy"
            />
            <figcaption className="absolute inset-x-0 bottom-0 p-4 bg-gradient-to-t from-slate-950/90 to-transparent">
              <p className="font-medium">{s.name}</p>
              <p className="text-xs text-slate-300 mt-1">{s.note}</p>
            </figcaption>
          </figure>
        ))}
      </div>
    </section>
  )
}

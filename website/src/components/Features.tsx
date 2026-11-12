const dhikr = [
  'SubhanAllah, Alhamdulillah, Allahu Akbar and Astaghfirullah — each with Arabic text and English meaning',
  'Add your own dhikr with Arabic, English translation and a personal target',
  'Choose a target of 33, 99, 100 or 1000, or set your own',
  'Hold to reset when you want to start again',
  'Search your dhikr list quickly',
]

const studio = [
  'Pick from the built-in scenes',
  'Upload your own private scenes',
  'Explore public scenes shared by the community and save them to your device',
]

export default function Features() {
  return (
    <section id="features" className="max-w-6xl mx-auto px-6 py-20">
      <div className="grid md:grid-cols-2 gap-12">
        <div>
          <p className="text-teal-300 text-sm tracking-[0.2em] uppercase mb-3">Scenes that grow with your dhikr</p>
          <h2 className="text-3xl sm:text-4xl font-semibold tracking-tight">
            Each dhikr has its own scene.
          </h2>
          <p className="mt-4 text-slate-400 leading-relaxed">
            In the Garden, bare trees bloom into fruit, flowers spread across the grass, animals gather
            by the pond and a rainbow appears as you finish. Winter, Ocean and Night each have their own
            quiet atmosphere. A progress line shows Start, Quarter, Half and Done, so you always know
            where you are.
          </p>
          <div className="mt-8 flex flex-wrap gap-2">
            {['Start', 'Quarter', 'Half', 'Done'].map((s) => (
              <span
                key={s}
                className="px-3 py-1 rounded-full border border-slate-700 text-slate-300 text-xs"
              >
                {s}
              </span>
            ))}
          </div>
        </div>
        <div className="space-y-10">
          <div>
            <h3 className="text-xl font-medium mb-3">Dhikr</h3>
            <ul className="space-y-2 text-slate-400">
              {dhikr.map((d) => (
                <li key={d} className="flex gap-2">
                  <span className="text-teal-400 mt-1">•</span>
                  <span>{d}</span>
                </li>
              ))}
            </ul>
          </div>
          <div>
            <h3 className="text-xl font-medium mb-3">Scene Studio</h3>
            <ul className="space-y-2 text-slate-400">
              {studio.map((d) => (
                <li key={d} className="flex gap-2">
                  <span className="text-teal-400 mt-1">•</span>
                  <span>{d}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  )
}

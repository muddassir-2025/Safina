export default function Privacy() {
  return (
    <section className="max-w-3xl mx-auto px-6 py-16">
      <h1 className="text-3xl sm:text-4xl font-semibold tracking-tight">Privacy Policy</h1>
      <p className="text-slate-400 mt-2">Last updated: November 12, 2026</p>

      <div className="mt-8 space-y-8 text-slate-300 leading-relaxed">
        <div>
          <h2 className="text-xl font-medium text-white mb-2">Overview</h2>
          <p>
            Safina (“we”, “our”, “us”) is a dhikr counter app. This policy explains what
            information we collect, how we use it, and your choices. By using Safina you agree to this
            policy.
          </p>
        </div>

        <div>
          <h2 className="text-xl font-medium text-white mb-2">Information we collect</h2>
          <ul className="list-disc pl-6 space-y-1 text-slate-400">
            <li>
              <span className="text-slate-300">Account information</span> — if you sign in with Google,
              we receive your name, email address and profile picture from Google to identify your
              account.
            </li>
            <li>
              <span className="text-slate-300">Your dhikr data</span> — the dhikr you create, your
              counts, targets and scene preferences. When signed in, this is stored on our servers so
              it can sync across devices.
            </li>
            <li>
              <span className="text-slate-300">Usage analytics</span> — anonymous events such as session
              starts and completions, collected with Firebase Analytics to improve the app.
            </li>
            <li>
              <span className="text-slate-300">Device information</span> — basic technical data such as
              device model, OS version and app version, used for diagnostics.
            </li>
          </ul>
        </div>

        <div>
          <h2 className="text-xl font-medium text-white mb-2">How we use your information</h2>
          <p className="text-slate-400">
            To provide and sync your dhikr data, authenticate you, improve the app, fix bugs and
            understand which features are used. We do not sell your personal data.
          </p>
        </div>

        <div>
          <h2 className="text-xl font-medium text-white mb-2">Third-party services</h2>
          <p className="text-slate-400">
            Safina uses the following third-party services, each governed by their own privacy policy:
          </p>
          <ul className="list-disc pl-6 space-y-1 text-slate-400 mt-2">
            <li>Google Sign-In / Google (authentication)</li>
            <li>Firebase Analytics (usage analytics)</li>
            <li>Neon (cloud database for synced dhikr data)</li>
          </ul>
        </div>

        <div>
          <h2 className="text-xl font-medium text-white mb-2">Data retention and deletion</h2>
          <p className="text-slate-400">
            You can sign out at any time. To request deletion of your account and associated data,
            contact us at the email below and we will remove it within a reasonable period.
          </p>
        </div>

        <div>
          <h2 className="text-xl font-medium text-white mb-2">Children’s privacy</h2>
          <p className="text-slate-400">
            Safina is not directed to children under 13 and we do not knowingly collect personal
            information from children.
          </p>
        </div>

        <div>
          <h2 className="text-xl font-medium text-white mb-2">Changes to this policy</h2>
          <p className="text-slate-400">
            We may update this policy from time to time. Changes will be posted on this page with an
            updated date.
          </p>
        </div>

        <div>
          <h2 className="text-xl font-medium text-white mb-2">Contact</h2>
          <p className="text-slate-400">
            Questions about this policy? Contact us at{' '}
            <a className="text-teal-300 hover:underline" href="mailto:studymuddassir@gmail.com">
              studymuddassir@gmail.com
            </a>
            .
          </p>
        </div>
      </div>
    </section>
  )
}

export default function Home() {
  return (
    <main className="min-h-screen bg-neutral-900 text-white flex flex-col items-center justify-center p-6">
      <div className="text-center space-y-6 max-w-md">
        <div className="text-6xl">⚡</div>
        <h1 className="text-4xl font-bold tracking-tight">
          AetherNova
        </h1>
        <p className="text-neutral-400 text-lg">
          Welcome to AetherNova v1.0.0
        </p>
        <div className="pt-4">
          <div className="inline-block bg-indigo-600 hover:bg-indigo-700 text-white font-medium py-3 px-8 rounded-xl transition-colors">
            Get Started
          </div>
        </div>
        <p className="text-neutral-500 text-sm pt-6">
          Built with Next.js + Capacitor
        </p>
      </div>
    </main>
  );
}

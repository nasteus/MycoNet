<script>
  let { peers = [], onSelectPeer } = $props();
</script>

<div class="h-full bg-slate-50 p-4 overflow-y-auto">
  <div class="flex items-center justify-between mb-4 pb-2 border-b border-slate-200">
    <div>
      <h2 class="text-sm font-semibold text-slate-900">Nearby Devices</h2>
      <p class="text-xs text-slate-500">Nodes discovered via Wi-Fi Aware & local mesh</p>
    </div>
    <div class="px-2.5 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-medium">
      {peers.length} Online
    </div>
  </div>

  {#if peers.length === 0}
    <div class="flex flex-col items-center justify-center p-12 text-center text-slate-400 text-xs">
      <div class="w-12 h-12 mb-3 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-500">
        <svg class="w-6 h-6 animate-pulse" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="2"></circle>
          <path d="M16.24 7.76a6 6 0 0 1 0 8.49m-8.48-.01a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"></path>
        </svg>
      </div>
      <p class="font-medium text-slate-700 text-sm mb-1">Scanning for peers</p>
      <p class="text-xs text-slate-500 max-w-xs">
        Keep Wi-Fi turned on. Nearby MycoNet devices will automatically appear here.
      </p>
    </div>
  {:else}
    <div class="space-y-2.5">
      {#each peers as peer}
        <div class="p-3.5 bg-white border border-slate-200 hover:border-blue-300 rounded-xl flex items-center justify-between transition-all shadow-xs">
          <div class="flex items-center gap-3">
            <div class="relative">
              <div class="w-9 h-9 rounded-full bg-blue-50 text-blue-700 font-semibold flex items-center justify-center text-sm border border-blue-100">
                {(peer.alias || peer.id).substring(0, 1).toUpperCase()}
              </div>
              <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-white"></span>
            </div>
            <div>
              <div class="text-sm font-semibold text-slate-900 flex items-center gap-2">
                <span>{peer.alias || peer.id}</span>
                <span class="text-[10px] text-slate-500 font-mono px-1.5 py-0.5 rounded bg-slate-100">
                  {peer.id}
                </span>
              </div>
              <div class="text-xs text-slate-500 mt-0.5">
                {peer.directDistance || 'Direct link'} • Last seen {new Date(peer.lastSeen).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </div>
            </div>
          </div>

          <button
            onclick={() => onSelectPeer(peer.id)}
            class="px-3 py-1.5 text-xs font-semibold rounded-lg bg-blue-50 hover:bg-blue-600 text-blue-700 hover:text-white border border-blue-200 transition-colors"
          >
            Message
          </button>
        </div>
      {/each}
    </div>
  {/if}
</div>

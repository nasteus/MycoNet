<script>
  let { logs = [], onClearLogs } = $props();

  let filterType = $state('ALL');
  let logContainer;

  let filteredLogs = $derived(
    filterType === 'ALL' ? logs : logs.filter((l) => l.type === filterType)
  );

  function getBadgeClass(type) {
    switch (type) {
      case 'INCOMING':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'OUTGOING':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'RELAY':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'DROPPED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  }

  $effect(() => {
    if (filteredLogs.length && logContainer) {
      setTimeout(() => {
        logContainer.scrollTop = logContainer.scrollHeight;
      }, 30);
    }
  });
</script>

<div class="h-full bg-slate-50 flex flex-col text-xs font-sans">
  <!-- Controls Bar -->
  <div class="px-4 py-2.5 bg-white border-b border-slate-200 flex items-center justify-between gap-2 shadow-xs">
    <div class="flex items-center gap-1.5 overflow-x-auto">
      {#each ['ALL', 'INCOMING', 'OUTGOING', 'RELAY', 'DROPPED'] as f}
        <button
          onclick={() => (filterType = f)}
          class="px-2.5 py-1 rounded-md text-[11px] font-medium transition-colors {filterType === f
            ? 'bg-blue-600 text-white shadow-xs'
            : 'bg-slate-100 text-slate-600 hover:text-slate-900 hover:bg-slate-200'}"
        >
          {f}
        </button>
      {/each}
    </div>

    <button
      onclick={onClearLogs}
      class="text-[11px] font-medium text-slate-400 hover:text-rose-600 transition-colors px-2 py-1"
    >
      Clear
    </button>
  </div>

  <!-- Log Stream -->
  <div bind:this={logContainer} class="flex-1 p-3 overflow-y-auto space-y-2">
    {#if filteredLogs.length === 0}
      <div class="text-center text-slate-400 py-16">
        <p class="font-medium text-slate-600">Routing Engine Idle</p>
        <p class="text-xs text-slate-400 mt-1">Waiting for incoming radio packets or gossip floods…</p>
      </div>
    {:else}
      {#each filteredLogs as log}
        <div class="p-3 rounded-xl bg-white border border-slate-200 hover:border-slate-300 flex flex-col gap-1.5 shadow-xs">
          <div class="flex items-center justify-between text-[11px]">
            <div class="flex items-center gap-2">
              <span class="px-2 py-0.5 rounded-full border text-[10px] font-semibold {getBadgeClass(log.type)}">
                {log.type}
              </span>
              <span class="text-slate-500 font-mono">{new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
            </div>
            <span class="text-slate-500 font-mono text-[10px]">
              Hops: <strong class="text-slate-800">{log.hops}</strong> • TTL: <strong class="text-slate-800">{log.ttl}</strong>/15
            </span>
          </div>

          <div class="flex items-center justify-between text-xs text-slate-700">
            <div class="truncate max-w-[70%] font-mono text-[11px]">
              <span class="text-slate-400">From:</span> <span class="font-semibold text-slate-800">{log.sender}</span>
              <span class="text-slate-400 mx-1">➔</span>
              <span class="text-slate-400">To:</span> <span class="font-semibold text-slate-800">{log.target}</span>
            </div>
            <span class="text-slate-400 text-[10px] font-mono">
              {log.messageId ? log.messageId.substring(0, 8) : ''}
            </span>
          </div>

          {#if log.note}
            <div class="text-[11px] text-slate-600 bg-slate-50 px-2.5 py-1 rounded-md border border-slate-100 font-mono">
              {log.note}
            </div>
          {/if}
        </div>
      {/each}
    {/if}
  </div>
</div>

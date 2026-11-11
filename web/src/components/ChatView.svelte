<script>
  let { messages = [], selfNodeId = 'NODE-LOCAL', peers = [], onSendMessage } = $props();

  let textInput = $state('');
  let recipient = $state('*');
  let senderAlias = $state('User-1');
  let chatContainer;

  function handleSend() {
    if (!textInput.trim()) return;
    onSendMessage(recipient, textInput.trim(), senderAlias);
    textInput = '';
  }

  function handleKeydown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  $effect(() => {
    if (messages.length && chatContainer) {
      setTimeout(() => {
        chatContainer.scrollTop = chatContainer.scrollHeight;
      }, 50);
    }
  });
</script>

<div class="flex flex-col h-full bg-slate-50">
  <!-- Target Selector Bar -->
  <div class="px-4 py-2.5 bg-white border-b border-slate-200 flex items-center justify-between gap-3 text-xs shadow-xs">
    <div class="flex items-center gap-2">
      <span class="text-slate-500 font-medium">To:</span>
      <select
        bind:value={recipient}
        class="bg-slate-50 border border-slate-300 rounded-md px-2.5 py-1 text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs"
      >
        <option value="*">⚡ Broadcast (All Nearby Nodes)</option>
        {#each peers as peer}
          <option value={peer.id}>👤 {peer.alias || peer.id}</option>
        {/each}
      </select>
    </div>

    <div class="flex items-center gap-2">
      <span class="text-slate-500 font-medium">My Name:</span>
      <input
        type="text"
        bind:value={senderAlias}
        class="bg-slate-50 border border-slate-300 rounded-md px-2.5 py-1 text-slate-800 text-xs w-24 text-center focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
      />
    </div>
  </div>

  <!-- Messages List -->
  <div bind:this={chatContainer} class="flex-1 overflow-y-auto p-4 space-y-3">
    {#if messages.length === 0}
      <div class="flex flex-col items-center justify-center h-full text-slate-400 text-center px-6">
        <div class="w-14 h-14 mb-3 rounded-full bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-500">
          <svg class="w-7 h-7" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
          </svg>
        </div>
        <p class="text-sm font-semibold text-slate-700 mb-1">Direct Mesh Chat</p>
        <p class="text-xs text-slate-500 max-w-xs leading-relaxed">
          No messages in range yet. Type a message below to broadcast off-grid to nearby devices.
        </p>
      </div>
    {:else}
      {#each messages as msg}
        {@const isMe = msg.senderId === selfNodeId}
        <div class="flex flex-col {isMe ? 'items-end' : 'items-start'}">
          <!-- Metadata header -->
          <div class="flex items-center gap-1.5 mb-1 px-1">
            <span class="text-[11px] font-semibold {isMe ? 'text-blue-600' : 'text-slate-700'}">
              {isMe ? 'You' : (msg.senderAlias || msg.senderId)}
            </span>
            <span class="text-[10px] px-1.5 py-0.2 rounded-full {msg.recipientId === '*' ? 'bg-slate-100 text-slate-600' : 'bg-blue-50 text-blue-700'}">
              {msg.recipientId === '*' ? 'Broadcast' : 'Direct'}
            </span>
            <span class="text-[10px] text-slate-400">
              {msg.hopCount === 0 ? 'Direct' : `${msg.hopCount} hops`}
            </span>
          </div>

          <!-- Message Bubble -->
          <div
            class="max-w-[85%] rounded-2xl px-4 py-2.5 text-sm font-sans break-words shadow-xs {isMe
              ? 'bg-blue-600 text-white rounded-tr-xs'
              : 'bg-white text-slate-900 border border-slate-200 rounded-tl-xs'}"
          >
            {msg.text}
          </div>

          <!-- Metadata footer -->
          <div class="flex items-center gap-1.5 mt-1 px-1 text-[10px] text-slate-400 font-mono">
            <span>{new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
            <span>•</span>
            <span title={msg.messageId}>ID: {msg.messageId.substring(0, 6)}</span>
            {#if msg.isRelayed}
              <span class="text-amber-600 font-sans">• Relayed</span>
            {/if}
          </div>
        </div>
      {/each}
    {/if}
  </div>

  <!-- Message Composer -->
  <div class="p-3 bg-white border-t border-slate-200 shadow-md">
    <form onsubmit={(e) => { e.preventDefault(); handleSend(); }} class="flex items-center gap-2">
      <input
        type="text"
        placeholder="Type a message..."
        bind:value={textInput}
        onkeydown={handleKeydown}
        class="flex-1 bg-slate-100 hover:bg-slate-50 focus:bg-white border border-transparent focus:border-blue-500 rounded-full px-4 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:outline-none transition-all font-sans"
      />
      <button
        type="submit"
        disabled={!textInput.trim()}
        class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-40 disabled:hover:bg-blue-600 text-white text-xs font-semibold rounded-full transition-all flex items-center gap-1.5 shadow-sm shadow-blue-200"
      >
        <span>Send</span>
        <svg class="w-3.5 h-3.5 rotate-90" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="19" x2="12" y2="5"></line>
          <polyline points="5 12 12 5 19 12"></polyline>
        </svg>
      </button>
    </form>
  </div>
</div>

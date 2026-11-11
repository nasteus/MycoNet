<script>
  import { onMount } from 'svelte';
  import ChatView from './components/ChatView.svelte';
  import NodeList from './components/NodeList.svelte';
  import LogConsole from './components/LogConsole.svelte';

  let currentTab = $state('chat'); // 'chat' | 'nodes' | 'logs'
  let selfNodeId = $state('NODE-CONNECTING');
  let peers = $state([]);
  let messages = $state([]);
  let logs = $state([]);
  let isNativeBridgeAvailable = $state(false);
  let isMeshRunning = $state(true);

  onMount(() => {
    // Check if running inside Android WebView with MycoNetBridge
    if (typeof window !== 'undefined' && window.MycoNetBridge) {
      isNativeBridgeAvailable = true;
      try {
        selfNodeId = window.MycoNetBridge.getSelfNodeId();
        isMeshRunning = window.MycoNetBridge.isMeshRunning();
        const initialPeers = JSON.parse(window.MycoNetBridge.getConnectedPeers() || '[]');
        peers = initialPeers;
        const initialMessages = JSON.parse(window.MycoNetBridge.getRecentMessages() || '[]');
        messages = initialMessages;
        const initialLogs = JSON.parse(window.MycoNetBridge.getRecentLogs() || '[]');
        logs = initialLogs;
      } catch (err) {
        console.error('Error synchronizing with MycoNetBridge', err);
      }
    } else {
      // Mock mode for local browser preview/testing
      selfNodeId = 'NODE-ALPHA1';
      peers = [
        { id: 'NODE-E8A42F', alias: 'Unit-2', lastSeen: Date.now() - 5000, directDistance: 'Direct (1 hop)' },
        { id: 'NODE-C193D8', alias: 'Relay-Station', lastSeen: Date.now() - 32000, directDistance: 'Multi-hop (2 hops)' }
      ];
      logs = [
        { id: '1', timestamp: Date.now() - 10000, type: 'INCOMING', messageId: 'd3f28190-281b', sender: 'NODE-E8A42F', target: '*', hops: 1, ttl: 14, note: 'Gossip beacon received' },
        { id: '2', timestamp: Date.now() - 9500, type: 'RELAY', messageId: 'd3f28190-281b', sender: 'NODE-E8A42F', target: '*', hops: 2, ttl: 13, note: 'Retransmitted to all mesh neighbors' }
      ];
    }

    // Attach global event listener for native Kotlin push notifications
    window.onMycoNetEvent = (eventType, payload) => {
      if (eventType === 'message') {
        messages = [...messages, payload];
      } else if (eventType === 'log') {
        logs = [...logs, payload];
      } else if (eventType === 'peers') {
        peers = payload;
      } else if (eventType === 'mesh_state') {
        isMeshRunning = payload.running;
      }
    };
  });

  function handleToggleMesh() {
    const nextState = !isMeshRunning;
    if (isNativeBridgeAvailable && window.MycoNetBridge) {
      window.MycoNetBridge.setMeshRunning(nextState);
      isMeshRunning = nextState;
    } else {
      isMeshRunning = nextState;
    }
  }

  function handleStopAndExit() {
    if (confirm('Vrei să oprești complet serviciul de fundal și să închizi aplicația?')) {
      if (isNativeBridgeAvailable && window.MycoNetBridge) {
        window.MycoNetBridge.stopAndExit();
      } else {
        isMeshRunning = false;
        alert('Nod oprit (simulare browser)');
      }
    }
  }

  function handleSendMessage(recipient, text, alias) {
    if (!isMeshRunning) {
      alert('Nodul este oprit! Pornește mesh-ul pentru a putea transmite mesaje.');
      return;
    }

    if (isNativeBridgeAvailable && window.MycoNetBridge) {
      try {
        const responseJson = window.MycoNetBridge.sendMessage(recipient, text, alias);
        const msg = JSON.parse(responseJson);
        if (msg && !msg.error) {
          messages = [...messages, msg];
        }
      } catch (e) {
        console.error('Failed to send through bridge', e);
      }
    } else {
      // Browser Mock simulation
      const newMsg = {
        messageId: Math.random().toString(36).substring(2, 10),
        senderId: selfNodeId,
        senderAlias: alias,
        recipientId: recipient,
        text,
        hopCount: 0,
        ttl: 15,
        timestamp: Date.now(),
        pathTrace: [selfNodeId],
        isRelayed: false
      };
      messages = [...messages, newMsg];
      logs = [
        ...logs,
        {
          id: String(Date.now()),
          timestamp: Date.now(),
          type: 'OUTGOING',
          messageId: newMsg.messageId,
          sender: selfNodeId,
          target: recipient,
          hops: 0,
          ttl: 15,
          note: 'Broadcast simulation to radio'
        }
      ];
    }
  }

  function handleSelectPeer(peerId) {
    currentTab = 'chat';
  }

  function handleClearLogs() {
    logs = [];
  }
</script>

<main class="flex flex-col h-screen w-screen overflow-hidden bg-slate-50 text-slate-800 font-sans">
  <!-- Clean Top Bar -->
  <header class="px-3 py-2.5 bg-white border-b border-slate-200 flex items-center justify-between shadow-sm z-10">
    <div class="flex items-center gap-2.5">
      <!-- Network Logo Icon -->
      <div class="w-8 h-8 rounded-lg bg-blue-600 text-white flex items-center justify-center font-bold text-base shadow-sm shrink-0">
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="3"></circle>
          <circle cx="4" cy="12" r="2"></circle>
          <circle cx="20" cy="12" r="2"></circle>
          <circle cx="12" cy="4" r="2"></circle>
          <circle cx="12" cy="20" r="2"></circle>
          <line x1="6" y1="12" x2="9" y2="12"></line>
          <line x1="15" y1="12" x2="18" y2="12"></line>
          <line x1="12" y1="6" x2="12" y2="9"></line>
          <line x1="12" y1="15" x2="12" y2="18"></line>
        </svg>
      </div>

      <div>
        <div class="flex items-center gap-1.5">
          <h1 class="text-sm font-semibold tracking-tight text-slate-900 leading-tight">MycoNet</h1>
          <span class="text-[9px] px-1.5 py-0.2 rounded-full font-medium bg-blue-50 text-blue-700 border border-blue-200">
            Off-Grid
          </span>
        </div>
        <p class="text-[10px] text-slate-500 font-mono leading-tight">
          ID: <span class="text-slate-800 font-semibold">{selfNodeId}</span>
        </p>
      </div>
    </div>

    <!-- Right Controls: Toggle State & Exit App -->
    <div class="flex items-center gap-1.5">
      <!-- Interactive Mesh Status Toggle Button -->
      <button
        onclick={handleToggleMesh}
        class="px-2 py-1 rounded-full border text-[11px] font-medium flex items-center gap-1.5 transition-all shadow-xs {isMeshRunning
          ? 'bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100 active:scale-95'
          : 'bg-amber-50 border-amber-200 text-amber-700 hover:bg-amber-100 active:scale-95'}"
        title={isMeshRunning ? "Apasă pentru a pune mesh-ul pe pauză (economie baterie)" : "Apasă pentru a reactiva nodul"}
      >
        <span class="w-2 h-2 rounded-full {isMeshRunning ? 'bg-emerald-500' : 'bg-amber-500'}"></span>
        <span>{isMeshRunning ? 'Mesh Activ' : 'Mesh Oprit'}</span>
      </button>

      <!-- TTL Indicator -->
      <div class="px-2 py-1 rounded-full bg-slate-100 border border-slate-200 text-slate-600 text-[10px] font-mono hidden xs:flex">
        TTL: <span class="font-semibold text-slate-800 ml-0.5">15</span>
      </div>

      <!-- Dedicated Stop & Exit App Button -->
      <button
        onclick={handleStopAndExit}
        class="p-1.5 rounded-lg border border-red-200 bg-red-50 text-red-600 hover:bg-red-100 active:bg-red-200 active:scale-95 transition-all flex items-center gap-1 text-[11px] font-medium shadow-xs"
        title="Oprește complet serviciul de fundal și închide aplicația"
      >
        <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
          <line x1="12" y1="2" x2="12" y2="12"></line>
        </svg>
        <span class="text-[10px]">Ieșire</span>
      </button>
    </div>
  </header>

  <!-- Notification Banner when Mesh is Paused -->
  {#if !isMeshRunning}
    <div class="bg-amber-50 border-b border-amber-200 px-3.5 py-1.5 flex items-center justify-between text-[11px] text-amber-900 transition-all">
      <div class="flex items-center gap-2">
        <span class="w-2 h-2 rounded-full bg-amber-500 shrink-0"></span>
        <span>Nodul este oprit. Serviciul de fundal și scanările radio sunt inactive (0% baterie).</span>
      </div>
      <button
        onclick={handleToggleMesh}
        class="px-2 py-0.5 ml-2 rounded bg-amber-600 hover:bg-amber-700 text-white font-medium text-[10px] shrink-0 active:scale-95 transition"
      >
        Pornește
      </button>
    </div>
  {/if}

  <!-- Clean Segmented Tab Bar -->
  <nav class="flex bg-white border-b border-slate-200 px-3 py-1.5 gap-2 text-xs font-medium shadow-xs">
    <button
      onclick={() => (currentTab = 'chat')}
      class="flex-1 py-1.5 px-3 rounded-lg text-center transition-all flex items-center justify-center gap-1.5 {currentTab === 'chat'
        ? 'bg-blue-50 text-blue-700 font-semibold'
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}"
    >
      <span>💬 Chat</span>
      {#if messages.length > 0}
        <span class="text-[10px] px-1.5 py-0.2 rounded-full {currentTab === 'chat' ? 'bg-blue-200 text-blue-800' : 'bg-slate-100 text-slate-600'}">
          {messages.length}
        </span>
      {/if}
    </button>

    <button
      onclick={() => (currentTab = 'nodes')}
      class="flex-1 py-1.5 px-3 rounded-lg text-center transition-all flex items-center justify-center gap-1.5 {currentTab === 'nodes'
        ? 'bg-blue-50 text-blue-700 font-semibold'
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}"
    >
      <span>📡 Nodes</span>
      <span class="text-[10px] px-1.5 py-0.2 rounded-full {currentTab === 'nodes' ? 'bg-blue-200 text-blue-800' : 'bg-slate-100 text-slate-600'}">
        {peers.length}
      </span>
    </button>

    <button
      onclick={() => (currentTab = 'logs')}
      class="flex-1 py-1.5 px-3 rounded-lg text-center transition-all flex items-center justify-center gap-1.5 {currentTab === 'logs'
        ? 'bg-blue-50 text-blue-700 font-semibold'
        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'}"
    >
      <span>📋 Logs</span>
      {#if logs.length > 0}
        <span class="text-[10px] px-1.5 py-0.2 rounded-full {currentTab === 'logs' ? 'bg-blue-200 text-blue-800' : 'bg-slate-100 text-slate-600'}">
          {logs.length}
        </span>
      {/if}
    </button>
  </nav>

  <!-- Main View Area -->
  <section class="flex-1 overflow-hidden relative bg-slate-50">
    {#if currentTab === 'chat'}
      <ChatView
        {messages}
        {selfNodeId}
        {peers}
        onSendMessage={handleSendMessage}
      />
    {:else if currentTab === 'nodes'}
      <NodeList
        {peers}
        onSelectPeer={handleSelectPeer}
      />
    {:else if currentTab === 'logs'}
      <LogConsole
        {logs}
        onClearLogs={handleClearLogs}
      />
    {/if}
  </section>
</main>

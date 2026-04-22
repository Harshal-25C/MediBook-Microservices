<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>MediBook — UC5 Payment Service README</title>
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@300;400;600;700&family=Syne:wght@400;600;700;800&display=swap" rel="stylesheet"/>
<style>
  :root {
    --bg: #080c14;
    --surface: #0d1420;
    --border: #1a2540;
    --border-glow: #1e3a6e;
    --accent: #00d4ff;
    --accent2: #7c3aed;
    --accent3: #10b981;
    --warn: #f59e0b;
    --danger: #ef4444;
    --text: #e2e8f0;
    --muted: #64748b;
    --code-bg: #0a0f1e;
    --tag-blue: #1e3a6e;
    --tag-green: #064e3b;
    --tag-purple: #3b0764;
    --tag-amber: #451a03;
  }

  * { margin: 0; padding: 0; box-sizing: border-box; }

  html { scroll-behavior: smooth; }

  body {
    background: var(--bg);
    color: var(--text);
    font-family: 'Syne', sans-serif;
    line-height: 1.7;
    overflow-x: hidden;
  }

  /* ── GRID BACKGROUND ─────────────────── */
  body::before {
    content: '';
    position: fixed;
    inset: 0;
    background-image:
      linear-gradient(rgba(0,212,255,.03) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0,212,255,.03) 1px, transparent 1px);
    background-size: 40px 40px;
    pointer-events: none;
    z-index: 0;
  }

  /* ── ANIMATED GRADIENT ORBS ─────────── */
  .orb {
    position: fixed;
    border-radius: 50%;
    filter: blur(80px);
    opacity: .12;
    pointer-events: none;
    z-index: 0;
    animation: drift 18s ease-in-out infinite;
  }
  .orb1 { width: 500px; height: 500px; background: var(--accent); top: -100px; right: -100px; animation-delay: 0s; }
  .orb2 { width: 400px; height: 400px; background: var(--accent2); bottom: 10%; left: -80px; animation-delay: -7s; }
  .orb3 { width: 300px; height: 300px; background: var(--accent3); top: 50%; left: 50%; animation-delay: -13s; }

  @keyframes drift {
    0%, 100% { transform: translate(0,0) scale(1); }
    33% { transform: translate(30px,-20px) scale(1.05); }
    66% { transform: translate(-20px,30px) scale(.95); }
  }

  /* ── LAYOUT ──────────────────────────── */
  .wrapper { position: relative; z-index: 1; max-width: 1100px; margin: 0 auto; padding: 0 28px 80px; }

  /* ── HERO ────────────────────────────── */
  .hero {
    text-align: center;
    padding: 80px 0 60px;
    animation: fadeUp .8s ease both;
  }

  .hero-tag {
    display: inline-flex; align-items: center; gap: 8px;
    background: rgba(0,212,255,.08); border: 1px solid rgba(0,212,255,.25);
    border-radius: 100px; padding: 6px 18px; font-size: .78rem;
    font-family: 'JetBrains Mono', monospace; color: var(--accent);
    letter-spacing: .08em; margin-bottom: 28px;
    animation: fadeUp .8s ease .1s both;
  }
  .hero-tag::before { content: '◉'; animation: pulse 2s ease infinite; }

  @keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: .3; } }

  .hero h1 {
    font-size: clamp(2.4rem, 6vw, 4.2rem);
    font-weight: 800;
    line-height: 1.05;
    background: linear-gradient(135deg, #fff 30%, var(--accent) 70%);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    background-clip: text;
    animation: fadeUp .8s ease .2s both;
    margin-bottom: 12px;
  }

  .hero-sub {
    font-size: 1rem; color: var(--muted); max-width: 600px; margin: 0 auto 36px;
    animation: fadeUp .8s ease .3s both;
    font-family: 'JetBrains Mono', monospace;
  }

  .badges {
    display: flex; flex-wrap: wrap; gap: 10px; justify-content: center;
    animation: fadeUp .8s ease .4s both; margin-bottom: 48px;
  }

  .badge {
    display: flex; align-items: center; gap: 6px;
    padding: 6px 14px; border-radius: 6px; font-size: .75rem;
    font-family: 'JetBrains Mono', monospace; font-weight: 600;
    letter-spacing: .04em; border: 1px solid;
  }
  .badge-blue { background: rgba(0,212,255,.08); border-color: rgba(0,212,255,.3); color: var(--accent); }
  .badge-green { background: rgba(16,185,129,.08); border-color: rgba(16,185,129,.3); color: var(--accent3); }
  .badge-purple { background: rgba(124,58,237,.08); border-color: rgba(124,58,237,.3); color: #a78bfa; }
  .badge-amber { background: rgba(245,158,11,.08); border-color: rgba(245,158,11,.3); color: var(--warn); }

  /* ── NAV ──────────────────────────────── */
  .toc {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 28px 32px;
    margin-bottom: 52px;
    animation: fadeUp .8s ease .5s both;
  }
  .toc-title {
    font-size: .7rem; font-family: 'JetBrains Mono', monospace;
    color: var(--accent); letter-spacing: .12em; text-transform: uppercase;
    margin-bottom: 16px;
  }
  .toc-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 8px; list-style: none; }
  .toc-list li a {
    display: flex; align-items: center; gap: 8px;
    padding: 8px 12px; border-radius: 8px; text-decoration: none;
    color: var(--muted); font-size: .875rem; transition: all .2s;
    border: 1px solid transparent;
  }
  .toc-list li a:hover { color: var(--text); background: rgba(0,212,255,.06); border-color: var(--border-glow); }
  .toc-list li a .num { color: var(--accent); font-family: 'JetBrains Mono', monospace; font-size: .75rem; }

  /* ── SECTIONS ────────────────────────── */
  section {
    margin-bottom: 64px;
    animation: fadeUp .7s ease both;
  }

  .section-label {
    display: inline-flex; align-items: center; gap: 8px;
    font-size: .7rem; font-family: 'JetBrains Mono', monospace;
    color: var(--accent); letter-spacing: .12em; text-transform: uppercase;
    margin-bottom: 10px;
  }
  .section-label::before { content: ''; display: block; width: 24px; height: 1px; background: var(--accent); }

  h2 {
    font-size: clamp(1.5rem, 3vw, 2rem); font-weight: 700;
    color: #fff; margin-bottom: 28px; line-height: 1.2;
  }

  h3 {
    font-size: 1.05rem; font-weight: 600; color: var(--text); margin-bottom: 14px;
  }

  p { color: var(--muted); margin-bottom: 16px; line-height: 1.8; }

  /* ── SERVICE CARDS ───────────────────── */
  .services-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
  }

  .svc-card {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 24px;
    position: relative; overflow: hidden;
    transition: border-color .25s, transform .25s;
    cursor: default;
  }
  .svc-card::before {
    content: '';
    position: absolute; inset: 0;
    background: linear-gradient(135deg, var(--card-color, rgba(0,212,255,.04)), transparent 60%);
    opacity: 0; transition: opacity .3s;
  }
  .svc-card:hover { border-color: var(--card-color, var(--border-glow)); transform: translateY(-3px); }
  .svc-card:hover::before { opacity: 1; }

  .svc-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
  .svc-name { font-weight: 700; font-size: 1rem; color: #fff; }
  .svc-port {
    font-family: 'JetBrains Mono', monospace; font-size: .72rem;
    background: rgba(0,212,255,.08); border: 1px solid rgba(0,212,255,.2);
    color: var(--accent); padding: 3px 10px; border-radius: 100px;
  }
  .svc-desc { font-size: .85rem; color: var(--muted); margin-bottom: 14px; }
  .svc-tags { display: flex; flex-wrap: wrap; gap: 6px; }
  .tag {
    font-size: .7rem; font-family: 'JetBrains Mono', monospace;
    padding: 3px 9px; border-radius: 4px; letter-spacing: .03em;
  }
  .tag-blue { background: var(--tag-blue); color: #93c5fd; }
  .tag-green { background: var(--tag-green); color: #6ee7b7; }
  .tag-purple { background: var(--tag-purple); color: #c4b5fd; }
  .tag-amber { background: var(--tag-amber); color: #fcd34d; }

  /* ── ARCH DIAGRAM ───────────────────── */
  .arch-diagram {
    background: var(--code-bg);
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 36px;
    font-family: 'JetBrains Mono', monospace;
    font-size: .82rem;
    line-height: 1.9;
    overflow-x: auto;
    color: #94a3b8;
  }
  .arch-accent { color: var(--accent); }
  .arch-green { color: var(--accent3); }
  .arch-purple { color: #a78bfa; }
  .arch-amber { color: var(--warn); }

  /* ── API ENDPOINT CARDS ─────────────── */
  .endpoint-group { margin-bottom: 36px; }
  .endpoint-group h3 { font-size: .85rem; color: var(--muted); font-family: 'JetBrains Mono', monospace; text-transform: uppercase; letter-spacing: .08em; border-bottom: 1px solid var(--border); padding-bottom: 10px; margin-bottom: 18px; }

  .endpoint {
    background: var(--surface);
    border: 1px solid var(--border);
    border-left: 3px solid transparent;
    border-radius: 10px;
    padding: 0;
    margin-bottom: 12px;
    overflow: hidden;
    transition: border-color .2s;
  }
  .endpoint:hover { border-color: var(--border-glow); }
  .endpoint.post { border-left-color: #22c55e; }
  .endpoint.get { border-left-color: var(--accent); }
  .endpoint.put { border-left-color: var(--warn); }
  .endpoint.delete { border-left-color: var(--danger); }

  .ep-header {
    display: flex; align-items: center; gap: 12px;
    padding: 14px 20px; cursor: pointer;
    user-select: none;
  }
  .ep-method {
    font-family: 'JetBrains Mono', monospace; font-size: .72rem;
    font-weight: 700; padding: 3px 10px; border-radius: 5px; min-width: 56px; text-align: center;
  }
  .method-post { background: rgba(34,197,94,.15); color: #4ade80; }
  .method-get { background: rgba(0,212,255,.12); color: var(--accent); }
  .method-put { background: rgba(245,158,11,.12); color: var(--warn); }
  .method-delete { background: rgba(239,68,68,.12); color: #f87171; }

  .ep-path { font-family: 'JetBrains Mono', monospace; font-size: .85rem; color: #fff; flex: 1; }
  .ep-summary { font-size: .8rem; color: var(--muted); }
  .ep-arrow { color: var(--muted); font-size: .8rem; transition: transform .2s; }
  .endpoint.open .ep-arrow { transform: rotate(90deg); }

  .ep-body {
    max-height: 0; overflow: hidden;
    transition: max-height .35s ease;
  }
  .endpoint.open .ep-body { max-height: 1200px; }

  .ep-inner { padding: 0 20px 20px; border-top: 1px solid var(--border); padding-top: 16px; }
  .ep-desc { font-size: .84rem; color: var(--muted); margin-bottom: 14px; line-height: 1.7; }

  /* ── CODE BLOCKS ─────────────────────── */
  .code-tabs { display: flex; gap: 8px; margin-bottom: 0; flex-wrap: wrap; }
  .code-tab {
    font-size: .72rem; font-family: 'JetBrains Mono', monospace;
    padding: 5px 14px; border-radius: 6px 6px 0 0; cursor: pointer;
    color: var(--muted); background: var(--code-bg); border: 1px solid var(--border);
    border-bottom: none; transition: color .2s;
  }
  .code-tab.active { color: var(--accent); border-color: var(--border-glow); }

  .code-block {
    background: var(--code-bg);
    border: 1px solid var(--border);
    border-radius: 0 8px 8px 8px;
    padding: 20px;
    font-family: 'JetBrains Mono', monospace;
    font-size: .8rem;
    line-height: 1.7;
    overflow-x: auto;
    position: relative;
    white-space: pre;
  }
  .code-panel { display: none; }
  .code-panel.active { display: block; }

  .copy-btn {
    position: absolute; top: 10px; right: 10px;
    background: rgba(0,212,255,.1); border: 1px solid rgba(0,212,255,.25);
    color: var(--accent); font-family: 'JetBrains Mono', monospace;
    font-size: .68rem; padding: 4px 10px; border-radius: 5px; cursor: pointer;
    transition: all .2s;
  }
  .copy-btn:hover { background: rgba(0,212,255,.2); }

  .response-label {
    font-size: .72rem; font-family: 'JetBrains Mono', monospace;
    color: var(--accent3); margin: 14px 0 6px;
    display: flex; align-items: center; gap: 6px;
  }
  .response-label::before { content: '▸'; }

  /* ── STATUS FLOW ─────────────────────── */
  .flow-row {
    display: flex; align-items: center; flex-wrap: wrap; gap: 12px;
    margin-bottom: 24px;
  }
  .flow-state {
    padding: 10px 20px; border-radius: 10px; font-size: .85rem; font-weight: 600;
    border: 1px solid; min-width: 110px; text-align: center;
  }
  .flow-arrow { color: var(--muted); font-size: 1.2rem; }
  .state-pending { background: rgba(245,158,11,.08); border-color: rgba(245,158,11,.3); color: var(--warn); }
  .state-success { background: rgba(16,185,129,.08); border-color: rgba(16,185,129,.3); color: var(--accent3); }
  .state-failed { background: rgba(239,68,68,.08); border-color: rgba(239,68,68,.3); color: #f87171; }
  .state-refunded { background: rgba(124,58,237,.08); border-color: rgba(124,58,237,.3); color: #a78bfa; }

  /* ── ENV TABLE ───────────────────────── */
  .env-table { width: 100%; border-collapse: collapse; font-size: .85rem; }
  .env-table th {
    text-align: left; padding: 10px 16px;
    background: rgba(0,212,255,.05); color: var(--accent);
    font-family: 'JetBrains Mono', monospace; font-size: .72rem;
    text-transform: uppercase; letter-spacing: .08em;
    border-bottom: 1px solid var(--border);
  }
  .env-table td { padding: 10px 16px; border-bottom: 1px solid var(--border); color: var(--muted); vertical-align: top; }
  .env-table td:first-child { font-family: 'JetBrains Mono', monospace; color: var(--accent3); font-size: .8rem; }
  .env-table tr:last-child td { border-bottom: none; }
  .env-table tr:hover td { background: rgba(255,255,255,.02); }
  .req-badge { font-size: .68rem; background: rgba(239,68,68,.15); color: #f87171; border-radius: 4px; padding: 1px 7px; border: 1px solid rgba(239,68,68,.25); }
  .opt-badge { font-size: .68rem; background: rgba(16,185,129,.1); color: var(--accent3); border-radius: 4px; padding: 1px 7px; border: 1px solid rgba(16,185,129,.2); }

  /* ── CALLOUTS ────────────────────────── */
  .callout {
    display: flex; gap: 14px; padding: 18px 20px;
    border-radius: 10px; margin-bottom: 16px; border: 1px solid;
  }
  .callout-icon { font-size: 1.1rem; flex-shrink: 0; margin-top: 2px; }
  .callout-body { font-size: .85rem; line-height: 1.7; }
  .callout-blue { background: rgba(0,212,255,.05); border-color: rgba(0,212,255,.2); }
  .callout-blue .callout-body { color: #93c5fd; }
  .callout-green { background: rgba(16,185,129,.05); border-color: rgba(16,185,129,.2); }
  .callout-green .callout-body { color: #6ee7b7; }
  .callout-amber { background: rgba(245,158,11,.05); border-color: rgba(245,158,11,.2); }
  .callout-amber .callout-body { color: #fcd34d; }

  /* ── INLINE CODE ─────────────────────── */
  code {
    font-family: 'JetBrains Mono', monospace;
    background: rgba(0,212,255,.08); color: var(--accent);
    padding: 2px 7px; border-radius: 4px; font-size: .82em;
  }

  /* ── ANIMATIONS ──────────────────────── */
  @keyframes fadeUp {
    from { opacity: 0; transform: translateY(22px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .reveal { opacity: 0; transform: translateY(20px); transition: opacity .6s ease, transform .6s ease; }
  .reveal.visible { opacity: 1; transform: none; }

  /* ── PAYMENT FLOW DIAGRAM ────────────── */
  .flow-diagram {
    background: var(--code-bg); border: 1px solid var(--border);
    border-radius: 14px; padding: 32px; overflow-x: auto;
  }
  .flow-step {
    display: flex; align-items: flex-start; gap: 20px; margin-bottom: 0;
  }
  .flow-line { display: flex; flex-direction: column; align-items: center; }
  .flow-dot {
    width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
    display: flex; align-items: center; justify-content: center;
    font-size: .75rem; font-weight: 700; font-family: 'JetBrains Mono', monospace;
  }
  .flow-dot-blue { background: rgba(0,212,255,.15); border: 2px solid var(--accent); color: var(--accent); }
  .flow-dot-green { background: rgba(16,185,129,.15); border: 2px solid var(--accent3); color: var(--accent3); }
  .flow-dot-purple { background: rgba(124,58,237,.15); border: 2px solid #7c3aed; color: #a78bfa; }
  .flow-connector { width: 2px; flex: 1; min-height: 24px; background: var(--border); }
  .flow-content { padding: 6px 0 24px; flex: 1; }
  .flow-content h4 { font-size: .9rem; color: #fff; margin-bottom: 4px; }
  .flow-content p { font-size: .8rem; color: var(--muted); margin: 0; }

  /* ── SCROLLBAR ───────────────────────── */
  ::-webkit-scrollbar { width: 5px; height: 5px; }
  ::-webkit-scrollbar-track { background: var(--bg); }
  ::-webkit-scrollbar-thumb { background: var(--border-glow); border-radius: 3px; }
</style>
</head>
<body>

<div class="orb orb1"></div>
<div class="orb orb2"></div>
<div class="orb orb3"></div>

<div class="wrapper">

  <!-- ══ HERO ══════════════════════════════════════════════════════ -->
  <div class="hero">
    <div class="hero-tag">feature/UC5-payment-service · Spring Boot 3.2 · Java 17</div>
    <h1>MediBook Payment Service</h1>
    <p class="hero-sub">UC5 · Razorpay-Ready · Microservices Architecture · JWT Secured</p>
    <div class="badges">
      <span class="badge badge-blue">☕ Spring Boot 3.2</span>
      <span class="badge badge-green">🔒 JWT Auth</span>
      <span class="badge badge-amber">💳 Razorpay SDK</span>
      <span class="badge badge-purple">🔗 Feign Client</span>
      <span class="badge badge-blue">📦 Eureka Discovery</span>
      <span class="badge badge-green">🛡 Spring Security</span>
      <span class="badge badge-amber">🗄 MySQL 8</span>
      <span class="badge badge-purple">🐇 RabbitMQ</span>
    </div>
  </div>

  <!-- ══ TABLE OF CONTENTS ════════════════════════════════════════ -->
  <nav class="toc reveal">
    <div class="toc-title">📋 Table of Contents</div>
    <ul class="toc-list">
      <li><a href="#overview"><span class="num">01</span> Overview</a></li>
      <li><a href="#architecture"><span class="num">02</span> Architecture</a></li>
      <li><a href="#services"><span class="num">03</span> All Services</a></li>
      <li><a href="#payment-service"><span class="num">04</span> Payment Service</a></li>
      <li><a href="#api"><span class="num">05</span> API Reference</a></li>
      <li><a href="#flow"><span class="num">06</span> Payment Flow</a></li>
      <li><a href="#status"><span class="num">07</span> Status Lifecycle</a></li>
      <li><a href="#testing"><span class="num">08</span> API Testing</a></li>
      <li><a href="#env"><span class="num">09</span> Environment</a></li>
      <li><a href="#startup"><span class="num">10</span> Quick Start</a></li>
    </ul>
  </nav>

  <!-- ══ 01 OVERVIEW ═══════════════════════════════════════════════ -->
  <section id="overview" class="reveal">
    <div class="section-label">01 Overview</div>
    <h2>What is MediBook?</h2>
    <p>MediBook is an <strong style="color:#fff">Online Appointment Booking System</strong> built as a distributed microservices platform using Spring Boot, Spring Cloud, and Spring Security. It enables patients to book appointments with healthcare providers, manage slots, process payments, and receive notifications — all orchestrated through an API gateway with JWT authentication.</p>
    <p>The <strong style="color:#fff">UC5 Payment Service</strong> is the financial backbone of the platform. It handles the full payment lifecycle: initiation via Razorpay, HMAC-SHA256 signature verification, refund processing, and analytics — all Razorpay-ready from day one.</p>
    <div class="callout callout-blue">
      <div class="callout-icon">💡</div>
      <div class="callout-body">The service follows a <strong>gateway-agnostic architecture</strong>: the <code>PaymentService</code> interface is the stable contract and <code>PaymentServiceImpl</code> is the swappable implementation. Switching from mock to real Razorpay requires zero controller changes.</div>
    </div>
  </section>

  <!-- ══ 02 ARCHITECTURE ════════════════════════════════════════════ -->
  <section id="architecture" class="reveal">
    <div class="section-label">02 Architecture</div>
    <h2>System Architecture</h2>
    <div class="arch-diagram">
<span class="arch-amber">[ React Frontend :5173 ]</span>
         │  HTTP
         ▼
<span class="arch-accent">┌─────────────────────────────────────────────────────────────┐</span>
<span class="arch-accent">│         API GATEWAY  :8080  (Spring Cloud Gateway)          │</span>
<span class="arch-accent">│  JWT Filter ─ Route /auth/** /providers/** /payments/** ...  │</span>
<span class="arch-accent">└──────────────┬──────────────┬──────────────┬───────────────┘</span>
               │              │              │
    ┌──────────▼──┐  ┌────────▼───┐  ┌──────▼──────────┐
    │ auth-service│  │ appt-svc   │  │ <span class="arch-green">payment-service </span>│
    │   :8081     │  │  :8084     │  │    <span class="arch-green">:8085        </span>│
    │  MySQL:auth │  │ MySQL:appt │  │ MySQL:payment_db│
    │  JWT tokens │  │ RabbitMQ   │◄─┤ Razorpay SDK    │
    └─────────────┘  └────────────┘  │ Feign → appt-svc│
                                     └─────────────────┘
    ┌─────────────┐  ┌────────────┐
    │ provider-svc│  │schedule-svc│
    │   :8082     │  │   :8083    │
    └─────────────┘  └────────────┘
               │              │
    <span class="arch-purple">┌─────────────────────────────────┐</span>
    <span class="arch-purple">│  Eureka Discovery Server :8761   │</span>
    <span class="arch-purple">│  admin:medibook123               │</span>
    <span class="arch-purple">└─────────────────────────────────┘</span></div>
  </section>

  <!-- ══ 03 ALL SERVICES ════════════════════════════════════════════ -->
  <section id="services" class="reveal">
    <div class="section-label">03 All Services</div>
    <h2>Microservice Directory</h2>
    <div class="services-grid">

      <div class="svc-card" style="--card-color: rgba(124,58,237,.3)">
        <div class="svc-header">
          <span class="svc-name">🔐 Eureka Server</span>
          <span class="svc-port">:8761</span>
        </div>
        <div class="svc-desc">Service discovery hub. All services register here. Spring Security protects the dashboard.</div>
        <div class="svc-tags">
          <span class="tag tag-purple">Spring Eureka</span>
          <span class="tag tag-blue">Start First</span>
        </div>
      </div>

      <div class="svc-card" style="--card-color: rgba(0,212,255,.3)">
        <div class="svc-header">
          <span class="svc-name">🌐 API Gateway</span>
          <span class="svc-port">:8080</span>
        </div>
        <div class="svc-desc">Single entry point. JWT authentication filter. Routes to all downstream services. CORS configured for frontend :5173.</div>
        <div class="svc-tags">
          <span class="tag tag-blue">Spring Cloud Gateway</span>
          <span class="tag tag-green">JWT Filter</span>
          <span class="tag tag-purple">lb:// routing</span>
        </div>
      </div>

      <div class="svc-card" style="--card-color: rgba(16,185,129,.3)">
        <div class="svc-header">
          <span class="svc-name">🔑 Auth Service</span>
          <span class="svc-port">:8081</span>
        </div>
        <div class="svc-desc">User registration, login, JWT token generation. Role-based: PATIENT, DOCTOR, ADMIN. Email OTP support via Gmail SMTP.</div>
        <div class="svc-tags">
          <span class="tag tag-green">JWT</span>
          <span class="tag tag-blue">Spring Security</span>
          <span class="tag tag-amber">SMTP Mail</span>
        </div>
      </div>

      <div class="svc-card" style="--card-color: rgba(99,102,241,.3)">
        <div class="svc-header">
          <span class="svc-name">👨‍⚕️ Provider Service</span>
          <span class="svc-port">:8082</span>
        </div>
        <div class="svc-desc">Doctor/provider profile management. Specialty, bio, fees. CRUD operations for healthcare providers.</div>
        <div class="svc-tags">
          <span class="tag tag-purple">MySQL</span>
          <span class="tag tag-blue">JPA</span>
        </div>
      </div>

      <div class="svc-card" style="--card-color: rgba(245,158,11,.3)">
        <div class="svc-header">
          <span class="svc-name">📅 Schedule Service</span>
          <span class="svc-port">:8083</span>
        </div>
        <div class="svc-desc">Time-slot management for providers. Create, update, and fetch available appointment slots. Slot booking lock mechanism.</div>
        <div class="svc-tags">
          <span class="tag tag-amber">Slot Locking</span>
          <span class="tag tag-blue">MySQL</span>
        </div>
      </div>

      <div class="svc-card" style="--card-color: rgba(239,68,68,.3)">
        <div class="svc-header">
          <span class="svc-name">🏥 Appointment Service</span>
          <span class="svc-port">:8084</span>
        </div>
        <div class="svc-desc">Core booking engine. Books slots, manages appointment lifecycle (SCHEDULED→COMPLETED→CANCELLED). Feign calls to schedule-service. Publishes events via RabbitMQ.</div>
        <div class="svc-tags">
          <span class="tag tag-green">RabbitMQ</span>
          <span class="tag tag-purple">Feign Client</span>
          <span class="tag tag-blue">No-show Scheduler</span>
        </div>
      </div>

      <div class="svc-card" style="--card-color: rgba(0,212,255,.4); border-color: rgba(0,212,255,.25)">
        <div class="svc-header">
          <span class="svc-name" style="color: var(--accent)">💳 Payment Service</span>
          <span class="svc-port">:8085</span>
        </div>
        <div class="svc-desc"><strong style="color:#fff">UC5 — This service.</strong> Razorpay-ready payment engine. Handles initiation, HMAC verification, refunds, and revenue analytics.</div>
        <div class="svc-tags">
          <span class="tag tag-blue">Razorpay SDK</span>
          <span class="tag tag-green">HMAC-SHA256</span>
          <span class="tag tag-amber">Feign → Appt</span>
          <span class="tag tag-purple">UC5</span>
        </div>
      </div>

    </div>
  </section>

  <!-- ══ 04 PAYMENT SERVICE DEEP DIVE ══════════════════════════════ -->
  <section id="payment-service" class="reveal">
    <div class="section-label">04 Payment Service</div>
    <h2>Deep Dive: Payment Service</h2>

    <h3>Entity: <code>Payment</code></h3>
    <p>Maps to the <code>payments</code> MySQL table. Every transaction — successful or failed — creates a permanent record for audit trail compliance.</p>

    <div style="overflow-x:auto; margin-bottom: 28px;">
      <table class="env-table">
        <thead><tr><th>Field</th><th>Type</th><th>Description</th></tr></thead>
        <tbody>
          <tr><td>paymentId</td><td>INT AUTO</td><td>Primary key, auto-generated</td></tr>
          <tr><td>appointmentId</td><td>INT UNIQUE</td><td>One payment per appointment</td></tr>
          <tr><td>patientId</td><td>INT</td><td>Links to auth-service user</td></tr>
          <tr><td>amount</td><td>DOUBLE</td><td>Amount in ₹ (rupees)</td></tr>
          <tr><td>currency</td><td>VARCHAR</td><td>Default: INR</td></tr>
          <tr><td>paymentMethod</td><td>VARCHAR</td><td>CARD / UPI / NETBANKING / WALLET</td></tr>
          <tr><td>status</td><td>VARCHAR</td><td>PENDING / SUCCESS / FAILED / REFUNDED</td></tr>
          <tr><td>razorpayOrderId</td><td>VARCHAR</td><td>Razorpay order ID (or MOCK_ORDER_N)</td></tr>
          <tr><td>razorpayPaymentId</td><td>VARCHAR</td><td>Razorpay payment ID after completion</td></tr>
          <tr><td>razorpaySignature</td><td>VARCHAR</td><td>HMAC-SHA256 verification hash</td></tr>
          <tr><td>createdAt</td><td>DATETIME</td><td>Auto-set on @PrePersist</td></tr>
          <tr><td>updatedAt</td><td>DATETIME</td><td>Auto-set on @PreUpdate</td></tr>
          <tr><td>notes</td><td>TEXT</td><td>System-generated audit notes</td></tr>
        </tbody>
      </table>
    </div>

    <h3>Service Interface: <code>PaymentService</code></h3>
    <div class="callout callout-blue">
      <div class="callout-icon">🔌</div>
      <div class="callout-body">The interface defines the <strong>stable contract</strong>. <code>PaymentServiceImpl</code> is the swappable implementation. The controller depends only on the interface — zero breaking changes when switching payment gateways.</div>
    </div>

    <h3>Feign Client: <code>AppointmentClient</code></h3>
    <p>Payment service communicates with appointment-service via Spring Cloud OpenFeign. Before initiating payment, it fetches appointment status to ensure only <code>SCHEDULED</code> or <code>PENDING_PAYMENT</code> appointments can be paid for.</p>
    <div class="code-block" style="border-radius:10px; margin-bottom: 28px;"><span style="color:#7c3aed">@FeignClient</span>(name = <span style="color:#22c55e">"appointment-service"</span>)
<span style="color:#7c3aed">public interface</span> <span style="color:#00d4ff">AppointmentClient</span> {

    <span style="color:#7c3aed">@GetMapping</span>(<span style="color:#22c55e">"/appointments/{appointmentId}"</span>)
    <span style="color:#00d4ff">AppointmentDto</span> getById(<span style="color:#7c3aed">@PathVariable</span> int appointmentId);
}</div>
  </section>

  <!-- ══ 05 API REFERENCE ═══════════════════════════════════════════ -->
  <section id="api" class="reveal">
    <div class="section-label">05 API Reference</div>
    <h2>Payment Service API</h2>
    <p>Base URL via Gateway: <code>http://localhost:8080/payments</code> · Direct: <code>http://localhost:8085/payments</code></p>
    <p>Swagger UI: <code>http://localhost:8085/swagger-ui.html</code></p>

    <div class="endpoint-group">
      <h3>Core Payment Operations</h3>

      <!-- INITIATE -->
      <div class="endpoint post" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-post">POST</span>
          <span class="ep-path">/payments/initiate</span>
          <span class="ep-summary">Initiate payment for appointment</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Creates a Razorpay order for an appointment. Validates appointment status (must be SCHEDULED). Checks for duplicate payment. Returns orderId that frontend uses to open Razorpay popup. Requires JWT auth header.</p>
            <div class="code-tabs">
              <div class="code-tab active" onclick="switchTab(event, 'init-curl')">cURL</div>
              <div class="code-tab" onclick="switchTab(event, 'init-http')">HTTP</div>
              <div class="code-tab" onclick="switchTab(event, 'init-js')">JavaScript</div>
            </div>
            <div id="init-curl" class="code-panel active">
              <div class="code-block">
                <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b">curl</span> -X POST http://localhost:8080/payments/initiate \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;JWT_TOKEN&gt;"</span> \
  -d <span style="color:#22c55e">'{
    "appointmentId": 5,
    "patientId": 12,
    "amount": 500.00,
    "paymentMethod": "UPI",
    "currency": "INR"
  }'</span></div>
            </div>
            <div id="init-http" class="code-panel">
              <div class="code-block">POST /payments/initiate HTTP/1.1
Host: localhost:8080
Authorization: Bearer &lt;JWT_TOKEN&gt;
Content-Type: application/json

{
  "appointmentId": 5,
  "patientId": 12,
  "amount": 500.00,
  "paymentMethod": "UPI",
  "currency": "INR"
}</div>
            </div>
            <div id="init-js" class="code-panel">
              <div class="code-block">
                <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#7c3aed">const</span> response = <span style="color:#7c3aed">await</span> fetch(<span style="color:#22c55e">"http://localhost:8080/payments/initiate"</span>, {
  method: <span style="color:#22c55e">"POST"</span>,
  headers: {
    <span style="color:#22c55e">"Content-Type"</span>: <span style="color:#22c55e">"application/json"</span>,
    <span style="color:#22c55e">"Authorization"</span>: <span style="color:#22c55e">`Bearer ${token}`</span>
  },
  body: JSON.stringify({
    appointmentId: <span style="color:#f59e0b">5</span>,
    patientId: <span style="color:#f59e0b">12</span>,
    amount: <span style="color:#f59e0b">500.00</span>,
    paymentMethod: <span style="color:#22c55e">"UPI"</span>,
    currency: <span style="color:#22c55e">"INR"</span>
  })
});
<span style="color:#7c3aed">const</span> data = <span style="color:#7c3aed">await</span> response.json();</div>
            </div>
            <div class="response-label">201 Created — Success Response</div>
            <div class="code-block" style="border-radius:10px;">{
  <span style="color:#00d4ff">"paymentId"</span>: 1,
  <span style="color:#00d4ff">"appointmentId"</span>: 5,
  <span style="color:#00d4ff">"status"</span>: <span style="color:#22c55e">"PENDING"</span>,
  <span style="color:#00d4ff">"amount"</span>: 500.0,
  <span style="color:#00d4ff">"currency"</span>: <span style="color:#22c55e">"INR"</span>,
  <span style="color:#00d4ff">"paymentMethod"</span>: <span style="color:#22c55e">"UPI"</span>,
  <span style="color:#00d4ff">"razorpayOrderId"</span>: <span style="color:#22c55e">"order_NiXe5u3kZ9XYAB"</span>,
  <span style="color:#00d4ff">"razorpayPaymentId"</span>: null,
  <span style="color:#00d4ff">"message"</span>: <span style="color:#22c55e">"Order created. Complete payment in popup."</span>,
  <span style="color:#00d4ff">"transactionTime"</span>: <span style="color:#22c55e">"2026-04-22 10:30:00"</span>
}</div>
          </div>
        </div>
      </div>

      <!-- VERIFY -->
      <div class="endpoint post" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-post">POST</span>
          <span class="ep-path">/payments/verify</span>
          <span class="ep-summary">Verify payment signature</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Verifies HMAC-SHA256 Razorpay signature after patient completes payment. If signature matches: status → SUCCESS. If not: status → FAILED. In mock mode: pass MOCK_ orderId — signature is skipped automatically.</p>
            <div class="code-tabs">
              <div class="code-tab active" onclick="switchTab(event, 'verify-curl')">cURL</div>
              <div class="code-tab" onclick="switchTab(event, 'verify-mock')">Mock Mode</div>
            </div>
            <div id="verify-curl" class="code-panel active">
              <div class="code-block">
                <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b">curl</span> -X POST http://localhost:8080/payments/verify \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;JWT_TOKEN&gt;"</span> \
  -d <span style="color:#22c55e">'{
    "razorpayOrderId": "order_NiXe5u3kZ9XYAB",
    "razorpayPaymentId": "pay_ABC123XYZ456",
    "razorpaySignature": "abc123signature_hmac_sha256_here"
  }'</span></div>
            </div>
            <div id="verify-mock" class="code-panel">
              <div class="code-block">
                <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Mock mode — signature check is bypassed for MOCK_ orders</span>
curl -X POST http://localhost:8080/payments/verify \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;JWT_TOKEN&gt;"</span> \
  -d <span style="color:#22c55e">'{
    "razorpayOrderId": "MOCK_ORDER_5",
    "razorpayPaymentId": "MOCK_PAY_1712345678",
    "razorpaySignature": null
  }'</span></div>
            </div>
            <div class="response-label">200 OK — Verified Response</div>
            <div class="code-block" style="border-radius:10px;">{
  <span style="color:#00d4ff">"paymentId"</span>: 1,
  <span style="color:#00d4ff">"status"</span>: <span style="color:#10b981">"SUCCESS"</span>,
  <span style="color:#00d4ff">"razorpayPaymentId"</span>: <span style="color:#22c55e">"pay_ABC123XYZ456"</span>,
  <span style="color:#00d4ff">"message"</span>: <span style="color:#22c55e">"Payment successful. Appointment confirmed."</span>
}</div>
          </div>
        </div>
      </div>

      <!-- REFUND -->
      <div class="endpoint post" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-post">POST</span>
          <span class="ep-path">/payments/{paymentId}/refund</span>
          <span class="ep-summary">Initiate refund</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Initiates a refund for a successful payment. Only <code>SUCCESS</code> status payments can be refunded. Calls Razorpay refund API — in mock mode, sets status to REFUNDED immediately. Typically called internally by appointment-service on cancellation.</p>
            <div class="code-block">
              <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b">curl</span> -X POST http://localhost:8080/payments/1/refund \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;ADMIN_JWT&gt;"</span></div>
            <div class="response-label">200 OK — Refund Response</div>
            <div class="code-block" style="border-radius:10px;">{
  <span style="color:#00d4ff">"paymentId"</span>: 1,
  <span style="color:#00d4ff">"status"</span>: <span style="color:#a78bfa">"REFUNDED"</span>,
  <span style="color:#00d4ff">"message"</span>: <span style="color:#22c55e">"Refund initiated successfully."</span>
}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="endpoint-group">
      <h3>Query Endpoints</h3>

      <!-- GET by appointment -->
      <div class="endpoint get" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-get">GET</span>
          <span class="ep-path">/payments/appointment/{appointmentId}</span>
          <span class="ep-summary">Get payment by appointment</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Retrieves the payment record for a given appointment. Used by patients, doctors, and admins on the appointment details page.</p>
            <div class="code-block">
              <button class="copy-btn" onclick="copyCode(this)">copy</button>curl http://localhost:8080/payments/appointment/<span style="color:#f59e0b">5</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;JWT_TOKEN&gt;"</span></div>
          </div>
        </div>
      </div>

      <!-- GET by patient -->
      <div class="endpoint get" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-get">GET</span>
          <span class="ep-path">/payments/patient/{patientId}</span>
          <span class="ep-summary">Get all payments for a patient</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Returns complete payment history for a patient including all statuses — PENDING, SUCCESS, FAILED, REFUNDED.</p>
            <div class="code-block">
              <button class="copy-btn" onclick="copyCode(this)">copy</button>curl http://localhost:8080/payments/patient/<span style="color:#f59e0b">12</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;JWT_TOKEN&gt;"</span></div>
          </div>
        </div>
      </div>

      <!-- GET by provider -->
      <div class="endpoint get" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-get">GET</span>
          <span class="ep-path">/payments/provider/{providerId}</span>
          <span class="ep-summary">Get payments by provider</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Returns all payments for appointments of a specific provider. Uses a native SQL JOIN query between <code>payments</code> and <code>appointments</code> tables, ordered by created date descending.</p>
            <div class="code-block">
              <button class="copy-btn" onclick="copyCode(this)">copy</button>curl http://localhost:8080/payments/provider/<span style="color:#f59e0b">3</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;ADMIN_JWT&gt;"</span></div>
          </div>
        </div>
      </div>

      <!-- GET by status -->
      <div class="endpoint get" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-get">GET</span>
          <span class="ep-path">/payments/status?status=SUCCESS</span>
          <span class="ep-summary">Filter payments by status</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Admin endpoint to filter all payments by status. Valid values: <code>PENDING</code>, <code>SUCCESS</code>, <code>FAILED</code>, <code>REFUNDED</code>. Invalid status throws 400 BadRequest.</p>
            <div class="code-block">
              <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Get all failed payments</span>
curl "http://localhost:8080/payments/status?status=FAILED" \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;ADMIN_JWT&gt;"</span>

<span style="color:#64748b"># Get all refunded payments</span>
curl "http://localhost:8080/payments/status?status=REFUNDED" \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;ADMIN_JWT&gt;"</span></div>
          </div>
        </div>
      </div>

      <!-- GET revenue -->
      <div class="endpoint get" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-get">GET</span>
          <span class="ep-path">/payments/revenue/total</span>
          <span class="ep-summary">Get total platform revenue</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Calculates total revenue from all <code>SUCCESS</code> status payments. Uses <code>SUM(amount)</code> JPA query. Used on admin analytics dashboard.</p>
            <div class="code-block">
              <button class="copy-btn" onclick="copyCode(this)">copy</button>curl http://localhost:8080/payments/revenue/total \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;ADMIN_JWT&gt;"</span></div>
            <div class="response-label">200 OK</div>
            <div class="code-block" style="border-radius:10px;">{
  <span style="color:#00d4ff">"totalRevenue"</span>: 125000.0,
  <span style="color:#00d4ff">"currency"</span>: <span style="color:#22c55e">"INR"</span>,
  <span style="color:#00d4ff">"message"</span>: <span style="color:#22c55e">"Total revenue from all successful payments"</span>
}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="endpoint-group">
      <h3>Admin Operations</h3>

      <!-- PUT update status -->
      <div class="endpoint put" onclick="toggleEp(this)">
        <div class="ep-header">
          <span class="ep-method method-put">PUT</span>
          <span class="ep-path">/payments/{paymentId}/status?status=SUCCESS</span>
          <span class="ep-summary">Update payment status manually</span>
          <span class="ep-arrow">▶</span>
        </div>
        <div class="ep-body">
          <div class="ep-inner">
            <p class="ep-desc">Admin endpoint to manually correct payment status. Also used by Razorpay webhook handler to update status automatically when Razorpay notifies the server. Valid statuses: PENDING, SUCCESS, FAILED, REFUNDED.</p>
            <div class="code-block">
              <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Admin corrects a failed payment to success</span>
curl -X PUT "http://localhost:8080/payments/1/status?status=SUCCESS" \
  -H <span style="color:#22c55e">"Authorization: Bearer &lt;ADMIN_JWT&gt;"</span></div>
            <div class="response-label">200 OK</div>
            <div class="code-block" style="border-radius:10px;">{
  <span style="color:#00d4ff">"message"</span>: <span style="color:#22c55e">"Payment status updated to: SUCCESS"</span>,
  <span style="color:#00d4ff">"paymentId"</span>: 1
}</div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- ══ 06 PAYMENT FLOW ═══════════════════════════════════════════ -->
  <section id="flow" class="reveal">
    <div class="section-label">06 Payment Flow</div>
    <h2>End-to-End Payment Flow</h2>
    <div class="flow-diagram">
      <div class="flow-step">
        <div class="flow-line">
          <div class="flow-dot flow-dot-blue">1</div>
          <div class="flow-connector"></div>
        </div>
        <div class="flow-content">
          <h4>Patient Books Appointment</h4>
          <p>appointment-service creates appointment with status <code>SCHEDULED</code></p>
        </div>
      </div>
      <div class="flow-step">
        <div class="flow-line">
          <div class="flow-dot flow-dot-blue">2</div>
          <div class="flow-connector"></div>
        </div>
        <div class="flow-content">
          <h4>POST /payments/initiate</h4>
          <p>Payment-service validates appointment (Feign call) → Creates Razorpay order → Returns <code>razorpayOrderId</code> · Status = PENDING</p>
        </div>
      </div>
      <div class="flow-step">
        <div class="flow-line">
          <div class="flow-dot flow-dot-purple">3</div>
          <div class="flow-connector"></div>
        </div>
        <div class="flow-content">
          <h4>Frontend Opens Razorpay Popup</h4>
          <p>Frontend uses <code>orderId</code> with Razorpay.js SDK to display payment interface to patient</p>
        </div>
      </div>
      <div class="flow-step">
        <div class="flow-line">
          <div class="flow-dot flow-dot-purple">4</div>
          <div class="flow-connector"></div>
        </div>
        <div class="flow-content">
          <h4>Patient Completes Payment</h4>
          <p>Razorpay sends back <code>orderId</code> + <code>paymentId</code> + <code>signature</code> to frontend</p>
        </div>
      </div>
      <div class="flow-step">
        <div class="flow-line">
          <div class="flow-dot flow-dot-green">5</div>
          <div class="flow-connector"></div>
        </div>
        <div class="flow-content">
          <h4>POST /payments/verify</h4>
          <p>HMAC-SHA256 signature verified → Status → SUCCESS (or FAILED) → Appointment confirmed</p>
        </div>
      </div>
      <div class="flow-step">
        <div class="flow-line">
          <div class="flow-dot flow-dot-green">6</div>
        </div>
        <div class="flow-content">
          <h4>On Cancellation → POST /payments/{id}/refund</h4>
          <p>Auto-triggered by appointment-service cancellation → Razorpay refund API → Status → REFUNDED</p>
        </div>
      </div>
    </div>
  </section>

  <!-- ══ 07 STATUS LIFECYCLE ═══════════════════════════════════════ -->
  <section id="status" class="reveal">
    <div class="section-label">07 Status Lifecycle</div>
    <h2>Payment Status Lifecycle</h2>

    <div class="flow-row">
      <div class="flow-state state-pending">PENDING</div>
      <span class="flow-arrow">→</span>
      <div class="flow-state state-success">SUCCESS</div>
      <span class="flow-arrow">→</span>
      <div class="flow-state state-refunded">REFUNDED</div>
    </div>
    <div class="flow-row">
      <div class="flow-state state-pending">PENDING</div>
      <span class="flow-arrow">→</span>
      <div class="flow-state state-failed">FAILED</div>
    </div>

    <div style="overflow-x:auto;">
      <table class="env-table">
        <thead><tr><th>Status</th><th>Trigger</th><th>Next States</th><th>Description</th></tr></thead>
        <tbody>
          <tr>
            <td><span style="color: var(--warn)">PENDING</span></td>
            <td>initiatePayment()</td>
            <td>SUCCESS, FAILED</td>
            <td>Payment order created, awaiting patient completion</td>
          </tr>
          <tr>
            <td><span style="color: var(--accent3)">SUCCESS</span></td>
            <td>verifyPayment() — valid signature</td>
            <td>REFUNDED</td>
            <td>Payment verified. Appointment fully confirmed.</td>
          </tr>
          <tr>
            <td><span style="color: #f87171">FAILED</span></td>
            <td>verifyPayment() — invalid signature</td>
            <td>—</td>
            <td>Payment tampered or failed. Patient must retry.</td>
          </tr>
          <tr>
            <td><span style="color: #a78bfa">REFUNDED</span></td>
            <td>initiateRefund()</td>
            <td>—</td>
            <td>Money returned. Triggered on appointment cancellation.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <!-- ══ 08 API TESTING ════════════════════════════════════════════ -->
  <section id="testing" class="reveal">
    <div class="section-label">08 API Testing</div>
    <h2>Complete API Testing Guide</h2>

    <div class="callout callout-amber">
      <div class="callout-icon">⚠️</div>
      <div class="callout-body"><strong>Prerequisites:</strong> All services running. MySQL databases created. Valid JWT token obtained from auth-service. Replace <code>&lt;JWT_TOKEN&gt;</code> with your actual token in all requests below.</div>
    </div>

    <h3>Step 1 — Get a JWT Token</h3>
    <div class="code-block" style="border-radius:10px; margin-bottom:28px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Register a patient first (if not exists)</span>
curl -X POST http://localhost:8080/auth/register \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -d <span style="color:#22c55e">'{
    "name": "Test Patient",
    "email": "patient@test.com",
    "password": "Password@123",
    "role": "PATIENT"
  }'</span>

<span style="color:#64748b"># Login to get JWT token</span>
curl -X POST http://localhost:8080/auth/login \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -d <span style="color:#22c55e">'{"email":"patient@test.com","password":"Password@123"}'</span>

<span style="color:#64748b"># Response contains token — save it:</span>
<span style="color:#7c3aed">TOKEN</span>=<span style="color:#22c55e">"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJwYXRpZW50QHRl..."</span></div>

    <h3>Step 2 — Full Payment Flow Test</h3>
    <div class="code-block" style="border-radius:10px; margin-bottom:28px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># ─── 1. Initiate payment ────────────────────────────────────</span>
curl -X POST http://localhost:8080/payments/initiate \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer $TOKEN"</span> \
  -d <span style="color:#22c55e">'{
    "appointmentId": 5,
    "patientId": 12,
    "amount": 500.00,
    "paymentMethod": "UPI",
    "currency": "INR"
  }'</span>

<span style="color:#64748b"># ─── 2. Verify payment (use orderId from step 1) ─────────────</span>
curl -X POST http://localhost:8080/payments/verify \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer $TOKEN"</span> \
  -d <span style="color:#22c55e">'{
    "razorpayOrderId": "order_NiXe5u3kZ9XYAB",
    "razorpayPaymentId": "pay_ABC123XYZ456",
    "razorpaySignature": "&lt;hmac_signature&gt;"
  }'</span>

<span style="color:#64748b"># ─── 3. Check payment status ────────────────────────────────</span>
curl http://localhost:8080/payments/appointment/5 \
  -H <span style="color:#22c55e">"Authorization: Bearer $TOKEN"</span></div>

    <h3>Step 3 — Refund Test</h3>
    <div class="code-block" style="border-radius:10px; margin-bottom:28px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Initiate refund for paymentId=1</span>
curl -X POST http://localhost:8080/payments/1/refund \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span>

<span style="color:#64748b"># Verify refund status</span>
curl http://localhost:8080/payments/1 \
  -H <span style="color:#22c55e">"Authorization: Bearer $TOKEN"</span></div>

    <h3>Step 4 — Admin Analytics Tests</h3>
    <div class="code-block" style="border-radius:10px; margin-bottom:28px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Total platform revenue</span>
curl http://localhost:8080/payments/revenue/total \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span>

<span style="color:#64748b"># All failed payments</span>
curl "http://localhost:8080/payments/status?status=FAILED" \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span>

<span style="color:#64748b"># All refunded payments</span>
curl "http://localhost:8080/payments/status?status=REFUNDED" \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span>

<span style="color:#64748b"># Payments by provider ID</span>
curl http://localhost:8080/payments/provider/3 \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span>

<span style="color:#64748b"># Manually update status</span>
curl -X PUT "http://localhost:8080/payments/1/status?status=SUCCESS" \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span></div>

    <h3>Error Scenarios to Test</h3>
    <div class="code-block" style="border-radius:10px; margin-bottom:28px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># ✗ Duplicate payment — should return 409 Conflict</span>
curl -X POST http://localhost:8080/payments/initiate \
  -H <span style="color:#22c55e">"Content-Type: application/json"</span> \
  -H <span style="color:#22c55e">"Authorization: Bearer $TOKEN"</span> \
  -d <span style="color:#22c55e">'{"appointmentId": 5, "patientId": 12, "amount": 500, "paymentMethod": "UPI"}'</span>

<span style="color:#64748b"># ✗ Invalid status filter — should return 400 Bad Request</span>
curl "http://localhost:8080/payments/status?status=INVALID_STATUS" \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span>

<span style="color:#64748b"># ✗ Refund non-success payment — should return 400 Bad Request</span>
curl -X POST http://localhost:8080/payments/99/refund \
  -H <span style="color:#22c55e">"Authorization: Bearer $ADMIN_TOKEN"</span>

<span style="color:#64748b"># ✗ No auth header — should return 401 Unauthorized</span>
curl http://localhost:8080/payments/revenue/total</div>

    <h3>Swagger UI Testing</h3>
    <div class="callout callout-green">
      <div class="callout-icon">🧪</div>
      <div class="callout-body">Open <code>http://localhost:8085/swagger-ui.html</code> for interactive API testing. All endpoints are available with try-it-out enabled. Authorize with Bearer token using the Authorize button at the top right.</div>
    </div>
  </section>

  <!-- ══ 09 ENVIRONMENT ════════════════════════════════════════════ -->
  <section id="env" class="reveal">
    <div class="section-label">09 Environment</div>
    <h2>Environment Variables</h2>

    <h3>Payment Service (<code>application.yml</code>)</h3>
    <div style="overflow-x:auto; margin-bottom:28px;">
      <table class="env-table">
        <thead><tr><th>Variable</th><th>Default</th><th>Required</th><th>Description</th></tr></thead>
        <tbody>
          <tr><td>JWT_SECRET</td><td>—</td><td><span class="req-badge">required</span></td><td>Must match across all services. Min 256-bit HMAC key.</td></tr>
          <tr><td>DB_USERNAME</td><td>medibook_user</td><td><span class="opt-badge">optional</span></td><td>MySQL username for payment_db</td></tr>
          <tr><td>DB_PASSWORD</td><td>medibook_pass</td><td><span class="opt-badge">optional</span></td><td>MySQL password for payment_db</td></tr>
          <tr><td>RAZORPAY-API-KEY</td><td>—</td><td><span class="req-badge">required</span></td><td>Razorpay API key ID from dashboard</td></tr>
          <tr><td>RAZORPAY-KEY-SECRET</td><td>—</td><td><span class="req-badge">required</span></td><td>Razorpay key secret for HMAC signing</td></tr>
          <tr><td>EUREKA_DEFAULT_ZONE</td><td>localhost:8761</td><td><span class="opt-badge">optional</span></td><td>Eureka server URL with credentials</td></tr>
        </tbody>
      </table>
    </div>

    <h3>Auth Service Additional Variables</h3>
    <div style="overflow-x:auto;">
      <table class="env-table">
        <thead><tr><th>Variable</th><th>Description</th></tr></thead>
        <tbody>
          <tr><td>MAIL_HOST</td><td>SMTP host (default: smtp.gmail.com)</td></tr>
          <tr><td>MAIL_PORT</td><td>SMTP port (default: 587)</td></tr>
          <tr><td>MAIL_USERNAME</td><td>Gmail address for sending OTP</td></tr>
          <tr><td>MAIL_PASSWORD</td><td>Gmail App Password (not account password)</td></tr>
        </tbody>
      </table>
    </div>
  </section>

  <!-- ══ 10 QUICK START ════════════════════════════════════════════ -->
  <section id="startup" class="reveal">
    <div class="section-label">10 Quick Start</div>
    <h2>Running the Stack</h2>

    <h3>Service Startup Order</h3>
    <div class="code-block" style="border-radius:10px; margin-bottom:28px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># ── 1. Start Eureka Server FIRST ─────────────────────────────</span>
cd eureka-server && mvn spring-boot:run

<span style="color:#64748b"># ── 2. Start API Gateway SECOND ─────────────────────────────</span>
cd api-gateway && mvn spring-boot:run

<span style="color:#64748b"># ── 3. Start remaining services in any order ────────────────</span>
cd auth-service        && mvn spring-boot:run &
cd provider-service    && mvn spring-boot:run &
cd schedule-service    && mvn spring-boot:run &
cd appointment-service && mvn spring-boot:run &
cd payment-service     && mvn spring-boot:run &</div>

    <h3>Port Reference</h3>
    <div style="overflow-x:auto; margin-bottom:28px;">
      <table class="env-table">
        <thead><tr><th>Service</th><th>Port</th><th>URL</th></tr></thead>
        <tbody>
          <tr><td>Eureka Server</td><td>8761</td><td>http://localhost:8761 (admin/medibook123)</td></tr>
          <tr><td>API Gateway</td><td>8080</td><td>http://localhost:8080</td></tr>
          <tr><td>Auth Service</td><td>8081</td><td>http://localhost:8081</td></tr>
          <tr><td>Provider Service</td><td>8082</td><td>http://localhost:8082</td></tr>
          <tr><td>Schedule Service</td><td>8083</td><td>http://localhost:8083</td></tr>
          <tr><td>Appointment Service</td><td>8084</td><td>http://localhost:8084</td></tr>
          <tr><td style="color: var(--accent)">Payment Service ★</td><td style="color:var(--accent)">8085</td><td>http://localhost:8085 · Swagger: /swagger-ui.html</td></tr>
        </tbody>
      </table>
    </div>

    <div class="callout callout-green">
      <div class="callout-icon">✅</div>
      <div class="callout-body"><strong>All APIs are routed via Gateway on port 8080.</strong> You never need to call payment-service on :8085 directly in production — use <code>http://localhost:8080/payments/...</code> for all requests. Direct access on :8085 is available for Swagger UI and local debugging only.</div>
    </div>

    <h3>Database Setup</h3>
    <div class="code-block" style="border-radius:10px; margin-bottom:28px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Create MySQL user and databases</span>
mysql -u root -p &lt;&lt;EOF
CREATE USER <span style="color:#22c55e">'medibook_user'</span>@<span style="color:#22c55e">'localhost'</span> IDENTIFIED BY <span style="color:#22c55e">'medibook_pass'</span>;
GRANT ALL PRIVILEGES ON *.* TO <span style="color:#22c55e">'medibook_user'</span>@<span style="color:#22c55e">'localhost'</span>;
FLUSH PRIVILEGES;
<span style="color:#64748b">-- Databases are auto-created by Spring (createDatabaseIfNotExist=true)</span>
EOF</div>

    <h3>Parent POM Build (all modules)</h3>
    <div class="code-block" style="border-radius:10px;">
      <button class="copy-btn" onclick="copyCode(this)">copy</button><span style="color:#64748b"># Build all services from root</span>
mvn clean install -DskipTests

<span style="color:#64748b"># Build payment-service only</span>
cd payment-service && mvn clean package -DskipTests</div>
  </section>

  <!-- ══ FOOTER ════════════════════════════════════════════════════ -->
  <footer style="text-align:center; padding: 48px 0 32px; border-top: 1px solid var(--border); color: var(--muted); font-size: .8rem; font-family: 'JetBrains Mono', monospace;">
    <div style="margin-bottom: 8px; color: var(--accent)">MediBook Microservices</div>
    feature/UC5-payment-service · Spring Boot 3.2 · Java 17 · Razorpay · MySQL · Eureka
  </footer>

</div>

<script>
  // ── Intersection Observer for reveal animations ──
  const obs = new IntersectionObserver((entries) => {
    entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('visible'); obs.unobserve(e.target); } });
  }, { threshold: 0.08 });
  document.querySelectorAll('.reveal').forEach(el => obs.observe(el));

  // ── Endpoint toggle ──
  function toggleEp(el) {
    el.classList.toggle('open');
  }

  // ── Code tab switching ──
  function switchTab(event, targetId) {
    event.stopPropagation();
    const tabs = event.target.parentElement;
    const body = tabs.parentElement;
    tabs.querySelectorAll('.code-tab').forEach(t => t.classList.remove('active'));
    body.querySelectorAll('.code-panel').forEach(p => p.classList.remove('active'));
    event.target.classList.add('active');
    const panel = document.getElementById(targetId);
    if (panel) panel.classList.add('active');
  }

  // ── Copy button ──
  function copyCode(btn) {
    const block = btn.parentElement;
    const text = block.innerText.replace('copy\n', '').replace('copy', '');
    navigator.clipboard.writeText(text.trim()).then(() => {
      btn.textContent = 'copied!';
      setTimeout(() => btn.textContent = 'copy', 2000);
    });
  }

  // ── Stagger animations on load ──
  document.querySelectorAll('section').forEach((s, i) => {
    s.style.animationDelay = `${i * 0.05}s`;
  });
</script>
</body>
</html>
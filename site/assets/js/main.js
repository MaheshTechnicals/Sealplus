/**
 * Seal Plus Website — main.js
 * Author: Mahesh Technicals
 * Features:
 *   - GitHub Releases API with localStorage cache (1 hr TTL)
 *   - Markdown-lite renderer for changelogs
 *   - Scroll-reveal animations (IntersectionObserver)
 *   - Sticky navbar with scroll detection
 *   - Mobile navigation toggle
 *   - Sponsor modal
 *   - Toast notifications
 *   - Manual refresh
 */

/* ── Config ─────────────────────────────────────────────────── */
const CONFIG = {
  apiUrl:    'https://api.github.com/repos/MaheshTechnicals/Sealplus/releases',
  cacheKey:  'sealplus_releases_cache',
  cacheTTL:  60 * 60 * 1000,   // 1 hour in ms
  maxReleases: 15,
};

/* ── DOM Refs ───────────────────────────────────────────────── */
const $ = id => document.getElementById(id);

/* ── Navbar Scroll ──────────────────────────────────────────── */
(function initNavbar() {
  const navbar = document.querySelector('.navbar');
  if (!navbar) return;

  window.addEventListener('scroll', () => {
    navbar.classList.toggle('scrolled', window.scrollY > 40);
  }, { passive: true });
})();

/* ── Mobile Nav Toggle ─────────────────────────────────────── */
(function initMobileNav() {
  const toggle = document.querySelector('.nav-toggle');
  const links  = document.querySelector('.nav-links');
  if (!toggle || !links) return;

  toggle.addEventListener('click', () => {
    toggle.classList.toggle('open');
    links.classList.toggle('mobile-open');
  });

  // Close on link click
  links.querySelectorAll('a').forEach(a => {
    a.addEventListener('click', () => {
      toggle.classList.remove('open');
      links.classList.remove('mobile-open');
    });
  });
})();

/* ── Active Nav Link ────────────────────────────────────────── */
(function initActiveNavLink() {
  const sections  = document.querySelectorAll('section[id]');
  const navLinks  = document.querySelectorAll('.nav-links a[href^="#"]');
  if (!sections.length || !navLinks.length) return;

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        navLinks.forEach(a => {
          a.classList.toggle(
            'active',
            a.getAttribute('href') === '#' + entry.target.id
          );
        });
      }
    });
  }, { threshold: 0.4 });

  sections.forEach(s => observer.observe(s));
})();

/* ── Scroll-Reveal Animations ──────────────────────────────── */
(function initScrollReveal() {
  const targets = document.querySelectorAll('.reveal, .reveal-left, .reveal-right');
  if (!targets.length) return;

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });

  targets.forEach(el => observer.observe(el));
})();

/* ── Toast Notifications ────────────────────────────────────── */
const toast = (() => {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  return function(message, type = 'info', duration = 3500) {
    const el = document.createElement('div');
    const icons = { success: '✅', error: '❌', info: 'ℹ️' };
    el.className = `toast ${type}`;
    el.innerHTML = `<span>${icons[type] || icons.info}</span><span>${message}</span>`;
    container.appendChild(el);

    requestAnimationFrame(() => {
      requestAnimationFrame(() => el.classList.add('show'));
    });

    setTimeout(() => {
      el.classList.remove('show');
      el.addEventListener('transitionend', () => el.remove());
    }, duration);
  };
})();

/* ── Load Sponsors from sponsors.json ───────────────────────── */
(function loadSponsors() {
  const stripContainer = document.getElementById('sponsors-strip-list');
  const modalContainer = document.getElementById('sponsors-modal-list');
  const viewAllBtn     = document.getElementById('sponsors-view-all');
  if (!stripContainer && !modalContainer) return;

  /* Palette for avatar backgrounds */
  const avatarColors = [
    'linear-gradient(135deg, #7b2ff7, #4facfe)',
    'linear-gradient(135deg, #f72585, #ff6f91)',
    'linear-gradient(135deg, #00e676, #00c853)',
    'linear-gradient(135deg, #ff9800, #ff5722)',
    'linear-gradient(135deg, #4facfe, #00f2fe)',
    'linear-gradient(135deg, #e040fb, #7c4dff)',
    'linear-gradient(135deg, #ffd600, #ff6d00)',
    'linear-gradient(135deg, #00bcd4, #2196f3)',
  ];

  function getInitials(name) {
    return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  const VISIBLE_LIMIT = 6;

  fetch('sponsors.json')
    .then(res => {
      if (!res.ok) throw new Error('Failed to load sponsors.json');
      return res.json();
    })
    .then(data => {
      const sponsors = data.sponsors || [];

      /* ── Strip: profile cards (limit 5 initially) ── */
      if (stripContainer) {
        const stripHTML = sponsors.map((s, i) => {
          const bg = avatarColors[i % avatarColors.length];
          const initials = getInitials(s.name);
          const hidden = i >= VISIBLE_LIMIT ? ' hidden-sponsor' : '';
          return `<div class="sponsor-card${hidden}">
            <div class="sponsor-avatar" style="background:${bg}">${initials}</div>
            <div class="sponsor-name">${s.name}</div>
            <div class="sponsor-badge"><span class="heart" aria-hidden="true">❤️</span> Sponsor</div>
          </div>`;
        }).join('');
        stripContainer.innerHTML = stripHTML;

        /* Show View All button if more than VISIBLE_LIMIT */
        if (viewAllBtn && sponsors.length > VISIBLE_LIMIT) {
          viewAllBtn.style.display = '';
          viewAllBtn.textContent = `View All Sponsors (${sponsors.length})`;
          viewAllBtn.addEventListener('click', () => {
            stripContainer.classList.toggle('show-all');
            const expanded = stripContainer.classList.contains('show-all');
            viewAllBtn.textContent = expanded
              ? 'Show Less'
              : `View All Sponsors (${sponsors.length})`;
          });
        }
      }

      /* ── Modal: compact profile cards ── */
      if (modalContainer) {
        const modalHTML = sponsors.map((s, i) => {
          const bg = avatarColors[i % avatarColors.length];
          const initials = getInitials(s.name);
          return `<div class="modal-sponsor-card">
            <div class="modal-sponsor-avatar" style="background:${bg}">${initials}</div>
            <div class="modal-sponsor-name">${s.name}</div>
          </div>`;
        }).join('');
        modalContainer.innerHTML = modalHTML;
      }
    })
    .catch(err => console.error('Sponsors load error:', err));
})();

/* ── Sponsor Modal ──────────────────────────────────────────── */
(function initSponsorModal() {
  const overlay   = document.querySelector('.modal-overlay');
  const closeBtns = document.querySelectorAll('.modal-close');
  const triggers  = document.querySelectorAll('[data-sponsor-modal]');
  if (!overlay) return;

  const open  = () => overlay.classList.add('open');
  const close = () => overlay.classList.remove('open');

  triggers.forEach(btn => btn.addEventListener('click', open));
  closeBtns.forEach(btn => btn.addEventListener('click', close));

  overlay.addEventListener('click', e => {
    if (e.target === overlay) close();
  });

  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') close();
  });
})();

/* ── Animate Counter ────────────────────────────────────────── */
function animateCounter(el, end, duration = 1400, suffix = '') {
  const start = 0;
  const startTime = performance.now();

  function update(now) {
    const elapsed  = now - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const ease     = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.floor(start + (end - start) * ease) + suffix;
    if (progress < 1) requestAnimationFrame(update);
  }
  requestAnimationFrame(update);
}

(function initCounters() {
  const counters = document.querySelectorAll('[data-counter]');
  if (!counters.length) return;

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const el     = entry.target;
        const end    = parseInt(el.dataset.counter, 10);
        const suffix = el.dataset.suffix || '';
        animateCounter(el, end, 1500, suffix);
        observer.unobserve(el);
      }
    });
  }, { threshold: 0.5 });

  counters.forEach(el => observer.observe(el));
})();

/* ── GitHub Releases ────────────────────────────────────────── */

/**
 * Returns cached releases if fresh, otherwise null.
 */
function getCachedReleases() {
  try {
    const raw = localStorage.getItem(CONFIG.cacheKey);
    if (!raw) return null;
    const { data, timestamp } = JSON.parse(raw);
    if (Date.now() - timestamp > CONFIG.cacheTTL) return null;
    return data;
  } catch {
    return null;
  }
}

/**
 * Saves releases to localStorage with current timestamp.
 */
function cacheReleases(data) {
  try {
    localStorage.setItem(CONFIG.cacheKey, JSON.stringify({ data, timestamp: Date.now() }));
  } catch { /* ignore quota errors */ }
}

/**
 * Fetches releases from GitHub API.
 */
async function fetchReleases(forceRefresh = false) {
  if (!forceRefresh) {
    const cached = getCachedReleases();
    if (cached) return cached;
  }

  const resp = await fetch(`${CONFIG.apiUrl}?per_page=${CONFIG.maxReleases}`);
  if (!resp.ok) throw new Error(`GitHub API error: ${resp.status}`);
  const data = await resp.json();
  cacheReleases(data);
  return data;
}

/* ── Markdown-lite Renderer ─────────────────────────────────── */
function simpleMarkdown(text) {
  if (!text) return '';

  return text
    // Escape HTML
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    // Headings
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    // Bold & italic
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/_(.+?)_/g, '<em>$1</em>')
    // Inline code
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // Links
    .replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
    // Unordered list items
    .replace(/^[\-\*] (.+)$/gm, '<li>$1</li>')
    // Ordered list items
    .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
    // Line breaks → <br> (except before headings / list items)
    .replace(/\n(?!<(h[1-3]|li))/g, '<br>');
}

/* ── Format date ────────────────────────────────────────────── */
function formatDate(iso) {
  return new Date(iso).toLocaleDateString('en-US', {
    year: 'numeric', month: 'long', day: 'numeric',
  });
}

/* ── File size helper ───────────────────────────────────────── */
function humanSize(bytes) {
  if (!bytes) return '';
  if (bytes >= 1048576) return `${(bytes / 1048576).toFixed(1)} MB`;
  if (bytes >= 1024)    return `${(bytes / 1024).toFixed(0)} KB`;
  return `${bytes} B`;
}

/* ── APK asset label ────────────────────────────────────────── */
function apkLabel(name) {
  const n = name.toLowerCase();
  if (n.includes('universal')) return { label: '⬇️ Universal APK', cls: 'universal' };
  if (n.includes('arm64'))     return { label: '⬇️ ARM64-v8a', cls: '' };
  if (n.includes('arm'))       return { label: '⬇️ ARMv7', cls: '' };
  if (n.includes('x86_64'))    return { label: '⬇️ x86_64', cls: '' };
  if (n.includes('x86'))       return { label: '⬇️ x86', cls: '' };
  if (n.endsWith('.apk'))      return { label: '⬇️ Download APK', cls: '' };
  return null;
}

/* ── Render a single release card ───────────────────────────── */
function renderReleaseCard(release, index) {
  const isLatest       = index === 0;
  const isPrerelease   = release.prerelease;
  const version        = release.tag_name;
  const name           = release.name || version;
  const publishedAt    = formatDate(release.published_at);
  const body           = release.body || '_No changelog provided._';
  const assets         = (release.assets || []).filter(a => a.name.endsWith('.apk'));
  const isLongChangelog = body.length > 400;

  const badgesHtml = [
    isLatest     ? `<span class="release-badge latest">✦ Latest</span>` : '',
    isPrerelease ? `<span class="release-badge prerelease">⚡ Pre-release</span>` : '',
  ].join('');

  const changelogClass = isLongChangelog ? 'release-changelog expandable' : 'release-changelog';
  const expandBtn = isLongChangelog
    ? `<button class="btn-expand" onclick="toggleChangelog(this)">
         Show more <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
           stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
           <polyline points="6 9 12 15 18 9"/>
         </svg>
       </button>`
    : '';

  const downloadsHtml = assets.length
    ? `<div class="release-downloads">
         <div class="downloads-label">Download APK</div>
         <div class="downloads-grid">
           ${assets.map(asset => {
             const info = apkLabel(asset.name);
             if (!info) return '';
             const size = humanSize(asset.size);
             return `<a href="${asset.browser_download_url}"
                        class="btn btn-download ${info.cls}"
                        target="_blank" rel="noopener"
                        title="${asset.name}">
                       ${info.label}${size ? ` <small style="opacity:.6;font-weight:400;">${size}</small>` : ''}
                     </a>`;
           }).join('')}
         </div>
       </div>`
    : `<p style="font-size:.85rem;color:var(--text-muted);margin-top:16px;">
         No APK assets available for this release.
       </p>`;

  return `
    <div class="glass-card release-card reveal">
      <div class="release-header">
        <div class="release-meta">
          <div class="release-version">${escapeHtml(version)}</div>
          <div class="release-name">${escapeHtml(name)}</div>
          <div class="release-date">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <rect x="3" y="4" width="18" height="18" rx="2"/>
              <line x1="16" y1="2" x2="16" y2="6"/>
              <line x1="8"  y1="2" x2="8"  y2="6"/>
              <line x1="3"  y1="10" x2="21" y2="10"/>
            </svg>
            ${publishedAt}
          </div>
        </div>
        <div class="release-badges">${badgesHtml}</div>
      </div>

      <div class="${changelogClass}">
        <div class="changelog-body">${simpleMarkdown(body)}</div>
      </div>
      ${expandBtn}

      ${downloadsHtml}
    </div>
  `;
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/* ── Expand / collapse changelog ────────────────────────────── */
function toggleChangelog(btn) {
  const card = btn.closest('.release-card');
  const box  = card.querySelector('.release-changelog');
  const expanded = box.classList.toggle('expanded');
  btn.innerHTML = expanded
    ? `Show less <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
         <polyline points="18 15 12 9 6 15"/></svg>`
    : `Show more <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
         <polyline points="6 9 12 15 18 9"/></svg>`;
}
window.toggleChangelog = toggleChangelog;

/* ── Skeleton loader ────────────────────────────────────────── */
function renderSkeletons(count = 3) {
  return Array.from({ length: count }, () => `
    <div class="glass-card skeleton-card">
      <div class="skeleton sk-line sk-w30"></div>
      <div class="skeleton sk-line sk-w60" style="margin-top:6px;"></div>
      <div class="skeleton sk-line sk-w45" style="margin-top:4px;height:10px;"></div>
      <div class="skeleton sk-h40"></div>
      <div style="display:flex;gap:10px;margin-top:16px;">
        <div class="skeleton sk-line" style="width:130px;height:36px;border-radius:8px;"></div>
        <div class="skeleton sk-line" style="width:110px;height:36px;border-radius:8px;"></div>
      </div>
    </div>
  `).join('');
}

/* ── Toggle older releases accordion ────────────────────────── */
function toggleOlderReleases(btn) {
  const wrap = btn.closest('.older-releases-wrap');
  const olderList = wrap.querySelector('.older-releases-list');
  const isOpen = olderList.classList.toggle('open');
  btn.classList.toggle('open', isOpen);
  const count = olderList.querySelectorAll('.release-card').length;
  btn.querySelector('.toggle-label').textContent = isOpen
    ? `Hide older releases`
    : `Show ${count} older release${count !== 1 ? 's' : ''}`;

  // Reveal cards that just became visible
  if (isOpen) {
    requestAnimationFrame(() => {
      olderList.querySelectorAll('.reveal:not(.visible)').forEach(el => {
        el.classList.add('visible');
      });
    });
  }
}
window.toggleOlderReleases = toggleOlderReleases;

/* ── Main: Load Releases ────────────────────────────────────── */
async function loadReleases(forceRefresh = false) {
  const list       = document.querySelector('.releases-list');
  const infoEl     = document.querySelector('.releases-info');
  const refreshBtn = document.querySelector('.btn-refresh');
  if (!list) return;

  // Show single skeleton
  list.innerHTML = renderSkeletons(1);
  refreshBtn?.classList.add('loading');

  try {
    const releases = await fetchReleases(forceRefresh);

    if (!releases || releases.length === 0) {
      list.innerHTML = `
        <div class="releases-msg">
          <div class="msg-icon">📭</div>
          <p>No releases found yet. Check back soon!</p>
        </div>`;
      return;
    }

    const [latest, ...older] = releases;

    // Latest release card (always visible, no transition delay)
    const latestHtml = renderReleaseCard(latest, 0);

    // All older releases inside accordion
    let olderHtml = '';
    if (older.length > 0) {
      const olderCards = older.map((r, i) => renderReleaseCard(r, i + 1)).join('');
      olderHtml = `
        <div class="older-releases-wrap">
          <button class="btn-older-toggle" onclick="toggleOlderReleases(this)" aria-expanded="false">
            <span class="toggle-label">Show ${older.length} older release${older.length !== 1 ? 's' : ''}</span>
            <svg class="toggle-icon" width="18" height="18" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>
          <div class="older-releases-list" role="region">
            ${olderCards}
          </div>
        </div>`;
    }

    list.innerHTML = latestHtml + olderHtml;

    // Update info text
    if (infoEl) {
      let cacheAgo = '';
      try {
        const raw = localStorage.getItem(CONFIG.cacheKey);
        if (raw) {
          const ago = Math.round((Date.now() - JSON.parse(raw).timestamp) / 60000);
          cacheAgo = ` · cached ${ago < 1 ? 'just now' : ago + 'm ago'}`;
        }
      } catch {}
      infoEl.innerHTML = `<span>${releases.length}</span> release${releases.length > 1 ? 's' : ''} available${cacheAgo}`;
    }

    // Immediately reveal the latest card (it's in viewport)
    requestAnimationFrame(() => {
      list.querySelector('.release-card')?.classList.add('visible');
    });

    if (forceRefresh) toast('Releases updated!', 'success');

  } catch (err) {
    list.innerHTML = `
      <div class="glass-card releases-msg">
        <div class="msg-icon">⚠️</div>
        <p style="margin-bottom:12px;">Failed to load releases. Please try again.</p>
        <button class="btn btn-secondary" onclick="loadReleases(true)">Retry</button>
      </div>`;
    console.error('[SealPlus] Releases fetch error:', err);
    if (forceRefresh) toast('Failed to refresh.', 'error');
  } finally {
    refreshBtn?.classList.remove('loading');
  }
}

/* ── Refresh button ─────────────────────────────────────────── */
(function initRefreshBtn() {
  const btn = document.querySelector('.btn-refresh');
  if (!btn) return;
  btn.addEventListener('click', () => {
    // Invalidate cache
    localStorage.removeItem(CONFIG.cacheKey);
    loadReleases(true);
    toast('Refreshing releases…', 'info', 2000);
  });
})();

/* ══════════════════════════════════════════════════════════════
   SCREENSHOTS SLIDER
═══════════════════════════════════════════════════════════════ */
const ssSlider = (() => {
  const TOTAL       = 9;
  const AUTO_MS     = 3800;   // auto-advance interval
  const DRAG_THRESH = 40;     // px needed to register a swipe

  let current   = 0;
  let autoTimer = null;
  let dragStartX = 0;
  let dragDeltaX = 0;
  let isDragging = false;

  let track, slides, dots;

  /**
   * offsetWidth — reads CSS layout width, NOT affected by scale() transforms.
   * This is the key fix: getBoundingClientRect includes scale, offsetWidth does not.
   */
  function getSlideWidth() {
    if (!slides || !slides[0]) return 260;
    return slides[0].offsetWidth;        // layout width, transform-independent
  }

  function getGap() {
    if (!track) return 24;
    // gap is a layout property — use parseFloat for sub-pixel accuracy
    return parseFloat(getComputedStyle(track).gap) || 24;
  }

  function getViewportWidth() {
    if (!track) return window.innerWidth;
    return track.parentElement.offsetWidth; // layout width, transform-independent
  }

  function calcOffset(index) {
    const sw     = getSlideWidth();       // stable, never changes with active state
    const gap    = getGap();
    const vpW    = getViewportWidth();
    const centre = (vpW - sw) / 2;        // offset to place slide 0 in centre
    return centre - index * (sw + gap);   // shift left by N slot-widths
  }

  function applyOffset(extra = 0) {
    const base = calcOffset(current);
    track.style.transform = `translateX(${base + extra}px)`;
  }

  function updateSlides() {
    slides.forEach((s, i) => {
      s.classList.toggle('active', i === current);
      s.setAttribute('aria-hidden', i !== current ? 'true' : 'false');
    });
  }

  function updateDots() {
    dots.forEach((d, i) => d.classList.toggle('active', i === current));
  }

  function goTo(index) {
    current = Math.max(0, Math.min(index, TOTAL - 1));
    track.classList.remove('dragging');
    applyOffset();
    updateSlides();
    updateDots();
  }

  function next() { goTo(current === TOTAL - 1 ? 0 : current + 1); }
  function prev() { goTo(current === 0 ? TOTAL - 1 : current - 1); }

  /* Auto-advance */
  function startAuto() {
    clearInterval(autoTimer);
    autoTimer = setInterval(next, AUTO_MS);
  }
  function stopAuto() { clearInterval(autoTimer); }

  /* Pointer / touch events */
  function onPointerDown(e) {
    isDragging  = true;
    dragStartX  = e.type === 'touchstart' ? e.touches[0].clientX : e.clientX;
    dragDeltaX  = 0;
    track.classList.add('dragging');
    stopAuto();
  }

  function onPointerMove(e) {
    if (!isDragging) return;
    const x  = e.type === 'touchmove' ? e.touches[0].clientX : e.clientX;
    dragDeltaX = x - dragStartX;
    applyOffset(dragDeltaX);
    // Prevent vertical scroll hijacking only when swiping horizontally
    if (Math.abs(dragDeltaX) > 10 && e.cancelable) e.preventDefault();
  }

  function onPointerUp() {
    if (!isDragging) return;
    isDragging = false;
    if (dragDeltaX < -DRAG_THRESH)       next();
    else if (dragDeltaX > DRAG_THRESH)   prev();
    else                                  goTo(current); // snap back
    startAuto();
  }

  /* Keyboard */
  function onKeyDown(e) {
    if (e.key === 'ArrowLeft')  { prev(); stopAuto(); }
    if (e.key === 'ArrowRight') { next(); stopAuto(); }
  }

  /* Build dots */
  function buildDots() {
    const container = document.getElementById('ssDots');
    if (!container) return [];
    container.innerHTML = '';
    return Array.from({ length: TOTAL }, (_, i) => {
      const btn = document.createElement('button');
      btn.className    = 'ss-dot' + (i === 0 ? ' active' : '');
      btn.setAttribute('role', 'tab');
      btn.setAttribute('aria-label', `Screenshot ${i + 1}`);
      btn.addEventListener('click', () => { goTo(i); stopAuto(); startAuto(); });
      container.appendChild(btn);
      return btn;
    });
  }

  function init() {
    track  = document.getElementById('ssTrack');
    if (!track) return;

    slides = Array.from(track.querySelectorAll('.ss-slide'));
    dots   = buildDots();

    // Initial state
    goTo(0);

    // Touch events (passive: false on move to allow preventDefault)
    track.addEventListener('touchstart',  onPointerDown, { passive: true });
    track.addEventListener('touchmove',   onPointerMove, { passive: false });
    track.addEventListener('touchend',    onPointerUp);
    track.addEventListener('touchcancel', onPointerUp);

    // Mouse drag
    track.addEventListener('mousedown',  onPointerDown);
    window.addEventListener('mousemove', onPointerMove);
    window.addEventListener('mouseup',   onPointerUp);

    // Keyboard
    document.addEventListener('keydown', onKeyDown);

    // Pause on hover
    track.parentElement.addEventListener('mouseenter', stopAuto);
    track.parentElement.addEventListener('mouseleave', startAuto);

    // Recalculate on resize
    window.addEventListener('resize', () => {
      applyOffset();
    }, { passive: true });

    startAuto();
  }

  return { init, next, prev, goTo };
})();

/* ── Init ───────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  loadReleases();
  ssSlider.init();
});

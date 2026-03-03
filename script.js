/* ============================
   SEAL PLUS — script.js
   ============================ */

(function () {
  'use strict';

  /* ─── Helpers ─────────────────────────────────── */
  const $ = (sel, ctx = document) => ctx.querySelector(sel);
  const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

  /* ─── Navbar scroll ───────────────────────────── */
  const navbar = $('.navbar');
  const onScroll = () => {
    if (!navbar) return;
    navbar.classList.toggle('scrolled', window.scrollY > 20);
    backToTop && backToTop.classList.toggle('visible', window.scrollY > 300);
  };
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  /* ─── Hamburger menu ──────────────────────────── */
  const hamburger = $('.hamburger');
  const navLinks  = $('.nav-links');
  if (hamburger && navLinks) {
    hamburger.addEventListener('click', () => {
      hamburger.classList.toggle('open');
      navLinks.classList.toggle('open');
    });
    // close on link click
    navLinks.addEventListener('click', e => {
      if (e.target.tagName === 'A') {
        hamburger.classList.remove('open');
        navLinks.classList.remove('open');
      }
    });
  }

  /* ─── AOS (scroll reveal) ─────────────────────── */
  const aosObserver = new IntersectionObserver((entries) => {
    entries.forEach(e => { if (e.isIntersecting) { e.target.classList.add('visible'); aosObserver.unobserve(e.target); } });
  }, { threshold: 0.08, rootMargin: '0px 0px -20px 0px' });

  $$('[data-aos]').forEach(el => aosObserver.observe(el));

  // Trigger immediately for elements already in viewport
  requestAnimationFrame(() => {
    $$('[data-aos]').forEach(el => {
      const r = el.getBoundingClientRect();
      if (r.top < window.innerHeight && r.bottom > 0) {
        el.classList.add('visible');
        aosObserver.unobserve(el);
      }
    });
  });

  /* ─── Counter animation ───────────────────────── */
  function animateCounter(el) {
    const target = parseInt(el.dataset.count, 10);
    if (isNaN(target)) return;
    const duration = 1800;
    const start = performance.now();
    const step = (now) => {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      // ease-out
      const eased = 1 - Math.pow(1 - progress, 3);
      el.textContent = Math.round(eased * target).toLocaleString();
      if (progress < 1) requestAnimationFrame(step);
      else el.textContent = target.toLocaleString();
    };
    requestAnimationFrame(step);
  }

  const counterObserver = new IntersectionObserver((entries) => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        animateCounter(e.target);
        counterObserver.unobserve(e.target);
      }
    });
  }, { threshold: 0 });

  $$('.stat-num[data-count]').forEach(el => counterObserver.observe(el));

  // Also fire immediately for counters already visible on load
  requestAnimationFrame(() => {
    $$('.stat-num[data-count]').forEach(el => {
      const r = el.getBoundingClientRect();
      if (r.top < window.innerHeight && r.bottom > 0) {
        counterObserver.unobserve(el);
        animateCounter(el);
      }
    });
  });

  /* ─── Screenshots carousel ────────────────────── */
  const track       = $('#carouselTrack, .carousel-track');
  const prevBtn     = $('#carouselPrev, .carousel-prev');
  const nextBtn     = $('#carouselNext, .carousel-next');
  const dotsWrap    = $('#carouselDots, .carousel-dots');
  const items       = track ? $$('.carousel-item', track) : [];
  let   currentIdx  = 0;

  function getVisibleCount() {
    if (window.innerWidth < 480) return 1;
    if (window.innerWidth < 768) return 2;
    if (window.innerWidth < 1024) return 3;
    return 4;
  }

  function scrollCarousel(idx) {
    if (!track || !items.length) return;
    const item    = items[0];
    const itemW   = item.offsetWidth + 20; // 20px gap
    const visible = getVisibleCount();
    const maxIdx  = Math.max(0, items.length - visible);
    currentIdx    = Math.max(0, Math.min(idx, maxIdx));
    track.style.transform = `translateX(-${currentIdx * itemW}px)`;
    track.style.transition = 'transform 0.45s cubic-bezier(0.4, 0, 0.2, 1)';
    updateDots();
  }

  function updateDots() {
    if (!dotsWrap) return;
    $$('.carousel-dot', dotsWrap).forEach((d, i) => d.classList.toggle('active', i === currentIdx));
  }

  // Build dots
  if (dotsWrap && items.length) {
    items.forEach((_, i) => {
      const d = document.createElement('button');
      d.className = 'carousel-dot' + (i === 0 ? ' active' : '');
      d.setAttribute('aria-label', `Go to screenshot ${i + 1}`);
      d.addEventListener('click', () => scrollCarousel(i));
      dotsWrap.appendChild(d);
    });
  }

  prevBtn && prevBtn.addEventListener('click', () => scrollCarousel(currentIdx - 1));
  nextBtn && nextBtn.addEventListener('click', () => scrollCarousel(currentIdx + 1));

  // Keyboard arrows for carousel
  document.addEventListener('keydown', e => {
    if (lightbox && lightbox.classList.contains('open')) return;
    if (e.key === 'ArrowLeft')  scrollCarousel(currentIdx - 1);
    if (e.key === 'ArrowRight') scrollCarousel(currentIdx + 1);
  });

  // Drag / touch on carousel
  if (track) {
    let dragStart = 0, dragging = false;
    const onDragStart = e => { dragStart = (e.touches ? e.touches[0].clientX : e.clientX); dragging = true; };
    const onDragEnd   = e => {
      if (!dragging) return;
      dragging = false;
      const end   = (e.changedTouches ? e.changedTouches[0].clientX : e.clientX);
      const delta = dragStart - end;
      if (Math.abs(delta) > 40) scrollCarousel(currentIdx + (delta > 0 ? 1 : -1));
    };
    track.addEventListener('mousedown',  onDragStart);
    track.addEventListener('touchstart', onDragStart, { passive: true });
    track.addEventListener('mouseup',    onDragEnd);
    track.addEventListener('touchend',   onDragEnd);
  }

  /* ─── Lightbox ────────────────────────────────── */
  const lightbox      = $('#lightbox');
  const lightboxImg   = $('#lightboxImg');
  const lightboxClose = $('#lightboxClose');
  const lightboxPrev  = $('#lightboxPrev');
  const lightboxNext  = $('#lightboxNext');
  let   lbIndex       = 0;
  const lbImages      = items.map(el => $('img', el)).map(i => i && i.src).filter(Boolean);

  function openLightbox(idx) {
    if (!lightbox || !lightboxImg || !lbImages.length) return;
    lbIndex = idx;
    lightboxImg.src = lbImages[lbIndex];
    lightbox.classList.add('open');
    document.body.style.overflow = 'hidden';
  }

  function closeLightbox() {
    if (!lightbox) return;
    lightbox.classList.remove('open');
    document.body.style.overflow = '';
  }

  function lbNavigate(dir) {
    lbIndex = (lbIndex + dir + lbImages.length) % lbImages.length;
    if (lightboxImg) {
      lightboxImg.style.opacity = '0';
      setTimeout(() => {
        lightboxImg.src = lbImages[lbIndex];
        lightboxImg.style.opacity = '1';
      }, 150);
    }
  }

  if (lightboxImg) lightboxImg.style.transition = 'opacity 0.15s';

  items.forEach((item, i) => item.addEventListener('click', () => openLightbox(i)));
  lightboxClose && lightboxClose.addEventListener('click', closeLightbox);
  lightboxPrev  && lightboxPrev.addEventListener('click', () => lbNavigate(-1));
  lightboxNext  && lightboxNext.addEventListener('click', () => lbNavigate( 1));
  lightbox && lightbox.addEventListener('click', e => { if (e.target === lightbox) closeLightbox(); });

  document.addEventListener('keydown', e => {
    if (!lightbox || !lightbox.classList.contains('open')) return;
    if (e.key === 'Escape')     closeLightbox();
    if (e.key === 'ArrowLeft')  lbNavigate(-1);
    if (e.key === 'ArrowRight') lbNavigate( 1);
  });

  /* ─── Hero phone auto-cycle ───────────────────── */
  const phoneShot = $('.phone-screenshot');
  if (phoneShot && lbImages.length) {
    let phoneIdx = 0;
    setInterval(() => {
      phoneIdx = (phoneIdx + 1) % lbImages.length;
      phoneShot.style.opacity = '0';
      setTimeout(() => {
        phoneShot.src = lbImages[phoneIdx];
        phoneShot.style.opacity = '1';
      }, 250);
    }, 3000);
  }

  /* ─── FAQ accordion ───────────────────────────── */
  $$('.faq-question').forEach(btn => {
    btn.addEventListener('click', () => {
      const item = btn.closest('.faq-item');
      const isOpen = item.classList.contains('open');
      // close all
      $$('.faq-item.open').forEach(el => el.classList.remove('open'));
      if (!isOpen) item.classList.add('open');
    });
  });

  /* ─── Back to top ─────────────────────────────── */
  const backToTop = $('#backToTop');
  if (backToTop) {
    backToTop.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
  }

  /* ─── Marquee pause on hover ──────────────────── */
  const marqueeInner = $('.marquee-inner');
  if (marqueeInner) {
    marqueeInner.addEventListener('mouseenter', () => marqueeInner.style.animationPlayState = 'paused');
    marqueeInner.addEventListener('mouseleave', () => marqueeInner.style.animationPlayState = 'running');
  }

  /* ─── Smooth anchor nav offset ────────────────── */
  $$('a[href^="#"]').forEach(link => {
    link.addEventListener('click', e => {
      const target = document.getElementById(link.getAttribute('href').slice(1));
      if (!target) return;
      e.preventDefault();
      const offset = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--nav-h'), 10) || 68;
      window.scrollTo({ top: target.getBoundingClientRect().top + window.scrollY - offset - 8, behavior: 'smooth' });
    });
  });

  /* ─── Active nav highlighting ─────────────────── */
  const sections = $$('section[id]');
  const navAnchors = $$('.nav-links a[href^="#"]');

  const sectionObserver = new IntersectionObserver(entries => {
    entries.forEach(e => {
      if (!e.isIntersecting) return;
      const id = e.target.id;
      navAnchors.forEach(a => {
        a.style.color = a.getAttribute('href') === `#${id}` ? 'var(--purple)' : '';
      });
    });
  }, { rootMargin: '-40% 0px -50% 0px' });

  sections.forEach(s => sectionObserver.observe(s));

  /* ─── Resize handler ──────────────────────────── */
  window.addEventListener('resize', () => scrollCarousel(currentIdx), { passive: true });

})();

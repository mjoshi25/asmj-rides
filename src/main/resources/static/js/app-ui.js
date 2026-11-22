(() => {
  'use strict';
  document.querySelectorAll('[data-password-toggle]').forEach((toggle) => {
    toggle.addEventListener('click', () => {
      const field = document.getElementById(toggle.dataset.passwordToggle);
      if (!field) return;
      const reveal = field.type === 'password';
      field.type = reveal ? 'text' : 'password';
      toggle.textContent = reveal ? 'Hide' : 'Show';
      toggle.setAttribute('aria-label', reveal ? 'Hide password' : 'Show password');
    });
  });
  document.querySelectorAll('.profile-menu').forEach((menu) => {
    document.addEventListener('click', (event) => {
      if (!menu.contains(event.target)) menu.removeAttribute('open');
    });
    menu.querySelectorAll('a').forEach((link) => link.addEventListener('click', () => menu.removeAttribute('open')));
  });
  // One delegated handler keeps the shared navigation working on every authenticated page.
  document.addEventListener('click', (event) => {
    const toggle = event.target.closest('.unified-nav-toggle');
    if (toggle) {
      const header = toggle.closest('.unified-app-header');
      if (!header) return;
      const expanded = toggle.getAttribute('aria-expanded') === 'true';
      toggle.setAttribute('aria-expanded', String(!expanded));
      header.classList.toggle('nav-open', !expanded);
      return;
    }
    const clickedHeader = event.target.closest('.unified-app-header');
    document.querySelectorAll('.unified-app-header.nav-open').forEach((header) => {
      if (!clickedHeader || clickedHeader !== header || event.target.closest('.unified-site-nav a, .unified-site-nav button')) {
        header.classList.remove('nav-open');
        header.querySelector('.unified-nav-toggle')?.setAttribute('aria-expanded', 'false');
      }
    });
  });
  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') return;
    document.querySelectorAll('.unified-app-header.nav-open').forEach((header) => {
      header.classList.remove('nav-open');
      header.querySelector('.unified-nav-toggle')?.setAttribute('aria-expanded', 'false');
    });
  });
})();

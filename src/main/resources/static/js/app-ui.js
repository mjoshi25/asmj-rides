(() => {
  'use strict';
  document.querySelectorAll('.nav-toggle').forEach((toggle) => {
    toggle.addEventListener('click', () => {
      const header = toggle.closest('header');
      const expanded = toggle.getAttribute('aria-expanded') === 'true';
      toggle.setAttribute('aria-expanded', String(!expanded));
      header.classList.toggle('nav-open', !expanded);
    });
  });
})();

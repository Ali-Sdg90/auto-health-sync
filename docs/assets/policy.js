(() => {
  const nav = document.querySelector(".policy-nav");
  if (!nav) return;

  const entries = [...nav.querySelectorAll('a[href^="#"]')]
    .map((link) => ({ link, section: document.querySelector(link.hash) }))
    .filter(({ section }) => section);

  const setActive = (activeId) => {
    entries.forEach(({ link, section }) => {
      if (section.id === activeId) {
        link.setAttribute("aria-current", "location");
      } else {
        link.removeAttribute("aria-current");
      }
    });
  };

  let updateQueued = false;
  let clickedSectionId = null;

  entries.forEach(({ link, section }) => {
    link.addEventListener("click", () => {
      clickedSectionId = section.id;
      setActive(section.id);
    });
  });

  const updateActiveSection = () => {
    updateQueued = false;
    if (clickedSectionId) return;

    const marker = window.innerHeight * 0.42;
    let activeSection = entries[0]?.section;
    let closestDistance = Number.POSITIVE_INFINITY;

    entries.forEach(({ section }) => {
      const distance = Math.abs(section.getBoundingClientRect().top - marker);
      if (distance < closestDistance) {
        closestDistance = distance;
        activeSection = section;
      }
    });

    const atPageEnd = window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 4;
    if (atPageEnd) activeSection = entries.at(-1)?.section;
    if (activeSection) setActive(activeSection.id);
  };

  const queueUpdate = () => {
    if (updateQueued) return;
    updateQueued = true;
    window.requestAnimationFrame(updateActiveSection);
  };

  const releaseClickedSection = () => {
    if (!clickedSectionId) return;
    clickedSectionId = null;
    queueUpdate();
  };

  window.addEventListener("scroll", queueUpdate, { passive: true });
  window.addEventListener("resize", queueUpdate);
  window.addEventListener("wheel", releaseClickedSection, { passive: true });
  window.addEventListener("touchstart", releaseClickedSection, { passive: true });
  window.addEventListener("pointerdown", (event) => {
    if (!(event.target instanceof Element) || !nav.contains(event.target)) releaseClickedSection();
  }, { passive: true });
  window.addEventListener("keydown", (event) => {
    if (["ArrowUp", "ArrowDown", "PageUp", "PageDown", "Home", "End", " "].includes(event.key)) {
      releaseClickedSection();
    }
  });
  updateActiveSection();
})();

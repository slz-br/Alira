var size = 100;
var readerOn = false;
var synth = window.speechSynthesis;

var currentEl = null;
var currentOriginalHTML = null;
var readingQueue = [];   // fila de elementos a ler em sequência
var queuePos = 0;        // posição atual na fila
var session = 0;         // identifica cada "sessão" de leitura, pra ignorar callbacks antigos

var READABLE_TAGS = ['P','H1','H2','H3','H4','H5','H6','LI','TD','TH','A','LABEL'];

/* ── Tamanho e tipo da fonte (salvos no navegador, valem para todas as páginas) ── */
var SIZE_MIN = 80, SIZE_MAX = 150;

var FONTS = {
  quicksand:    "'Quicksand', sans-serif",
  arial:        "Arial, Helvetica, sans-serif",
  opendyslexic: "'OpenDyslexic', 'Comic Sans MS', sans-serif"
};

function savePref(key, value) {
  try { localStorage.setItem(key, value); } catch (e) {}
}
function loadPref(key) {
  try { return localStorage.getItem(key); } catch (e) { return null; }
}

function applySize() {
  document.documentElement.style.fontSize = size + '%';
  document.documentElement.style.setProperty('--font-scale', size / 100);
  document.documentElement.classList.toggle('font-scaled', size > 100);
  var fval = document.getElementById('fval');
  if (fval) fval.textContent = size + '%';
  var dec = document.getElementById('b-fdec');
  var inc = document.getElementById('b-finc');
  if (dec) dec.disabled = size <= SIZE_MIN;
  if (inc) inc.disabled = size >= SIZE_MAX;
}

function changeFont(delta) {
  size = Math.min(SIZE_MAX, Math.max(SIZE_MIN, size + delta));
  applySize();
  savePref('alira-font-size', size);
}

function changeFontFamily(key) {
  if (!FONTS[key]) key = 'quicksand';
  document.documentElement.style.setProperty('--font-main', FONTS[key]);
  var sel = document.getElementById('font-select');
  if (sel) sel.value = key;
  savePref('alira-font-family', key);
}

function loadPreferences() {
  var savedSize = parseInt(loadPref('alira-font-size'), 10);
  if (!isNaN(savedSize)) size = Math.min(SIZE_MAX, Math.max(SIZE_MIN, savedSize));
  applySize();
  changeFontFamily(loadPref('alira-font-family') || 'quicksand');
}

function toggle(cls, btnId) {
  document.body.classList.toggle(cls);
  document.getElementById(btnId).classList.toggle('on');
}

function toggleReader() {
  readerOn = !readerOn;
  var btn = document.getElementById('btn-reader');
  btn.classList.toggle('on', readerOn);
  btn.setAttribute('aria-pressed', readerOn);
  document.body.style.cursor = readerOn ? 'crosshair' : '';
  if (!readerOn) {
    session++;           // invalida qualquer leitura em andamento
    synth.cancel();
    restoreCurrent();
    readingQueue = [];
    queuePos = 0;
  }
}

/* ── Restaura o HTML original do elemento que estava sendo lido ── */
function restoreCurrent() {
  if (currentEl && currentOriginalHTML !== null) {
    currentEl.innerHTML = currentOriginalHTML;
  }
  currentEl = null;
  currentOriginalHTML = null;
}

function removeHighlights() { restoreCurrent(); }

/* ── Envolve só os NÓS DE TEXTO em <span class="rw">, preservando
      qualquer <strong>, <span> colorido, etc. que já exista dentro ── */
function wrapWords(node, wordIndexRef) {
  if (node.nodeType === Node.TEXT_NODE) {
    if (!node.textContent.trim()) return;

    var parts = node.textContent.split(/(\s+)/);
    var frag = document.createDocumentFragment();

    parts.forEach(function (part) {
      if (part.trim() === '') {
        frag.appendChild(document.createTextNode(part));
      } else {
        var span = document.createElement('span');
        span.className = 'rw';
        span.setAttribute('data-i', wordIndexRef.i++);
        span.textContent = part;
        frag.appendChild(span);
      }
    });

    node.parentNode.replaceChild(frag, node);
  } else if (node.nodeType === Node.ELEMENT_NODE) {
    if (node.tagName === 'SCRIPT' || node.tagName === 'STYLE') return;
    Array.prototype.slice.call(node.childNodes).forEach(function (child) {
      wrapWords(child, wordIndexRef);
    });
  }
}

/* ── Monta a lista de blocos legíveis da página inteira, na ordem em que
      aparecem, ignorando os que já estão dentro de outro bloco da lista ── */
function getReadableElements() {
  var all = Array.prototype.slice.call(
    document.querySelectorAll(READABLE_TAGS.join(','))
  );
  return all.filter(function (el) {
    return !all.some(function (other) {
      return other !== el && other.contains(el);
    });
  });
}

/* ── Fala um elemento e, ao terminar, avança pro próximo da fila ── */
function speakElement(el, mySession) {
  if (!synth) { alert('Seu navegador não suporta síntese de voz.'); return; }

  var text = (el.innerText || el.textContent || '').trim();
  if (!text) { advanceQueue(mySession); return; }

  currentEl = el;
  currentOriginalHTML = el.innerHTML;

  var wordIndexRef = { i: 0 };
  Array.prototype.slice.call(el.childNodes).forEach(function (child) {
    wrapWords(child, wordIndexRef);
  });

  var spans = el.querySelectorAll('.rw');
  var words = Array.prototype.map.call(spans, function (s) { return s.textContent; });

  var utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'pt-BR';
  utterance.rate = 0.95;

  utterance.onboundary = function (e) {
    if (mySession !== session) return;
    spans.forEach(function (s) { s.classList.remove('reading-highlight'); });
    var count = 0;
    for (var i = 0; i < words.length; i++) {
      if (count >= e.charIndex) { spans[i] && spans[i].classList.add('reading-highlight'); break; }
      count += words[i].length + 1;
    }
  };

  utterance.onend = function () {
    if (mySession !== session) return; // sessão antiga (usuário clicou em outro lugar ou desligou)
    if (currentEl === el) restoreCurrent();
    advanceQueue(mySession);
  };

  synth.speak(utterance);
}

/* ── Avança pro próximo item da fila, se ainda houver ── */
function advanceQueue(mySession) {
  if (mySession !== session || !readerOn) return;
  queuePos++;
  if (queuePos >= readingQueue.length) return; // fila acabou
  speakElement(readingQueue[queuePos], mySession);
}

/* ── Inicia a leitura contínua a partir do elemento clicado ── */
function startReadingFrom(el) {
  synth.cancel();
  restoreCurrent();
  session++;

  var all = getReadableElements();
  var startIndex = all.indexOf(el);

  // se clicou em algo "de dentro" (ex: span colorido dentro de um p),
  // sobe até achar o bloco pai que está na lista
  if (startIndex === -1) {
    var parent = el.parentElement;
    while (parent && parent !== document.body) {
      startIndex = all.indexOf(parent);
      if (startIndex !== -1) { el = parent; break; }
      parent = parent.parentElement;
    }
  }
  if (startIndex === -1) return; // clicou em algo sem texto legível (ex: um div vazio)

  readingQueue = all;
  queuePos = startIndex;
  speakElement(readingQueue[queuePos], session);
}

document.addEventListener('click', function (e) {
  if (!readerOn) return;
  startReadingFrom(e.target);
});

function reset() {
  size = 100;
  applySize();
  savePref('alira-font-size', size);
  changeFontFamily('quicksand');
  document.body.classList.remove('spacing');
  var bs = document.getElementById('b-spacing');
  if (bs) bs.classList.remove('on');
  if (readerOn) toggleReader();
}

loadPreferences();

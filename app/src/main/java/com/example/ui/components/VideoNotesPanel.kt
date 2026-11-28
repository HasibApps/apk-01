package com.example.ui.components

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

private class NoteJsBridge(
    private val changedFn: (String) -> Unit,
    private val seekFn: (Float) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onChanged(html: String) {
        main.post { changedFn.invoke(html) }
    }

    @JavascriptInterface
    fun seek(seconds: Float) {
        main.post { seekFn.invoke(seconds) }
    }
}

/**
 * ভিডিও নোট প্যানেল।
 *  [＋] বাটন = ভিডিওর এই মুহূর্তের টাইমলাইন নোটে যোগ ("topics: ..." + "Time line 01 : 1:23:06")
 *  টাইমলাইনে ট্যাপ = ভিডিও ওই সময় থেকে চলবে।
 *  লেখা HTML আকারে লেকচারের সাথে সেভ হয় (backup JSON এ যায়)।
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoNotesPanel(
    lectureKey: String,
    initialHtml: String,
    currentSeconds: () -> Float,
    onSeek: (Float) -> Unit,
    onNoteChanged: (String) -> Unit,
    onShowPdf: (() -> Unit)?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webView by remember(lectureKey) { mutableStateOf<WebView?>(null) }
    val getSeconds by rememberUpdatedState(currentSeconds)
    val seekCb by rememberUpdatedState(onSeek)
    val changedCb by rememberUpdatedState(onNoteChanged)

    DisposableEffect(lectureKey) {
        onDispose {
            webView?.let {
                try {
                    it.evaluateJavascript("if(window.flushNow) flushNow();", null)
                    it.stopLoading()
                    it.destroy()
                } catch (_: Throwable) {
                }
            }
            webView = null
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1120))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "NOTES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                // ＋ : বর্তমান ভিডিও টাইমলাইন নোটে যোগ
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .clickableNoRipple {
                            val sec = getSeconds().toInt().coerceAtLeast(0)
                            webView?.evaluateJavascript("addTimeline($sec);", null)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add timeline", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onShowPdf != null) {
                    IconButton(onClick = onShowPdf, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Description, contentDescription = "PDF", tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp))
                    }
                }
                IconButton(
                    onClick = {
                        val wv = webView
                        if (wv == null) onClose()
                        else wv.evaluateJavascript("if(window.flushNow) flushNow();") { onClose() }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            key(lectureKey) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        setBackgroundColor(android.graphics.Color.WHITE)
                        addJavascriptInterface(
                            NoteJsBridge(
                                changedFn = { changedCb(it) },
                                seekFn = { seekCb(it) }
                            ),
                            "NoteBridge"
                        )
                        loadDataWithBaseURL(null, buildNoteEditorHtml(initialHtml), "text/html", "UTF-8", null)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

private fun buildNoteEditorHtml(initialHtml: String): String {
    // JSON.quote → বাংলা/বিশেষ অক্ষর নিরাপদে JS স্ট্রিং হিসেবে বসানো
    val quoted = JSONObject.quote(initialHtml)
    return NOTE_EDITOR_TEMPLATE.replace("__INITIAL__", quoted)
}

private val NOTE_EDITOR_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
  * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
  html, body { margin: 0; padding: 0; height: 100%; background: #fff; color: #333;
    font-family: -apple-system, 'Noto Sans', 'Noto Sans Bengali', sans-serif; }
  #wrap { display: flex; flex-direction: column; height: 100%; }
  #ed { flex: 1; overflow-y: auto; padding: 14px 14px 30px 14px; font-size: 16px; line-height: 1.55; outline: none; word-wrap: break-word; }
  #ed:empty:before { content: attr(data-ph); color: #bdbdbd; }
  #ed h1 { font-size: 1.8em; margin: .3em 0; } #ed h2 { font-size: 1.5em; margin: .3em 0; }
  #ed h3 { font-size: 1.25em; margin: .3em 0; } #ed h4 { font-size: 1.1em; margin: .3em 0; }
  #ed a.tl { color: #0a63f5; text-decoration: underline; font-weight: 600; padding: 0 2px; }
  #ed .tlb { margin: 10px 0 4px 0; padding: 8px 10px; background: #f4f8ff; border-left: 4px solid #0a63f5; border-radius: 6px; }
  #ed .lbl { font-weight: 700; }
  #ed .chk { display: flex; align-items: flex-start; gap: 10px; margin: 6px 0; }
  #ed .chk input { width: 22px; height: 22px; margin-top: 2px; flex: none; }
  #ed .chk .ct { flex: 1; min-height: 1.4em; }
  #bar { border-top: 1px solid #eee; background: #fcfcfc; }
  #row0 { display: flex; align-items: center; justify-content: space-between; padding: 4px 8px; }
  .ib { border: none; background: transparent; font-size: 20px; color: #555; padding: 6px 12px; border-radius: 8px; }
  .ib.on { color: #0a63f5; }
  #fmt { display: none; padding: 6px 10px 10px 10px; }
  #fmt.show { display: block; }
  .grp { display: flex; background: #f4f4f5; border-radius: 12px; margin: 6px 0; overflow: hidden; }
  .grp button { flex: 1; border: none; background: transparent; padding: 10px 0; font-size: 17px; color: #222; border-right: 1px solid #fff; }
  .grp button:last-child { border-right: none; }
  .grp button.sel { background: #0a63f5; color: #fff; }
  .colors { display: flex; gap: 6px; overflow-x: auto; background: #f4f4f5; border-radius: 12px; padding: 6px 8px; margin: 6px 0; }
  .colors button { flex: none; width: 38px; height: 38px; border: none; border-radius: 8px; font-size: 20px; font-weight: 700; background: transparent; }
</style>
</head>
<body>
<div id="wrap">
  <div id="ed" contenteditable="true" data-ph="Note something down"></div>
  <div id="bar">
    <div id="row0">
      <div>
        <button class="ib" id="aa" onclick="toggleFmt()">Aa</button>
        <button class="ib" onclick="addCheck()">&#9745;</button>
      </div>
      <div>
        <button class="ib" onclick="cmd('undo')">&#8630;</button>
        <button class="ib" onclick="cmd('redo')">&#8631;</button>
      </div>
    </div>
    <div id="fmt">
      <div class="grp">
        <button onclick="blk('h1')"><b>H1</b></button><button onclick="blk('h2')"><b>H2</b></button>
        <button onclick="blk('h3')"><b>H3</b></button><button onclick="blk('h4')">H4</button>
        <button class="sel" onclick="blk('div')">body</button>
      </div>
      <div style="display:flex; gap:10px;">
        <div class="grp" style="flex:2">
          <button onclick="cmd('bold')"><b>B</b></button><button onclick="cmd('italic')"><i>I</i></button>
          <button onclick="cmd('underline')"><u>U</u></button><button onclick="cmd('strikeThrough')"><s>S</s></button>
        </div>
        <div class="grp" style="flex:1">
          <button onclick="cmd('indent')">&#8677;</button><button onclick="cmd('outdent')">&#8676;</button>
        </div>
      </div>
      <div style="display:flex; gap:10px;">
        <div class="grp" style="flex:1">
          <button onclick="cmd('insertOrderedList')">1.</button><button onclick="cmd('insertUnorderedList')">&#8226;</button><button onclick="dashList()">&#9642;</button>
        </div>
        <div class="grp" style="flex:1">
          <button onclick="cmd('justifyLeft')">&#8801;</button><button onclick="cmd('justifyCenter')">&#8801;</button><button onclick="cmd('justifyRight')">&#8801;</button>
        </div>
      </div>
      <div class="colors" id="fc"></div>
      <div class="colors" id="bc"></div>
    </div>
  </div>
</div>
<script>
  var ed = document.getElementById('ed');
  var timer = null;
  ed.innerHTML = __INITIAL__;

  function pushNow() {
    try { NoteBridge.onChanged(ed.innerHTML === '<br>' ? '' : ed.innerHTML); } catch (e) {}
  }
  function scheduleSave() { clearTimeout(timer); timer = setTimeout(pushNow, 300); }
  window.flushNow = function() { clearTimeout(timer); pushNow(); };

  ed.addEventListener('input', function() {
    if (ed.innerHTML === '<br>') ed.innerHTML = '';
    scheduleSave();
  });
  ed.addEventListener('change', function(e) {
    if (e.target && e.target.type === 'checkbox') {
      if (e.target.checked) e.target.setAttribute('checked', ''); else e.target.removeAttribute('checked');
      scheduleSave();
    }
  });
  // টাইমলাইনে ট্যাপ → ভিডিও ওই সময় থেকে চলবে
  ed.addEventListener('click', function(e) {
    var t = e.target.closest ? e.target.closest('.tl') : null;
    if (t) { e.preventDefault(); NoteBridge.seek(parseFloat(t.getAttribute('data-sec')) || 0); }
  });
  // চেকলিস্টে Enter = নতুন চেকবক্স লাইন
  ed.addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
      var sel = window.getSelection();
      var node = sel && sel.anchorNode;
      var line = node && (node.nodeType === 1 ? node : node.parentNode);
      line = line && line.closest ? line.closest('.chk') : null;
      if (line) { e.preventDefault(); insertCheckAfter(line); }
    }
  });

  function fmtTime(s) {
    s = Math.max(0, Math.floor(s));
    var h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
    return h + ':' + (m < 10 ? '0' : '') + m + ':' + (sec < 10 ? '0' : '') + sec;
  }
  function pad2(n) { return n < 10 ? '0' + n : '' + n; }

  // ＋ বাটন: topics + Time line NN : h:mm:ss ব্লক যোগ
  window.addTimeline = function(sec) {
    var max = 0;
    var blocks = ed.querySelectorAll('.tlb');
    for (var i = 0; i < blocks.length; i++) {
      var n = parseInt(blocks[i].getAttribute('data-n')) || 0;
      if (n > max) max = n;
    }
    var n = max + 1;
    var blk = document.createElement('div');
    blk.className = 'tlb';
    blk.setAttribute('data-n', n);
    blk.innerHTML =
      '<div class="topic"><span class="lbl" contenteditable="false">topics:</span>&nbsp;</div>' +
      '<div class="tline"><span contenteditable="false">Time line ' + pad2(n) + ' : ' +
      '<a class="tl" data-sec="' + sec + '">' + fmtTime(sec) + '</a></span></div>';
    ed.appendChild(blk);
    var after = document.createElement('div'); after.innerHTML = '<br>'; ed.appendChild(after);
    var topic = blk.querySelector('.topic');
    ed.focus();
    var r = document.createRange(); r.selectNodeContents(topic); r.collapse(false);
    var s = window.getSelection(); s.removeAllRanges(); s.addRange(r);
    blk.scrollIntoView({block: 'center'});
    scheduleSave();
  };

  function cmd(c, v) { ed.focus(); document.execCommand(c, false, v || null); scheduleSave(); }
  function blk(tag) { ed.focus(); document.execCommand('formatBlock', false, tag); scheduleSave(); }
  function dashList() {
    ed.focus(); document.execCommand('insertUnorderedList');
    var s = window.getSelection(); var n = s.anchorNode;
    var ul = n && (n.nodeType === 1 ? n : n.parentNode); ul = ul && ul.closest ? ul.closest('ul') : null;
    if (ul) ul.style.listStyleType = 'square';
    scheduleSave();
  }
  function newCheck() {
    var d = document.createElement('div'); d.className = 'chk';
    d.innerHTML = '<input type="checkbox" contenteditable="false"><span class="ct"><br></span>';
    return d;
  }
  function focusEnd(el) {
    var r = document.createRange(); r.selectNodeContents(el); r.collapse(true);
    var s = window.getSelection(); s.removeAllRanges(); s.addRange(r);
  }
  function addCheck() {
    ed.focus();
    var d = newCheck(); ed.appendChild(d); focusEnd(d.querySelector('.ct')); scheduleSave();
  }
  function insertCheckAfter(line) {
    var d = newCheck(); line.parentNode.insertBefore(d, line.nextSibling);
    focusEnd(d.querySelector('.ct')); scheduleSave();
  }
  function toggleFmt() { document.getElementById('fmt').classList.toggle('show'); document.getElementById('aa').classList.toggle('on'); }

  function buildColors(id, list, cmdName, fill) {
    var box = document.getElementById(id);
    list.forEach(function(c) {
      var b = document.createElement('button'); b.textContent = 'A';
      if (fill) { b.style.background = c; b.style.color = '#000'; } else { b.style.color = c; }
      b.onmousedown = function(e) { e.preventDefault(); };
      b.onclick = function() { ed.focus(); document.execCommand(cmdName, false, c); scheduleSave(); };
      box.appendChild(b);
    });
  }
  buildColors('fc', ['#000000', '#9e9e9e', '#f44336', '#ff9800', '#0a63f5', '#4caf50', '#9c27b0'], 'foreColor', false);
  buildColors('bc', ['#cfe2ff', '#f8d7da', '#fde8c8', '#c9f0e1', '#fff3b0', '#e2d4f5'], 'hiliteColor', true);

  // টুলবার বাটনে ট্যাপ করলে কীবোর্ড/সিলেকশন হারিয়ে না যায়
  document.querySelectorAll('#bar button').forEach(function(b) {
    b.addEventListener('mousedown', function(e) { e.preventDefault(); });
  });
  document.addEventListener('visibilitychange', function() { if (document.hidden) flushNow(); });
</script>
</body>
</html>
""".trimIndent()
